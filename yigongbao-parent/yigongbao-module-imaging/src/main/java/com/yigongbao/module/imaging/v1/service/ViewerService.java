package com.yigongbao.module.imaging.v1.service;

import com.yigongbao.module.imaging.v1.vo.ViewerConfigVO;
import com.yigongbao.module.imaging.v1.vo.ViewerStlVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 影像查看器适配服务（v1）
 *
 * @author hanjor
 * @date 2026-05-06
 */
public interface ViewerService {

    /** 获取查看器初始化配置（paths + token） */
    ViewerConfigVO getViewerConfig(Long orderId, String token);

    /** dcmPath：返回DCM压缩包URL列表 */
    List<String> getDcmUrls(Long orderId);

    /** stlPath：返回STL模型数据（按数据包分组） */
    ViewerStlVO getStlData(Long orderId);

    /** markPath：保存标注截图（关联到数据包+模型文件） */
    void saveMark(String groupId, String modelFileId, MultipartFile file);
}
