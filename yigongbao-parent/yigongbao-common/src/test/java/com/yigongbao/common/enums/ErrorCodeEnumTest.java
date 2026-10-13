package com.yigongbao.common.enums;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErrorCodeEnum 优先级测试
 *
 * @author hanjor
 * @date 2026-05-08
 */
class ErrorCodeEnumTest {

    /**
     * 测试所有错误码都有有效的 priority
     */
    @Test
    void testAllErrorCodesHavePriority() {
        for (ErrorCodeEnum errorCode : ErrorCodeEnum.values()) {
            assertNotNull(errorCode.getPriority(),
                    "错误码 " + errorCode.name() + " 的 priority 不能为 null");
            assertTrue(errorCode.getPriority() >= 1 && errorCode.getPriority() <= 5,
                    "错误码 " + errorCode.name() + " 的 priority 必须在 1-5 范围内，实际值：" + errorCode.getPriority());
        }
    }

    /**
     * 测试优先级分布情况
     */
    @Test
    void testPriorityDistribution() {
        Map<Integer, Integer> distribution = new HashMap<>();

        for (ErrorCodeEnum errorCode : ErrorCodeEnum.values()) {
            Integer priority = errorCode.getPriority();
            distribution.put(priority, distribution.getOrDefault(priority, 0) + 1);
        }

        System.out.println("=== 错误码优先级分布 ===");
        System.out.println("总错误码数量: " + ErrorCodeEnum.values().length);
        for (int i = 1; i <= 5; i++) {
            int count = distribution.getOrDefault(i, 0);
            System.out.println("优先级 " + i + ": " + count + " 个错误码");
        }

        // 验证所有错误码都被统计
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(ErrorCodeEnum.values().length, total, "统计的错误码数量应该等于总数");
    }
}
