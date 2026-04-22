package com.yigongbao.module.system.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.dto.ChangePasswordDTO;
import com.yigongbao.module.system.auth.dto.ForgotPasswordResetDTO;
import com.yigongbao.module.system.auth.dto.LoginDTO;
import com.yigongbao.module.system.auth.dto.SendCaptchaDTO;
import com.yigongbao.module.system.auth.entity.LoginLogEntity;
import com.yigongbao.module.system.auth.enums.CaptchaSceneEnum;
import com.yigongbao.module.system.auth.enums.LoginTypeEnum;
import com.yigongbao.module.system.auth.mapper.LoginLogMapper;
import com.yigongbao.module.system.auth.service.AuthService;
import com.yigongbao.module.system.auth.service.CaptchaService;
import com.yigongbao.module.system.auth.vo.GraphicCaptchaVO;
import com.yigongbao.module.system.auth.vo.LoginVO;
import com.yigongbao.module.system.auth.vo.LoginLogVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.resource.service.ResourceService;
import com.yigongbao.module.system.user.convert.UserConvert;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 认证 Service 实现类
 * 支持账号密码、手机验证码、邮箱验证码三种登录方式，以及忘记密码重置
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
    private final RedisTemplate<String, Object> redisTemplate;

    /** 图形验证码 Redis key 前缀 */
    private static final String GRAPHIC_CAPTCHA_PREFIX = "graphic:captcha:";
    /** 图形验证码有效期（秒） */
    private static final int GRAPHIC_CAPTCHA_TTL = 300;

    // ==================== 图形验证码 ====================

    /**
     * 生成图形验证码（仅 PASSWORD 登录时需要）
     */
    @Override
    public GraphicCaptchaVO getGraphicCaptcha() {
        // 生成 4 位行干扰验证码
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = captcha.getCode().toLowerCase();
        String captchaId = IdUtil.fastSimpleUUID();

        // 将图片写入 Base64
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        captcha.write(out);
        String imageBase64 = "data:image/png;base64," + Base64.encode(out.toByteArray());

        // 存入 Redis
        redisTemplate.opsForValue().set(GRAPHIC_CAPTCHA_PREFIX + captchaId, code, GRAPHIC_CAPTCHA_TTL, TimeUnit.SECONDS);
        log.info("图形验证码已生成，captchaId={}", captchaId);
        return new GraphicCaptchaVO(captchaId, imageBase64);
    }

    // ==================== 登录 ====================

    /**
     * 用户登录（派发到三种登录实现）
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        log.info("用户登录，loginType={}, principal={}", dto.getLoginType(), dto.getPrincipal());
        String ip = getClientIp();
        String userAgent = getUserAgent();

        if (LoginTypeEnum.PASSWORD == dto.getLoginType()) {
            return resolveByPassword(dto, ip, userAgent);
        } else if (LoginTypeEnum.PHONE == dto.getLoginType()) {
            return resolveByPhone(dto, ip, userAgent);
        } else if (LoginTypeEnum.EMAIL == dto.getLoginType()) {
            return resolveByEmail(dto, ip, userAgent);
        }
        throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
    }

    /**
     * 账号密码登录
     */
    private LoginVO resolveByPassword(LoginDTO dto, String ip, String userAgent) {
        // 1. 校验图形验证码
        verifyGraphicCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());

        try {
            // 2. 查询用户
            UserEntity user = userMapper.selectByUsername(dto.getPrincipal());
            if (user == null) {
                log.warn("用户不存在，username={}", dto.getPrincipal());
                saveLoginLog(null, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "用户不存在");
                throw new BusinessException(ErrorCodeEnum.USERNAME_OR_PASSWORD_ERROR);
            }

            // 3. 校验用户状态
            if (Integer.valueOf(StatusConstants.DISABLED).equals(user.getStatus())) {
                log.warn("用户已禁用，username={}", dto.getPrincipal());
                saveLoginLog(user.getId(), dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "用户已禁用");
                throw new BusinessException(ErrorCodeEnum.USER_DISABLED);
            }

            // 4. 校验账户是否锁定
            if (isAccountLocked(user)) {
                int remainingMinutes = calculateRemainingLockMinutes(user);
                log.warn("账户已锁定，username={}，剩余{}分钟", dto.getPrincipal(), remainingMinutes);
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

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("账号密码登录异常，principal={}", dto.getPrincipal(), e);
            saveLoginLog(null, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "系统异常");
            throw e;
        }
    }

    /**
     * 手机验证码登录
     */
    private LoginVO resolveByPhone(LoginDTO dto, String ip, String userAgent) {
        try {
            // 1. 校验验证码
            captchaService.verifyCaptcha(
                    com.yigongbao.module.system.auth.enums.CaptchaTypeEnum.PHONE.getValue(),
                    dto.getPrincipal(), CaptchaSceneEnum.LOGIN.getScene(), dto.getCredential()
            );

            // 2. 查询用户
            UserEntity user = userMapper.selectByPhone(dto.getPrincipal());
            if (user == null) {
                log.warn("手机号对应用户不存在，phone={}", dto.getPrincipal());
                saveLoginLog(null, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "用户不存在");
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }

            // 3. 校验用户状态
            if (Integer.valueOf(StatusConstants.DISABLED).equals(user.getStatus())) {
                log.warn("用户已禁用，phone={}", dto.getPrincipal());
                saveLoginLog(user.getId(), dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "用户已禁用");
                throw new BusinessException(ErrorCodeEnum.USER_DISABLED);
            }

            return buildLoginSuccess(user, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("手机验证码登录异常，phone={}", dto.getPrincipal(), e);
            saveLoginLog(null, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "系统异常");
            throw e;
        }
    }

    /**
     * 邮箱验证码登录
     */
    private LoginVO resolveByEmail(LoginDTO dto, String ip, String userAgent) {
        try {
            // 1. 校验验证码
            captchaService.verifyCaptcha(
                    com.yigongbao.module.system.auth.enums.CaptchaTypeEnum.EMAIL.getValue(),
                    dto.getPrincipal(), CaptchaSceneEnum.LOGIN.getScene(), dto.getCredential()
            );

            // 2. 查询用户
            UserEntity user = userMapper.selectByEmail(dto.getPrincipal());
            if (user == null) {
                log.warn("邮箱对应用户不存在，email={}", dto.getPrincipal());
                saveLoginLog(null, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "用户不存在");
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }

            // 3. 校验用户状态
            if (Integer.valueOf(StatusConstants.DISABLED).equals(user.getStatus())) {
                log.warn("用户已禁用，email={}", dto.getPrincipal());
                saveLoginLog(user.getId(), dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "用户已禁用");
                throw new BusinessException(ErrorCodeEnum.USER_DISABLED);
            }

            return buildLoginSuccess(user, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("邮箱验证码登录异常，email={}", dto.getPrincipal(), e);
            saveLoginLog(null, dto.getPrincipal(), dto.getLoginType().getValue(), ip, userAgent, 0, "系统异常");
            throw e;
        }
    }

    /**
     * 校验图形验证码（PASSWORD 登录专用）
     */
    private void verifyGraphicCaptcha(String captchaId, String captchaCode) {
        if (StrUtil.isBlank(captchaId) || StrUtil.isBlank(captchaCode)) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_GRAPHIC_EXPIRED);
        }
        String key = GRAPHIC_CAPTCHA_PREFIX + captchaId;
        String stored = (String) redisTemplate.opsForValue().get(key);
        if (stored == null) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_GRAPHIC_EXPIRED);
        }
        // 一次性使用：无论对错立即删除
        redisTemplate.delete(key);
        if (!stored.equals(captchaCode.toLowerCase())) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_GRAPHIC_ERROR);
        }
    }

    /**
     * 构建登录成功响应并执行 Sa-Token 登录
     */
    private LoginVO buildLoginSuccess(UserEntity user, String principal, String loginType, String ip, String userAgent) {
        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("realName", user.getRealName());
        String token = StpUtil.getTokenValue();
        saveLoginLog(user.getId(), principal, loginType, ip, userAgent, 1, null);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        log.info("用户登录成功，userId={}, loginType={}", user.getId(), loginType);
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
        log.info("用户登出成功，userId={}", userId);
    }

    /**
     * 获取当前用户信息（含菜单、权限）
     */
    @Override
    public LoginVO getCurrentUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在，userId={}", userId);
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
        log.info("修改密码，userId={}", userId);

        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在，userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            log.warn("旧密码错误，userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.OLD_PASSWORD_ERROR);
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            log.warn("新旧密码不能相同，userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.NEW_PASSWORD_SAME_AS_OLD);
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        log.info("修改密码成功，userId={}", userId);
    }

    // ==================== 验证码发送 ====================

    /**
     * 发送登录验证码（PHONE/EMAIL 登录场景）
     */
    @Override
    public void sendLoginCaptcha(SendCaptchaDTO dto) {
        log.info("发送登录验证码，type={}, target={}", dto.getCaptchaType(), dto.getTarget());
        captchaService.sendCaptcha(dto.getCaptchaType().getValue(), dto.getTarget(), CaptchaSceneEnum.LOGIN.getScene());
    }

    /**
     * 发送忘记密码验证码
     * 防反枚举攻击：无论 target 是否注册，接口始终返回成功
     */
    @Override
    public void sendForgotPasswordCaptcha(SendCaptchaDTO dto) {
        log.info("发送忘记密码验证码，type={}, target={}", dto.getCaptchaType(), dto.getTarget());
        // 检查 target 是否存在（防止滥用，但不对外暴露结果）
        UserEntity user = null;
        if (com.yigongbao.module.system.auth.enums.CaptchaTypeEnum.PHONE == dto.getCaptchaType()) {
            user = userMapper.selectByPhone(dto.getTarget());
        } else if (com.yigongbao.module.system.auth.enums.CaptchaTypeEnum.EMAIL == dto.getCaptchaType()) {
            user = userMapper.selectByEmail(dto.getTarget());
        }
        if (user == null) {
            // 静默成功，防止枚举用户
            log.info("忘记密码：target 未注册，静默成功，target={}", dto.getTarget());
            return;
        }
        captchaService.sendCaptcha(dto.getCaptchaType().getValue(), dto.getTarget(), CaptchaSceneEnum.FORGOT.getScene());
    }

    /**
     * 忘记密码：校验验证码并重置密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ForgotPasswordResetDTO dto) {
        log.info("忘记密码重置，type={}, target={}", dto.getCaptchaType(), dto.getTarget());

        // 1. 校验验证码（匹配后自动删除）
        captchaService.verifyCaptcha(dto.getCaptchaType().getValue(), dto.getTarget(),
                CaptchaSceneEnum.FORGOT.getScene(), dto.getCaptcha());

        // 2. 查询用户
        UserEntity user = null;
        if (com.yigongbao.module.system.auth.enums.CaptchaTypeEnum.PHONE == dto.getCaptchaType()) {
            user = userMapper.selectByPhone(dto.getTarget());
        } else if (com.yigongbao.module.system.auth.enums.CaptchaTypeEnum.EMAIL == dto.getCaptchaType()) {
            user = userMapper.selectByEmail(dto.getTarget());
        }
        if (user == null) {
            log.warn("忘记密码：用户不存在，target={}", dto.getTarget());
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        // 3. 更新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        // 解锁账户
        user.setLoginFailCount(0);
        user.setLockTime(null);
        userMapper.updateById(user);
        log.info("忘记密码重置成功，userId={}", user.getId());
    }

    // ==================== 私有工具方法 ====================

    private void saveLoginLog(Long userId, String principal, String loginType, String ip, String userAgent, Integer status, String failReason) {
        try {
            LoginLogEntity logEntity = new LoginLogEntity();
            logEntity.setUserId(userId);
            logEntity.setUsername(principal);
            logEntity.setLoginType(loginType);
            logEntity.setIp(ip);
            logEntity.setUserAgent(userAgent);
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
        log.warn("密码错误，principal={}", principal);
        saveLoginLog(user.getId(), principal, loginType, ip, userAgent, 0, "密码错误");

        int maxFailures = getMaxLoginFailures();
        int newFailCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;

        user.setLoginFailCount(newFailCount);
        if (newFailCount >= maxFailures) {
            user.setLockTime(LocalDateTime.now());
            log.warn("账户已被锁定，principal={}，失败次数={}", principal, newFailCount);
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
                log.warn("login.max.failures 配置值无效，val={}", val);
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
                log.warn("login.lock.duration 配置值无效，val={}", val);
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
}
