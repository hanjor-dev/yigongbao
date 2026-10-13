package com.yigongbao.module.production.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * 二维码生成工具类
 * 使用 ZXing 库生成二维码图片
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
public class QrCodeUtil {

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;

    /**
     * 生成二维码（Base64格式）
     *
     * @param content 二维码内容
     * @return Base64编码的PNG图片字符串
     */
    public static String generateQrCodeBase64(String content) {
        return generateQrCodeBase64(content, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * 生成二维码（Base64格式，自定义尺寸）
     *
     * @param content 二维码内容
     * @param width   宽度（像素）
     * @param height  高度（像素）
     * @return Base64编码的PNG图片字符串
     */
    public static String generateQrCodeBase64(String content, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            log.debug("生成二维码成功: contentLength={}, imageSize={}bytes", content.length(), outputStream.size());
            return base64;
        } catch (Exception e) {
            log.error("生成二维码失败: content={}", content, e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }
}
