package com.yigongbao.module.order.vo.order;

import com.yigongbao.module.basic.file.vo.FileVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情中的设计阶段文件信息。
 */
@Data
public class DesignFileDetailVO implements Serializable {

    private List<DesignPackageVO> packageList;
    private FileVO report;

    @Data
    public static class DesignPackageVO implements Serializable {
        private Long id;
        private Long orderId;
        private String orderCode;
        private String packageCode;
        private Integer packageSeq;
        private String fileId;
        private String fileName;
        private String fileUrl;
        private String downloadUrl;
        private Long fileSize;
        private Integer fileCount;
        private LocalDateTime uploadTime;
        private List<DesignPackageFileVO> files;
        private DesignDocVersionVO latestInstruction;
        private DesignDocVersionVO latestDrawing;
        private List<DesignDocVersionVO> latestDrawings;
    }

    @Data
    public static class DesignPackageFileVO implements Serializable {
        private Long id;
        private Long packageId;
        private String fileName;
        private String fileExt;
        private String filePath;
        private Long fileSize;
        private Integer sortOrder;
        private Boolean hasPrintInfo;
        private String fileUrl;
        private String downloadUrl;
    }

    @Data
    public static class DesignDocVersionVO implements Serializable {
        private Long id;
        private String version;
        private Integer versionSeq;
        private String sourceType;
        private String templateFileId;
        private String templateFileUrl;
        private String templateDownloadUrl;
        private String revisedFileId;
        private String revisedFileUrl;
        private String revisedDownloadUrl;
        private LocalDateTime generateTime;
        private LocalDateTime revisedUploadTime;
        private Integer isConfirmed;
        private String productCategory;
        private LocalDateTime confirmTime;
    }
}
