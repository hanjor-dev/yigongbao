package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据包文件截图关联 Entity
 * 每个 design_package_file 最多一条有效截图（upsert 语义）
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
@TableName("design_package_file_screenshot")
@EqualsAndHashCode(callSuper = false)
public class DesignPackageFileScreenshotEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 关联 design_package_file.id
     */
    private Long packageFileId;

    /**
     * 截图文件ID（关联 file_detail.id）
     */
    private String fileId;
}
