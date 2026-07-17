package com.yigongbao.module.design.service;

import com.yigongbao.module.design.vo.DesignQrImageVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 订单图纸二维码图片服务。
 */
public interface DesignQrImageService {

    /**
     * 上传并替换订单当前二维码图片。
     */
    DesignQrImageVO upload(Long orderId, MultipartFile file);

    /**
     * 查询订单当前二维码图片，没有时返回 null。
     */
    DesignQrImageVO getCurrent(Long orderId);
}
