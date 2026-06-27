package com.yigongbao.module.system.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.framework.util.IpLocationUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.dto.ChangePasswordDTO;
import com.yigongbao.module.system.auth.dto.ForgotPasswordResetDTO;
import com.yigongbao.module.system.auth.dto.LoginDTO;
import com.yigongbao.module.system.auth.dto.SendCaptchaDTO;
import com.yigongbao.module.system.auth.convert.LoginLogConvert;
import com.yigongbao.module.system.auth.entity.LoginLogEntity;
import com.yigongbao.module.system.auth.enums.CaptchaSceneEnum;
import com.yigongbao.module.system.auth.enums.LoginTypeEnum;
import com.yigongbao.module.system.auth.mapper.LoginLogMapper;
import com.yigongbao.module.system.auth.service.AuthService;
import com.yigongbao.module.system.auth.service.CaptchaService;
import com.yigongbao.module.system.auth.service.ImageCaptchaService;
import com.yigongbao.module.system.auth.vo.LoginLogVO;
import com.yigongbao.module.system.auth.vo.LoginVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.resource.service.ResourceService;
import com.yigongbao.module.system.user.convert.UserConvert;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.org.entity.OrgEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.time.LocalDateTime;

/**
 * 认证 Service 实现类
 * 支持账号密码、手机验证码两种登录方式，以及忘记密码重置（手机/邮箱二选一验证）
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final ResourceService resourceService;
    private final ConfigService configService;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final ImageCaptchaService imageCaptchaService;
    private final StringRedisTemplate stringRedisTemplate;
    private final OrgService orgService;

    // ==================== 登录 ====================

    /**
     * 用户登录（派发到三种登录实现）
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        String ip = getClientIp();
        String userAgent = getUserAgent();

        if (LoginTypeEnum.PASSWORD == dto.getLoginType()) {
            return resolveByPassword(dto, ip, userAgent);
        } else if (LoginTypeEnum.PHONE == dto.getLoginType()) {
            return resolveByPhone(dto, ip, userAgent);
        }
        throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
    }

    /**
     * 账号密码登录
     */
    private LoginVO resolveByPassword(LoginDTO dto, String ip, String userAgent) {
        // 1. 校验滑动验证码 Token（PASSWORD 登录必须通过二次验证）
        //verifyCaptchaToken(dto.getCaptchaToken());

        // 2. 查询用户（principal 支持用户名或邮箱，自动识别）
        boolean isEmail = Validator.isEmail(dto.getPrincipal());
        UserEntity user = isEmail
                ? userMapper.selectByEmail(dto.getPrincipal())
                : userMapper.selectByUsername(dto.getPrincipal());
        if (user == null) {
            log.warn("用户不存在: principal={}", dto.getPrincipal());
            saveLoginLog(null, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "用户不存在");
            throw new BusinessException(ErrorCodeEnum.USERNAME_OR_PASSWORD_ERROR);
        }

        // 3. 校验用户状态
        if (Integer.valueOf(StatusConstants.DISABLED).equals(user.getStatus())) {
            log.warn("用户已禁用: username={}", dto.getPrincipal());
            saveLoginLog(user.getId(), dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "用户已禁用");
            throw new BusinessException(ErrorCodeEnum.USER_DISABLED);
        }
        // 校验用户所属机构状态
        validateOrgStatus(user, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent);
        if (isAccountLocked(user)) {
            int remainingMinutes = calculateRemainingLockMinutes(user);
            log.warn("账户已锁定: username={}, 剩余{}分钟", dto.getPrincipal(), remainingMinutes);
            saveLoginLog(user.getId(), dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "账户已锁定");
            throw new BusinessException(ErrorCodeEnum.ACCOUNT_LOCKED, remainingMinutes);
        }

        // 5. 校验密码
        if (!passwordEncoder.matches(dto.getCredential(), user.getPassword())) {
            handleLoginFailure(user, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent);
            throw new BusinessException(ErrorCodeEnum.PASSWORD_ERROR);
        }

        // 6. 登录成功
        resetLoginFailCount(user);
        return buildLoginSuccess(user, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent);
    }

    /**
     * 手机验证码登录（已禁用：手机号不再唯一，无法区分用户身份）
     */
    private LoginVO resolveByPhone(LoginDTO dto, String ip, String userAgent) {
        log.warn("手机验证码登录已禁用: phone={}", dto.getPrincipal());
        throw new BusinessException(ErrorCodeEnum.LOGIN_TYPE_NOT_SUPPORTED);
    }

    /**
     * 校验滑动验证码 Token（PASSWORD 登录专用）
     * <p>
     * Token 由 POST /image-captch/check 校验成功后颁发，存入 Redis（2 分钟有效期）。
     * 验证通过后立即从 Redis 删除，防止重放攻击。
     *
     * @param captchaToken 验证码 Token（由 /check 接口返回的 id 字段）
     */
    private void verifyCaptchaToken(String captchaToken) {
        if (StrUtil.isBlank(captchaToken)) {
            log.warn("滑动验证码Token为空");
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_TOKEN_MISSING);
        }
        String redisKey = ImageCaptchaService.CAPTCHA_SECONDARY_TOKEN_PREFIX + captchaToken;
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        if (!Boolean.TRUE.equals(exists)) {
            log.warn("滑动验证码Token无效或已过期: captchaToken={}", captchaToken);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_TOKEN_INVALID);
        }
        // 验证通过，删除 token（一次性）
        stringRedisTemplate.delete(redisKey);
    }

    /**
     * 构建登录成功响应并执行 Sa-Token 登录
     */
    private LoginVO buildLoginSuccess(UserEntity user, String principal, String loginType, String ip, String userAgent) {
        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("realName", user.getRealName());
        StpUtil.getSession().set("permissions", resourceService.getUserPermissions(user.getId()));

        // 存储用户信息到会话，避免审核时查库（提升性能）
        if (user.getRoleCode() != null) {
            StpUtil.getSession().set("roleCode", user.getRoleCode());
        }
        if (user.getDeptId() != null) {
            StpUtil.getSession().set("deptId", user.getDeptId());
        }
        if (user.getRealName() != null) {
            StpUtil.getSession().set("userName", user.getRealName());
        }

        String token = StpUtil.getTokenValue();
        saveLoginLog(user.getId(), principal, loginType, ip, userAgent, 1, null);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        log.info("用户登录: userId={}, loginType={}", user.getId(), loginType);
        return loginVO;
    }

    // ==================== 登出 / 当前用户信息 / 改密 ====================

    /**
     * 用户登出
     */
    @Override
    public void logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        log.info("用户登出: userId={}", userId);
    }

    /**
     * 获取当前用户信息（含菜单、权限）
     */
    @Override
    public LoginVO getCurrentUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUser(UserConvert.toVO(user));
        vo.setMenus(resourceService.getUserMenuTree(userId));
        vo.setPermissions(resourceService.getUserPermissions(userId));
        return vo;
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            log.warn("旧密码错误: userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.OLD_PASSWORD_ERROR);
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            log.warn("新旧密码不能相同: userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.NEW_PASSWORD_SAME_AS_OLD);
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        // 踢出该用户所有在线 Session，旧 token 立即失效
        StpUtil.logout(userId);
        log.info("修改密码: userId={}", userId);
    }

    // ==================== 验证码发送 ====================

    /**
     * 发送登录验证码（PHONE/EMAIL 登录场景）
     */
    @Override
    public void sendLoginCaptcha(SendCaptchaDTO dto) {
        captchaService.sendCaptcha(dto.getCaptchaType().getValue(), dto.getTarget(), CaptchaSceneEnum.LOGIN.getScene());
    }

    /**
     * 发送忘记密码验证码（仅支持邮箱）
     * 防反枚举攻击：无论 target 是否注册，接口始终返回成功
     */
    @Override
    public void sendForgotPasswordCaptcha(SendCaptchaDTO dto) {
        // 手机号找回密码已禁用
        if (com.yigongbao.module.system.auth.enums.CaptchaTypeEnum.PHONE == dto.getCaptchaType()) {
            log.warn("手机号找回密码已禁用: phone={}", dto.getTarget());
            throw new BusinessException(ErrorCodeEnum.PHONE_RESET_PASSWORD_NOT_SUPPORTED);
        }

        // 检查邮箱是否存在
        UserEntity user = userMapper.selectByEmail(dto.getTarget());
        if (user == null) {
            log.info("忘记密码验证码: target未注册，静默成功, target={}", dto.getTarget());
            return;
        }
        captchaService.sendCaptcha(dto.getCaptchaType().getValue(), dto.getTarget(), CaptchaSceneEnum.FORGOT.getScene());
    }

    /**
     * 忘记密码：校验验证码并重置密码（仅支持邮箱）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ForgotPasswordResetDTO dto) {
        // 1. 校验验证码（匹配后自动删除）
        captchaService.verifyCaptcha(dto.getCaptchaType().getValue(), dto.getTarget(),
                CaptchaSceneEnum.FORGOT.getScene(), dto.getCaptcha());

        // 2. 仅支持邮箱重置密码
        if (com.yigongbao.module.system.auth.enums.CaptchaTypeEnum.PHONE == dto.getCaptchaType()) {
            log.warn("手机号重置密码已禁用: phone={}", dto.getTarget());
            throw new BusinessException(ErrorCodeEnum.PHONE_RESET_PASSWORD_NOT_SUPPORTED);
        }

        // 3. 查询用户
        UserEntity user = userMapper.selectByEmail(dto.getTarget());
        if (user == null) {
            log.warn("忘记密码重置失败: 用户不存在: target={}", dto.getTarget());
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        // 4. 更新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setLoginFailCount(0);
        user.setLockTime(null);
        userMapper.updateById(user);
        log.info("忘记密码重置: userId={}", user.getId());
    }

    // ==================== 私有工具方法 ====================

    /**
     * 校验用户所属机构状态，禁用时拒绝登录
     */
    private void validateOrgStatus(UserEntity user, String principal, String loginType, String ip, String userAgent) {
        if (user.getOrgId() == null) return;
        OrgEntity org = orgService.getById(user.getOrgId());
        if (org != null && StatusConstants.DISABLED == org.getStatus()) {
            log.warn("用户所属机构已禁用: principal={}, orgId={}", principal, user.getOrgId());
            saveLoginLog(user.getId(), principal, loginType, ip, userAgent, 0, "所属机构已禁用");
            throw new BusinessException(ErrorCodeEnum.ORG_DISABLED);
        }
    }

    private void saveLoginLog(Long userId, String principal, String loginType, String ip, String userAgent, Integer status, String failReason) {
        try {
            // 解析IP归属地
            String location = IpLocationUtil.getLocation(ip);

            LoginLogEntity logEntity = new LoginLogEntity();
            logEntity.setUserId(userId);
            logEntity.setUsername(principal);
            logEntity.setLoginType(loginType);
            logEntity.setIp(ip);
            logEntity.setUserAgent(userAgent);
            logEntity.setLocation(location);
            logEntity.setLoginTime(LocalDateTime.now());
            logEntity.setLoginStatus(status);
            logEntity.setFailReason(failReason);
            loginLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("保存登录日志异常", e);
        }
    }

    private boolean isAccountLocked(UserEntity user) {
        if (user.getLockTime() == null) {
            return false;
        }
        int lockDuration = getLockDurationMinutes();
        return user.getLockTime().plusMinutes(lockDuration).isAfter(LocalDateTime.now());
    }

    private int calculateRemainingLockMinutes(UserEntity user) {
        if (user.getLockTime() == null) {
            return 0;
        }
        int lockDuration = getLockDurationMinutes();
        LocalDateTime unlockTime = user.getLockTime().plusMinutes(lockDuration);
        long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), unlockTime).getSeconds();
        if (remainingSeconds <= 0) {
            return 0;
        }
        return (int) Math.max(1, (remainingSeconds + 59) / 60);
    }

    private void handleLoginFailure(UserEntity user, String principal, String loginType, String ip, String userAgent) {
        log.warn("密码错误: principal={}", principal);
        saveLoginLog(user.getId(), principal, loginType, ip, userAgent, 0, "密码错误");

        int maxFailures = getMaxLoginFailures();
        int newFailCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;

        user.setLoginFailCount(newFailCount);
        if (newFailCount >= maxFailures) {
            user.setLockTime(LocalDateTime.now());
            log.warn("账户已被锁定: principal={}, 失败次数={}", principal, newFailCount);
        }
        userMapper.updateById(user);
    }

    private void resetLoginFailCount(UserEntity user) {
        if (user.getLoginFailCount() != null && user.getLoginFailCount() > 0) {
            user.setLoginFailCount(0);
            user.setLockTime(null);
            userMapper.updateById(user);
        }
    }

    private int getMaxLoginFailures() {
        String val = configService.getConfigValue(SystemConfigKeyEnum.LOGIN_MAX_FAILURES.getKey());
        if (StrUtil.isNotBlank(val)) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                log.warn("login.max.failures 配置值无效: val={}", val);
            }
        }
        return 5;
    }

    private int getLockDurationMinutes() {
        String val = configService.getConfigValue(SystemConfigKeyEnum.LOGIN_LOCK_DURATION.getKey());
        if (StrUtil.isNotBlank(val)) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                log.warn("login.lock.duration 配置值无效: val={}", val);
            }
        }
        return 15;
    }

    private String getClientIp() {
        try {
            HttpServletRequest request = getHttpServletRequest();
            if (request == null) {
                return "unknown";
            }
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getUserAgent() {
        try {
            HttpServletRequest request = getHttpServletRequest();
            if (request == null) {
                return "unknown";
            }
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private HttpServletRequest getHttpServletRequest() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 登录历史查询 ====================

    @Override
    public LoginLogVO getPreviousLogin() {
        Long userId = StpUtil.getLoginIdAsLong();
        var logs = loginLogMapper.selectList(
                new LambdaQueryWrapper<LoginLogEntity>()
                        .eq(LoginLogEntity::getUserId, userId)
                        .eq(LoginLogEntity::getLoginStatus, 1)
                        .orderByDesc(LoginLogEntity::getLoginTime)
                        .last("LIMIT 1 OFFSET 1")
        );
        return logs.isEmpty() ? null : LoginLogConvert.toVO(logs.get(0));
    }

    @Override
    public List<LoginLogVO> getLoginHistory(Integer limit) {
        if (limit <= 0) {
            limit = 30;
        }
        if (limit > 100) {
            limit = 100;
        }
        Long userId = StpUtil.getLoginIdAsLong();

        Page<LoginLogEntity> page = new Page<>(1, limit);
        page = loginLogMapper.selectPage(page,
                new LambdaQueryWrapper<LoginLogEntity>()
                        .eq(LoginLogEntity::getUserId, userId)
                        .eq(LoginLogEntity::getLoginStatus, 1)
                        .orderByDesc(LoginLogEntity::getLoginTime)
        );
        return page.getRecords().stream().map(LoginLogConvert::toVO).toList();
    }
}
