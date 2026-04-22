package com.yigongbao.module.system.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图形验证码响应 VO
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Data
@AllArgsConstructor
public class GraphicCaptchaVO {

    /** 验证码唯一标识，前端登录时携带 */
    private String captchaId;

    /** Base64 编码的图片，格式：data:image/png;base64,... */
    private String imageBase64;
}
