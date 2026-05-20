package com.yigongbao.common.result;

import com.yigongbao.common.enums.ErrorCodeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 优先级测试
 *
 * @author hanjor
 * @date 2026-05-08
 */
class ResultTest {

    /**
     * 测试使用 ErrorCodeEnum 的错误响应包含正确的 priority
     */
    @Test
    void testErrorWithPriority() {
        Result<Void> result = Result.error(ErrorCodeEnum.UNAUTHORIZED);

        assertNotNull(result.getPriority(), "使用 ErrorCodeEnum 的错误响应应该包含 priority");
        assertEquals(1, result.getPriority(), "UNAUTHORIZED 的 priority 应该为 1");
        assertEquals(401, result.getCode(), "错误码应该正确");
        assertEquals("未登录或登录已过期，请重新登录", result.getMessage(), "错误信息应该正确");
    }

    /**
     * 测试成功响应的 priority 为 null
     */
    @Test
    void testSuccessNoPriority() {
        Result<String> result = Result.success("test data");

        assertNull(result.getPriority(), "成功响应的 priority 应该为 null");
        assertEquals(200, result.getCode(), "成功响应的 code 应该为 200");
        assertEquals("操作成功", result.getMessage(), "成功响应的 message 应该正确");
        assertEquals("test data", result.getData(), "成功响应的 data 应该正确");
    }

    /**
     * 测试使用 code+message 的错误响应 priority 为 null
     */
    @Test
    void testErrorWithCodeMessageNoPriority() {
        Result<Void> result = Result.error(400, "自定义错误");

        assertNull(result.getPriority(), "使用 code+message 的错误响应 priority 应该为 null");
        assertEquals(400, result.getCode(), "错误码应该正确");
        assertEquals("自定义错误", result.getMessage(), "错误信息应该正确");
    }

    /**
     * 测试使用 ErrorCodeEnum + 自定义消息的错误响应包含正确的 priority
     */
    @Test
    void testErrorWithEnumAndCustomMessage() {
        Result<Void> result = Result.error(ErrorCodeEnum.PARAM_ERROR, "自定义参数错误信息");

        assertNotNull(result.getPriority(), "使用 ErrorCodeEnum 的错误响应应该包含 priority");
        assertEquals(5, result.getPriority(), "PARAM_ERROR 的 priority 应该为 5");
        assertEquals(410, result.getCode(), "错误码应该正确");
        assertEquals("自定义参数错误信息", result.getMessage(), "自定义错误信息应该正确");
    }
}
