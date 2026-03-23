package com.yigongbao.module.system.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.dto.ChangePasswordDTO;
import com.yigongbao.module.system.auth.dto.LoginDTO;
import com.yigongbao.module.system.auth.entity.LoginLogEntity;
import com.yigongbao.module.system.auth.mapper.LoginLogMapper;
import com.yigongbao.module.system.auth.service.AuthService;
import com.yigongbao.module.system.auth.vo.LoginVO;
import com.yigongbao.module.system.auth.vo.LoginLogVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.common.config.DefaultConfigProperties;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.module.system.resource.service.ResourceService;
import com.yigongbao.module.system.resource.vo.ResourceVO;
import com.yigongbao.module.system.user.convert.UserConvert;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.time.LocalDateTime;

/**
 * 认证 Service 实现类
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
    private final DefaultConfigProperties defaultConfigProperties;

    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        log.info("用户登录，username={}", dto.getUsername());
        String ip = getClientIp();
        String userAgent = getUserAgent();

        try {
            // 查询用户
            UserEntity user = userMapper.selectByUsername(dto.getUsername());
            if (user == null) {
                log.warn("用户不存在，username={}", dto.getUsername());
                saveLoginLog(null, dto.getUsername(), ip, userAgent, 0, "用户不存在");
                throw new BusinessException(ErrorCodeEnum.USERNAME_OR_PASSWORD_ERROR);
            }

            // 校验用户状态
            if (user.getStatus().equals(StatusConstants.DISABLED)) {
                log.warn("用户已禁用，username={}", dto.getUsername());
                saveLoginLog(user.getId(), dto.getUsername(), ip, userAgent, 0, "用户已禁用");
                throw new BusinessException(ErrorCodeEnum.USER_DISABLED);
            }

            // 校验账户是否已锁定（自动解锁：lockTime + lockDuration <= now）
            if (isAccountLocked(user)) {
                int remainingMinutes = calculateRemainingLockMinutes(user);
                log.warn("账户已锁定，username={}，剩余锁定时间={}分钟", dto.getUsername(), remainingMinutes);
                saveLoginLog(user.getId(), dto.getUsername(), ip, userAgent, 0, "账户已锁定");
                throw new BusinessException(ErrorCodeEnum.ACCOUNT_LOCKED, remainingMinutes);
            }

            // 校验密码
            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                // 处理登录失败：计数+判断是否达到锁定阈值
                handleLoginFailure(user, dto.getUsername(), ip, userAgent);
                throw new BusinessException(ErrorCodeEnum.PASSWORD_ERROR);
            }

            // 登录成功：重置失败计数
            resetLoginFailCount(user);

            // 执行登录（Sa-Token）
            StpUtil.login(user.getId());
            String token = StpUtil.getTokenValue();

            // 记录登录成功日志
            saveLoginLog(user.getId(), dto.getUsername(), ip, userAgent, 1, null);

            // 构建返回结果
            LoginVO loginVO = new LoginVO();
            loginVO.setToken(token);
            loginVO.setUser(UserConvert.toVO(user));
            loginVO.setMenus(resourceService.getUserMenuTree(user.getId()));
            loginVO.setPermissions(resourceService.getUserPermissions(user.getId()));

            log.info("用户登录成功，username={}", dto.getUsername());
            return loginVO;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户登录异常，username={}", dto.getUsername(), e);
            saveLoginLog(null, dto.getUsername(), ip, userAgent, 0, "系统异常");
            throw e;
        }
    }

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

        // 校验旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            log.warn("旧密码错误，userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.OLD_PASSWORD_ERROR);
        }

        // 校验新旧密码不能相同
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            log.warn("新旧密码不能相同，userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.OLD_PASSWORD_ERROR);
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        log.info("修改密码成功，userId={}", userId);
    }

    /**
     * 获取客户端IP
     */
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
            // 多级代理时取第一个IP
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取User-Agent
     */
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

    /**
     * 获取HttpServletRequest
     */
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

    /**
     * 保存登录日志
     */
    private void saveLoginLog(Long userId, String username, String ip, String userAgent, Integer status, String failReason) {
        try {
            LoginLogEntity logEntity = new LoginLogEntity();
            logEntity.setUserId(userId);
            logEntity.setUsername(username);
            logEntity.setIp(ip);
            logEntity.setUserAgent(userAgent);
            logEntity.setLoginTime(LocalDateTime.now());
            logEntity.setLoginStatus(status);
            logEntity.setFailReason(failReason);
            loginLogMapper.insert(logEntity);
        } catch (Exception e) {
            // 日志记录失败不影响业务
            log.error("保存登录日志异常", e);
        }
    }

    /**
     * 判断账户是否已锁定
     * 锁定条件：lockTime 不为空，且 (lockTime + lockDuration分钟) > 当前时间
     *
     * @param user 用户实体
     * @return true-已锁定，false-未锁定
     */
    private boolean isAccountLocked(UserEntity user) {
        if (user.getLockTime() == null) {
            return false;
        }
        int lockDuration = getLockDurationMinutes();
        return user.getLockTime().plusMinutes(lockDuration).isAfter(LocalDateTime.now());
    }

    /**
     * 计算账户剩余锁定时间（分钟）
     *
     * @param user 用户实体
     * @return 剩余分钟数（向上取整，最小为1）
     */
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

    /**
     * 处理登录失败
     * 递增失败计数，达到阈值时锁定账户
     *
     * @param user 用户实体
     * @param username 用户名
     * @param ip IP地址
     * @param userAgent User-Agent
     */
    private void handleLoginFailure(UserEntity user, String username, String ip, String userAgent) {
        log.warn("密码错误，username={}", username);

        // 记录失败日志
        saveLoginLog(user.getId(), username, ip, userAgent, 0, "密码错误");

        // 递增失败计数
        int maxFailures = getMaxLoginFailures();
        int newFailCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;

        if (newFailCount >= maxFailures) {
            // 达到锁定阈值，锁定账户
            user.setLoginFailCount(newFailCount);
            user.setLockTime(LocalDateTime.now());
            userMapper.updateById(user);
            log.warn("账户已被锁定，username={}，失败次数={}", username, newFailCount);
        } else {
            // 未达到阈值，仅更新失败计数
            user.setLoginFailCount(newFailCount);
            userMapper.updateById(user);
        }
    }

    /**
     * 重置登录失败计数（登录成功时调用）
     *
     * @param user 用户实体
     */
    private void resetLoginFailCount(UserEntity user) {
        if (user.getLoginFailCount() != null && user.getLoginFailCount() > 0) {
            user.setLoginFailCount(0);
            user.setLockTime(null);
            userMapper.updateById(user);
        }
    }

    /**
     * 获取最大登录失败次数
     * 优先从数据库配置获取，如果不存在或已禁用则使用配置文件中的默认值兜底
     *
     * @return 最大失败次数
     */
    private int getMaxLoginFailures() {
        String configValue = configService.getConfigValue(SystemConfigKeyEnum.LOGIN_MAX_FAILURES.getKey());
        if (StrUtil.isNotBlank(configValue)) {
            try {
                return Integer.parseInt(configValue);
            } catch (NumberFormatException e) {
                log.warn("login.max.failures 配置值无效，configValue={}", configValue);
            }
        }
        // 理论上 configService.getConfigValue 不会返回 null，此处兜底以防万一
        return defaultConfigProperties.getLoginMaxFailures();
    }

    /**
     * 获取登录锁定时长（分钟）
     * 优先从数据库配置获取，如果不存在或已禁用则使用配置文件中的默认值兜底
     *
     * @return 锁定时长（分钟）
     */
    private int getLockDurationMinutes() {
        String configValue = configService.getConfigValue(SystemConfigKeyEnum.LOGIN_LOCK_DURATION.getKey());
        if (StrUtil.isNotBlank(configValue)) {
            try {
                return Integer.parseInt(configValue);
            } catch (NumberFormatException e) {
                log.warn("login.lock.duration 配置值无效，configValue={}", configValue);
            }
        }
        // 理论上 configService.getConfigValue 不会返回 null，此处兜底以防万一
        return defaultConfigProperties.getLoginLockDuration();
    }
}
