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

        // 获取当前用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("权限校验，userId={}, permission={}", userId, permission);

        // TODO: 后续需要注入 ResourceService 进行实际校验
        // 目前简化处理，后续实现完整的权限校验逻辑

        return point.proceed();
    }
}
