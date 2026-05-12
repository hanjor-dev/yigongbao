package com.yigongbao.framework.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.framework.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限校验切面
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Component
@Aspect
@RequiredArgsConstructor
@Slf4j
public class PermissionAspect {

    /**
     * 权限校验
     */
    @Around("@annotation(com.yigongbao.framework.annotation.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        String permission = annotation.value();

        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("权限校验，userId={}, permission={}", userId, permission);

        // 从 Session 读取权限列表（登录时已缓存）
        Object permissionsObj = StpUtil.getSession().get("permissions");
        if (permissionsObj == null) {
            log.warn("用户权限缓存为空，userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
        }

        @SuppressWarnings("unchecked")
        java.util.List<String> permissions = (java.util.List<String>) permissionsObj;
        if (!permissions.contains(permission)) {
            log.warn("权限校验失败，userId={}, permission={}", userId, permission);
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
        }

        log.debug("权限校验通过，userId={}, permission={}", userId, permission);
        return point.proceed();
    }
}
