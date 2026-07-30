package com.yigongbao.module.design.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指令单/图纸生成结果单项 VO
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DocItemVO {

    /** 记录ID */
    private Long id;

    /** 版本号，格式 A/1, A/2, ... */
    private String version;

    /** 模板文件ID，前端通过 GET /basic/file/download/{fileId} 下载 */
    private String fileId;

    /** 系统生成的模板文件访问URL */
    private String templateFileUrl;

    /** 生成时间 */
    private LocalDateTime generateTime;

    /** 是否已确认（0=未确认，1=已确认）；上传修订版或手动确认后置1，重新生成时自动重置为0 */
    private Integer isConfirmed;
}
