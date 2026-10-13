# 设计文件上传重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构设计文件上传模块，统一配置管理方式，优化接口设计，简化数据模型

**Architecture:** 
- 配置迁移：`DesignConfigConstants` → `SystemConfigKeyEnum` + `DefaultConfigProperties` + `application.yml`
- 接口重构：数据包保持直接上传（需解析压缩包），模型/报告改为接收 fileId 关联已上传文件
- FileService 新增 `linkFile` 方法，支持文件关联业务
- DesignModelEntity 精简冗余字段，查询时通过 fileId 获取文件信息
- **模型关联支持批量 fileId 列表**，适应批量上传场景

**Tech Stack:** Spring Boot, MyBatis-Plus, x-file-storage

---

## 文件结构概览

| 操作 | 文件路径 |
|------|----------|
| 修改 | `yigongbao-common/.../enums/SystemConfigKeyEnum.java` |
| 修改 | `yigongbao-common/.../config/DefaultConfigProperties.java` |
| 修改 | `yigongbao-boot/.../application-dev.yml` |
| 修改 | `yigongbao-module-basic/.../file/service/FileService.java` |
| 修改 | `yigongbao-module-basic/.../file/service/impl/FileServiceImpl.java` |
| 删除 | `yigongbao-module-design/.../constant/DesignConfigConstants.java` |
| 修改 | `yigongbao-module-design/.../entity/DesignModelEntity.java` |
| 修改 | `yigongbao-module-design/.../vo/DesignModelVO.java` |
| 修改 | `yigongbao-module-design/.../service/DesignFileService.java` |
| 修改 | `yigongbao-module-design/.../service/impl/DesignFileServiceImpl.java` |
| 修改 | `yigongbao-module-design/.../controller/DesignFileController.java` |
| 新建 | `yigongbao-module-design/.../dto/LinkFilesDTO.java` |
| 修改 | `sql/ddl.sql` (design_model 表结构) |
| 修改 | `sql/ddl_design.sql` (design_model 表结构) |
| 修改 | 单元测试文件 |

---

## Task 1: 配置层重构（SystemConfigKeyEnum + DefaultConfigProperties + application.yml）

**Files:**
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/SystemConfigKeyEnum.java`
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/config/DefaultConfigProperties.java`
- Modify: `yigongbao-parent/yigongbao-boot/src/main/resources/application-dev.yml`

- [ ] **Step 1: 在 SystemConfigKeyEnum 中添加设计模块配置项**

在 `DESIGN_ASSIGN_MODE` 之后添加（注意修改 `DESIGN_ASSIGN_MODE` 的分号为逗号）：

```java
// ==================== 设计文件配置 ====================
/**
 * 数据包允许的文件扩展名（逗号分隔）
 */
DESIGN_PACKAGE_ALLOWED_EXTENSIONS("design.package.allowed_extensions", "数据包允许的文件扩展名"),

/**
 * 数据包最大大小（MB）
 */
DESIGN_PACKAGE_MAX_SIZE_MB("design.package.max_size_mb", "数据包最大大小"),

/**
 * 可视化模型最大大小（MB）
 */
DESIGN_MODEL_MAX_SIZE_MB("design.model.max_size_mb", "可视化模型最大大小"),

/**
 * 设计报告最大大小（MB）
 */
DESIGN_REPORT_MAX_SIZE_MB("design.report.max_size_mb", "设计报告最大大小");
```

- [ ] **Step 2: 在 DefaultConfigProperties 中添加设计文件配置默认值**

在 `configDesignAssignMode` 之后添加：

```java
// ==================== 设计文件配置 ====================
/**
 * 数据包允许的文件扩展名（逗号分隔）
 * 默认：.stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp
 */
private String configDesignPackageAllowedExtensions = ".stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp";

/**
 * 数据包最大大小（MB）
 * 默认 500MB
 */
private Integer configDesignPackageMaxSizeMb = 500;

/**
 * 可视化模型最大大小（MB）
 * 默认 200MB
 */
private Integer configDesignModelMaxSizeMb = 200;

/**
 * 设计报告最大大小（MB）
 * 默认 50MB
 */
private Integer configDesignReportMaxSizeMb = 50;
```

- [ ] **Step 3: 在 application-dev.yml 中添加设计文件配置兜底**

在 `yigongbao.config` 节点下的 `flow-max-design-reject: 3` 之后添加：

```yaml
    # ==================== 设计文件配置 ====================
    # 数据包允许的文件扩展名（逗号分隔）
    design-package-allowed-extensions: ".stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp"
    # 数据包最大大小（MB），默认 500MB
    design-package-max-size-mb: 500
    # 可视化模型最大大小（MB），默认 200MB
    design-model-max-size-mb: 200
    # 设计报告最大大小（MB），默认 50MB
    design-report-max-size-mb: 50
```

- [ ] **Step 4: 验证编译通过**

Run: `cd yigongbao-parent && mvn compile -pl yigongbao-common -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/SystemConfigKeyEnum.java
git add yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/config/DefaultConfigProperties.java
git add yigongbao-parent/yigongbao-boot/src/main/resources/application-dev.yml
git commit -m "feat(design): add design file config to SystemConfigKeyEnum and DefaultConfigProperties"
```

---

## Task 2: FileService 新增 linkFile 方法

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/service/FileService.java`
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/service/impl/FileServiceImpl.java`

- [ ] **Step 1: 在 FileService 接口添加 linkFile 方法**

在 `uploadAndLink` 方法之后添加：

```java
/**
 * 将已上传的文件关联到业务
 * 用于前端先上传文件，后端再关联业务的场景
 *
 * @param fileId  文件ID（必须是已上传的文件）
 * @param bizType 业务类型（字典 dict_code）
 * @param bizId   业务ID
 * @return 更新后的文件信息
 */
FileVO linkFile(String fileId, String bizType, Long bizId);
```

- [ ] **Step 2: 在 FileServiceImpl 实现 linkFile 方法**

在 `uploadMultiple` 方法之后添加：

```java
@Override
public FileVO linkFile(String fileId, String bizType, Long bizId) {
    log.info("关联文件到业务，fileId={}, bizType={}, bizId={}", fileId, bizType, bizId);
    try {
        // 1. 校验文件是否存在
        FileDetail detail = fileRecorderService.getDetailById(fileId);
        if (detail == null) {
            log.warn("文件不存在，fileId={}", fileId);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
        }
        
        // 2. 校验 bizType 是否为合法的字典编码
        FileBizTypeEnum fileBizTypeEnum = FileBizTypeEnum.getByDictCode(bizType);
        if (fileBizTypeEnum == null) {
            log.warn("业务类型不合法，bizType={}", bizType);
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "bizType");
        }
        
        // 3. 更新文件的业务关联信息
        detail.setObjectType(bizType);
        detail.setObjectId(bizId != null ? bizId.toString() : null);
        fileRecorderService.updateById(detail);
        
        log.info("文件关联成功，fileId={}, bizType={}, bizId={}", fileId, bizType, bizId);
        return fileRecorderService.toFileVO(detail);
    } catch (BusinessException e) {
        throw e;
    } catch (Exception e) {
        log.error("关联文件异常，fileId={}", fileId, e);
        throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
    }
}
```

- [ ] **Step 3: 验证编译通过**

Run: `cd yigongbao-parent && mvn compile -pl yigongbao-module-basic -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/service/FileService.java
git add yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/service/impl/FileServiceImpl.java
git commit -m "feat(file): add linkFile method to associate uploaded file with business"
```

---

## Task 3: 精简 DesignModelEntity 字段 + 更新 DDL

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/entity/DesignModelEntity.java`
- Modify: `sql/ddl.sql`
- Modify: `sql/ddl_design.sql`

- [ ] **Step 1: 修改 DesignModelEntity，移除冗余字段**

保留字段：id, orderId, fileId（BaseEntity 字段自动继承）
移除字段：fileName, fileUrl, fileSize, fileExt, uploadTime

```java
package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 可视化模型文件 Entity
 * 文件详情通过 fileId 关联 file_detail 表查询
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@TableName("design_model")
@EqualsAndHashCode(callSuper = false)
public class DesignModelEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 文件ID（关联 file_detail.id）
     */
    private String fileId;
}
```

- [ ] **Step 2: 更新 sql/ddl.sql 中的 design_model 表结构**

找到 `design_model` 表定义，替换为：

```sql
-- ============================================================
-- 可视化模型文件表（design_model）
-- 精简版，文件详情通过 file_detail 查询
-- ============================================================
DROP TABLE IF EXISTS design_model;
CREATE TABLE design_model (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    file_id         VARCHAR(32)     NOT NULL COMMENT '文件ID（关联 file_detail.id）',

    -- 公共字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_design_model_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可视化模型文件表';
```

- [ ] **Step 3: 同步更新 sql/ddl_design.sql 中的 design_model 表结构**

找到 `design_model` 表定义，替换为与上面相同的 SQL。

- [ ] **Step 4: 验证编译通过**

Run: `cd yigongbao-parent && mvn compile -pl yigongbao-module-design -q`
Expected: BUILD SUCCESS

---

## Task 4: 创建 LinkFilesDTO 请求类（支持批量 fileId）

**Files:**
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/LinkFilesDTO.java`

- [ ] **Step 1: 创建 LinkFilesDTO（支持单个或批量 fileId）**

```java
package com.yigongbao.module.design.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 关联文件请求 DTO（支持批量）
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@Schema(description = "关联文件请求（支持批量）")
public class LinkFilesDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", required = true)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "文件ID列表（已通过 FileController 上传）", required = true)
    @NotEmpty(message = "文件ID列表不能为空")
    private List<String> fileIds;
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd yigongbao-parent && mvn compile -pl yigongbao-module-design -q`
Expected: BUILD SUCCESS

---

## Task 5: 重构 DesignFileService 接口 + DesignModelVO

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignFileService.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignModelVO.java`

- [ ] **Step 1: 修改 DesignFileService 接口签名**

- `uploadModel(Long orderId, MultipartFile file)` → `linkModels(Long orderId, List<String> fileIds)`（支持批量）
- `uploadReport(Long orderId, MultipartFile file)` → `linkReport(Long orderId, String fileId)`

```java
package com.yigongbao.module.design.service;

import com.yigongbao.module.design.vo.DesignModelVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import com.yigongbao.module.basic.file.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 设计文件服务接口
 * 数据包：直接上传（需解析压缩包）
 * 模型/报告：关联已上传的文件（通过 FileController 上传）
 *
 * @author hanjor
 * @date 2026-04-15
 */
public interface DesignFileService {

    // ==================== 数据包 ====================

    /**
     * 上传打印文件数据包
     *
     * @param orderId 订单ID
     * @param file    压缩包文件（支持 ZIP/RAR/7Z）
     * @return 数据包信息
     */
    DesignPackageVO uploadPackage(Long orderId, MultipartFile file);

    /**
     * 删除数据包
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     */
    void deletePackage(Long orderId, Long packageId);

    /**
     * 获取订单的数据包列表
     *
     * @param orderId 订单ID
     * @return 数据包列表
     */
    List<DesignPackageVO> listPackages(Long orderId);

    // ==================== 可视化模型 ====================

    /**
     * 批量关联可视化模型文件
     * 文件需先通过 FileController 上传
     *
     * @param orderId 订单ID
     * @param fileIds 文件ID列表
     * @return 模型信息列表
     */
    List<DesignModelVO> linkModels(Long orderId, List<String> fileIds);

    /**
     * 删除可视化模型
     *
     * @param orderId 订单ID
     * @param modelId 模型ID
     */
    void deleteModel(Long orderId, Long modelId);

    /**
     * 获取订单的可视化模型列表
     *
     * @param orderId 订单ID
     * @return 模型列表
     */
    List<DesignModelVO> listModels(Long orderId);

    // ==================== 设计报告 ====================

    /**
     * 关联设计报告
     * 文件需先通过 FileController 上传
     *
     * @param orderId 订单ID
     * @param fileId  文件ID
     * @return 文件信息
     */
    FileVO linkReport(Long orderId, String fileId);

    /**
     * 删除设计报告
     *
     * @param orderId 订单ID
     * @param fileId  文件ID
     */
    void deleteReport(Long orderId, String fileId);

    /**
     * 获取订单的设计报告
     *
     * @param orderId 订单ID
     * @return 文件信息，无报告返回 null
     */
    FileVO getReport(Long orderId);
}
```

- [ ] **Step 2: 更新 DesignModelVO 字段（uploadTime → createTime）**

VO 字段变更说明：
- `uploadTime` → `createTime`：原字段来自 Entity，重构后改为使用 BaseEntity 的 createTime
- 其他字段保留，数据来源从 Entity 改为通过 FileVO 填充

> **前端影响**：如果前端使用了 `uploadTime` 字段，需要同步修改为 `createTime`。

```java
package com.yigongbao.module.design.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 可视化模型 VO
 * 文件详情通过 fileId 从 FileService 获取后填充
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class DesignModelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模型ID（design_model.id）
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 文件ID（file_detail.id）
     */
    private String fileId;

    /**
     * 原始文件名（来自 FileVO）
     */
    private String fileName;

    /**
     * 文件访问地址（来自 FileVO）
     */
    private String fileUrl;

    /**
     * 文件大小（字节，来自 FileVO）
     */
    private Long fileSize;

    /**
     * 文件扩展名（来自 FileVO）
     */
    private String fileExt;

    /**
     * 创建时间（来自 design_model.create_time）
     */
    private LocalDateTime createTime;
}
```

---

## Task 6: 重构 DesignFileServiceImpl

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignFileServiceImpl.java`

- [ ] **Step 1: 修复 BizType 常量错误**

现有代码中的 BizType 常量与 `FileBizTypeEnum` 不一致，需要修正：

```java
// 修正前（错误）
private static final String BIZ_TYPE_DESIGN_PACKAGE = "10.6";
private static final String BIZ_TYPE_DESIGN_MODEL = "10.7";

// 修正后（正确，与 FileBizTypeEnum 对应）
/**
 * 打印文件包业务类型（对应 FileBizTypeEnum.PRINT_PACKAGE = "10.4"）
 */
private static final String BIZ_TYPE_DESIGN_PACKAGE = "10.4";

/**
 * 可视化模型业务类型（对应 FileBizTypeEnum.VISUAL_MODEL = "10.6"）
 */
private static final String BIZ_TYPE_DESIGN_MODEL = "10.6";
```

> **重要**：此修正会影响已有数据的 object_type 值。如果生产环境已有数据，需要执行数据迁移 SQL。

- [ ] **Step 2: 移除 DesignConfigConstants 引用，改用 SystemConfigKeyEnum**

替换 import：
```java
// 移除
import com.yigongbao.module.design.constant.DesignConfigConstants;

// 新增
import com.yigongbao.common.enums.SystemConfigKeyEnum;
```

修改 `getAllowedExtensions()` 方法：
```java
/**
 * 获取允许的文件扩展名集合
 */
private Set<String> getAllowedExtensions() {
    String config = configService.getConfigValue(SystemConfigKeyEnum.DESIGN_PACKAGE_ALLOWED_EXTENSIONS.getKey());
    if (StrUtil.isBlank(config)) {
        config = ".stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp";
    }
    return Arrays.stream(config.split(","))
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(StrUtil::isNotBlank)
            .collect(Collectors.toSet());
}
```

- [ ] **Step 3: 实现 linkModels 方法（批量关联，替代原 uploadModel）**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public List<DesignModelVO> linkModels(Long orderId, List<String> fileIds) {
    log.info("批量关联可视化模型, orderId={}, fileIds={}", orderId, fileIds);

    // 1. 校验工单状态和操作权限
    checkOrderAndPermission(orderId);

    // 2. 批量校验文件是否存在
    List<FileVO> fileVOs = fileService.listByIds(fileIds);
    if (fileVOs.size() != fileIds.size()) {
        // 找出不存在的 fileId
        Set<String> foundIds = fileVOs.stream().map(FileVO::getId).collect(Collectors.toSet());
        List<String> notFoundIds = fileIds.stream().filter(id -> !foundIds.contains(id)).toList();
        log.warn("部分文件不存在, notFoundIds={}", notFoundIds);
        throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
    }

    // 3. 批量关联文件到业务，并保存模型记录
    Map<String, FileVO> fileMap = fileVOs.stream()
            .collect(Collectors.toMap(FileVO::getId, f -> f));
    
    List<DesignModelVO> results = new ArrayList<>();
    for (String fileId : fileIds) {
        // 关联文件到业务
        fileService.linkFile(fileId, BIZ_TYPE_DESIGN_MODEL, orderId);

        // 保存模型记录
        DesignModelEntity modelEntity = new DesignModelEntity();
        modelEntity.setOrderId(orderId);
        modelEntity.setFileId(fileId);
        modelMapper.insert(modelEntity);

        results.add(buildModelVO(modelEntity, fileMap.get(fileId)));
    }

    log.info("批量关联可视化模型成功, orderId={}, count={}", orderId, results.size());
    return results;
}
```

- [ ] **Step 4: 实现 linkReport 方法（替代原 uploadReport）**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public FileVO linkReport(Long orderId, String fileId) {
    log.info("关联设计报告, orderId={}, fileId={}", orderId, fileId);

    // 1. 校验工单状态和操作权限
    checkOrderAndPermission(orderId);

    // 2. 校验文件是否存在
    FileVO fileVO = fileService.getById(fileId);
    if (fileVO == null) {
        throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
    }

    // 3. 删除旧报告（每工单仅一份）
    List<FileVO> existingReports = fileService.listByBiz(BIZ_TYPE_DESIGN_REPORT, orderId);
    for (FileVO existing : existingReports) {
        fileService.deleteById(existing.getId());
        log.info("删除旧设计报告, fileId={}", existing.getId());
    }

    // 4. 关联新文件到业务
    return fileService.linkFile(fileId, BIZ_TYPE_DESIGN_REPORT, orderId);
}
```

- [ ] **Step 5: 更新 listModels 方法，通过 fileId 批量查询文件信息**

```java
@Override
public List<DesignModelVO> listModels(Long orderId) {
    // 1. 查询模型记录
    List<DesignModelEntity> models = modelMapper.selectList(
            new LambdaQueryWrapper<DesignModelEntity>()
                    .eq(DesignModelEntity::getOrderId, orderId)
                    .orderByDesc(DesignModelEntity::getCreateTime));

    if (CollUtil.isEmpty(models)) {
        return Collections.emptyList();
    }

    // 2. 批量查询文件信息
    List<String> fileIds = models.stream()
            .map(DesignModelEntity::getFileId)
            .collect(Collectors.toList());
    List<FileVO> fileVOs = fileService.listByIds(fileIds);
    Map<String, FileVO> fileMap = fileVOs.stream()
            .collect(Collectors.toMap(FileVO::getId, f -> f, (a, b) -> a));

    // 3. 构建 VO
    return models.stream()
            .map(entity -> buildModelVO(entity, fileMap.get(entity.getFileId())))
            .collect(Collectors.toList());
}
```

- [ ] **Step 6: 更新 buildModelVO 方法，删除旧方法**

```java
/**
 * 构建模型 VO
 */
private DesignModelVO buildModelVO(DesignModelEntity entity, FileVO fileVO) {
    DesignModelVO vo = new DesignModelVO();
    vo.setId(entity.getId());
    vo.setOrderId(entity.getOrderId());
    vo.setFileId(entity.getFileId());
    vo.setCreateTime(entity.getCreateTime());
    
    // 从 FileVO 填充文件信息
    if (fileVO != null) {
        vo.setFileName(fileVO.getFileName());
        vo.setFileUrl(fileVO.getFileUrl());
        vo.setFileSize(fileVO.getFileSize());
        vo.setFileExt(fileVO.getFileExt());
    }
    return vo;
}
```

删除只接收 entity 参数的旧 `buildModelVO(DesignModelEntity entity)` 方法。

- [ ] **Step 7: 验证编译通过**

Run: `cd yigongbao-parent && mvn compile -pl yigongbao-module-design -q`
Expected: BUILD SUCCESS

---

## Task 7: 重构 DesignFileController + 删除 DesignConfigConstants

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignFileController.java`
- Delete: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/constant/DesignConfigConstants.java`

- [ ] **Step 1: 重写 DesignFileController**

- 移除 `{orderId}` 路径参数，改为请求参数/请求体
- 数据包保持上传接口不变
- 模型改为批量 link 接口，报告改为单个 link 接口

```java
package com.yigongbao.module.design.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.dto.LinkFilesDTO;
import com.yigongbao.module.design.service.DesignFileService;
import com.yigongbao.module.design.vo.DesignModelVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 设计文件管理控制器
 * 数据包：直接上传（需解析压缩包）
 * 模型/报告：关联已上传的文件
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Tag(name = "设计文件管理")
@RestController
@RequestMapping("/design")
@RequiredArgsConstructor
public class DesignFileController {

    private final DesignFileService designFileService;

    // ==================== 数据包 ====================

    @Operation(summary = "上传打印文件数据包")
    @PostMapping("/package/upload")
    public Result<DesignPackageVO> uploadPackage(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "压缩包文件") @RequestParam("file") MultipartFile file) {
        DesignPackageVO result = designFileService.uploadPackage(orderId, file);
        return Result.success(result);
    }

    @Operation(summary = "删除数据包")
    @DeleteMapping("/package/{packageId}")
    public Result<Void> deletePackage(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "数据包ID") @PathVariable Long packageId) {
        designFileService.deletePackage(orderId, packageId);
        return Result.success();
    }

    @Operation(summary = "获取数据包列表")
    @GetMapping("/packages")
    public Result<List<DesignPackageVO>> listPackages(
            @Parameter(description = "订单ID") @RequestParam Long orderId) {
        List<DesignPackageVO> result = designFileService.listPackages(orderId);
        return Result.success(result);
    }

    // ==================== 可视化模型 ====================

    @Operation(summary = "批量关联可视化模型")
    @PostMapping("/models/link")
    public Result<List<DesignModelVO>> linkModels(@Valid @RequestBody LinkFilesDTO dto) {
        List<DesignModelVO> result = designFileService.linkModels(dto.getOrderId(), dto.getFileIds());
        return Result.success(result);
    }

    @Operation(summary = "删除可视化模型")
    @DeleteMapping("/model/{modelId}")
    public Result<Void> deleteModel(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "模型ID") @PathVariable Long modelId) {
        designFileService.deleteModel(orderId, modelId);
        return Result.success();
    }

    @Operation(summary = "获取可视化模型列表")
    @GetMapping("/models")
    public Result<List<DesignModelVO>> listModels(
            @Parameter(description = "订单ID") @RequestParam Long orderId) {
        List<DesignModelVO> result = designFileService.listModels(orderId);
        return Result.success(result);
    }

    // ==================== 设计报告 ====================

    @Operation(summary = "关联设计报告")
    @PostMapping("/report/link")
    public Result<FileVO> linkReport(@Valid @RequestBody LinkFilesDTO dto) {
        // 设计报告只取第一个文件
        String fileId = dto.getFileIds().get(0);
        FileVO result = designFileService.linkReport(dto.getOrderId(), fileId);
        return Result.success(result);
    }

    @Operation(summary = "删除设计报告")
    @DeleteMapping("/report/{fileId}")
    public Result<Void> deleteReport(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "文件ID") @PathVariable String fileId) {
        designFileService.deleteReport(orderId, fileId);
        return Result.success();
    }

    @Operation(summary = "获取设计报告")
    @GetMapping("/report")
    public Result<FileVO> getReport(
            @Parameter(description = "订单ID") @RequestParam Long orderId) {
        FileVO result = designFileService.getReport(orderId);
        return Result.success(result);
    }
}
```

- [ ] **Step 2: 确认 DesignConfigConstants 无其他引用后删除**

Run: `cd yigongbao-parent && grep -r "DesignConfigConstants" --include="*.java" | grep -v "^Binary"`
Expected: No output (or only the file to be deleted)

```bash
rm yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/constant/DesignConfigConstants.java
```

- [ ] **Step 3: 验证编译通过**

Run: `cd yigongbao-parent && mvn compile -pl yigongbao-module-design -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit（design 模块完整重构）**

```bash
git add sql/ddl.sql sql/ddl_design.sql
git add yigongbao-parent/yigongbao-module-design/
git commit -m "refactor(design): complete design file module restructuring

- Simplify DesignModelEntity, remove redundant file fields
- Add LinkFilesDTO for batch file association
- Change uploadModel to linkModels (batch support)
- Change uploadReport to linkReport
- Fix BizType constants (10.7 -> 10.6 for model)
- Remove DesignConfigConstants, use SystemConfigKeyEnum
- Update Controller paths"
```

---

## Task 8: 更新单元测试

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignFileServiceImplTest.java`

- [ ] **Step 1: 更新 linkModels 测试（批量关联，替代 uploadModel）**

```java
@Nested
@DisplayName("linkModels 测试")
class LinkModelsTest {

    @Test
    @DisplayName("成功批量关联可视化模型")
    void shouldLinkModelsSuccessfully() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
            when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);

            List<String> fileIds = List.of("file-1", "file-2");
            
            FileVO fileVO1 = new FileVO();
            fileVO1.setId("file-1");
            fileVO1.setFileName("model1.stl");
            fileVO1.setFileExt("stl");
            
            FileVO fileVO2 = new FileVO();
            fileVO2.setId("file-2");
            fileVO2.setFileName("model2.stl");
            fileVO2.setFileExt("stl");
            
            when(fileService.listByIds(fileIds)).thenReturn(List.of(fileVO1, fileVO2));
            when(fileService.linkFile(anyString(), eq("10.6"), eq(orderId))).thenAnswer(inv -> {
                String fid = inv.getArgument(0);
                return "file-1".equals(fid) ? fileVO1 : fileVO2;
            });

            when(modelMapper.insert(any(DesignModelEntity.class))).thenAnswer(invocation -> {
                DesignModelEntity entity = invocation.getArgument(0);
                entity.setId(System.currentTimeMillis());
                entity.setCreateTime(LocalDateTime.now());
                return 1;
            });

            List<DesignModelVO> results = designFileService.linkModels(orderId, fileIds);

            assertEquals(2, results.size());
            verify(modelMapper, times(2)).insert(any(DesignModelEntity.class));
            verify(fileService, times(2)).linkFile(anyString(), eq("10.6"), eq(orderId));
        }
    }

    @Test
    @DisplayName("部分文件不存在抛出异常")
    void shouldThrowExceptionWhenSomeFilesNotFound() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
            when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);

            List<String> fileIds = List.of("file-1", "not-exist");
            
            FileVO fileVO1 = new FileVO();
            fileVO1.setId("file-1");
            when(fileService.listByIds(fileIds)).thenReturn(List.of(fileVO1)); // 只返回1个

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> designFileService.linkModels(orderId, fileIds));

            assertEquals(ErrorCodeEnum.ATTACHMENT_NOT_FOUND.getCode(), exception.getCode());
        }
    }
}
```

- [ ] **Step 2: 更新 linkReport 测试（替代 uploadReport）**

```java
@Nested
@DisplayName("linkReport 测试")
class LinkReportTest {

    @Test
    @DisplayName("成功关联设计报告")
    void shouldLinkReportSuccessfully() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
            when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);

            when(fileService.listByBiz("10.5", orderId)).thenReturn(Collections.emptyList());

            FileVO fileVO = new FileVO();
            fileVO.setId("file-789");
            fileVO.setFileName("report.pdf");
            when(fileService.getById("file-789")).thenReturn(fileVO);
            when(fileService.linkFile(eq("file-789"), eq("10.5"), eq(orderId))).thenReturn(fileVO);

            FileVO result = designFileService.linkReport(orderId, "file-789");

            assertNotNull(result);
            assertEquals("file-789", result.getId());
            verify(fileService).linkFile(eq("file-789"), eq("10.5"), eq(orderId));
        }
    }

    @Test
    @DisplayName("关联新报告时删除旧报告")
    void shouldDeleteOldReportWhenLinkNew() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
            when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);

            FileVO oldReport = new FileVO();
            oldReport.setId("old-file");
            when(fileService.listByBiz("10.5", orderId)).thenReturn(List.of(oldReport));

            FileVO newFileVO = new FileVO();
            newFileVO.setId("new-file");
            when(fileService.getById("new-file")).thenReturn(newFileVO);
            when(fileService.linkFile(eq("new-file"), eq("10.5"), eq(orderId))).thenReturn(newFileVO);

            designFileService.linkReport(orderId, "new-file");

            verify(fileService).deleteById("old-file");
            verify(fileService).linkFile(eq("new-file"), eq("10.5"), eq(orderId));
        }
    }
}
```

- [ ] **Step 3: 移除旧的 UploadModelTest 和 UploadReportTest 嵌套类**

- [ ] **Step 4: 添加必要的 import**

```java
import java.time.LocalDateTime;
import java.util.ArrayList;
```

- [ ] **Step 5: 运行测试验证**

Run: `cd yigongbao-parent && mvn test -pl yigongbao-module-design -Dtest=DesignFileServiceImplTest -q`
Expected: Tests run: XX, Failures: 0, Errors: 0

- [ ] **Step 6: Commit**

```bash
git add yigongbao-parent/yigongbao-module-design/src/test/
git commit -m "test(design): update DesignFileServiceImplTest for linkModels/linkReport"
```

---

## Task 9: 更新技术文档 + 全量验证

**Files:**
- Modify: `.docs/技术实现/design/03_设计文件上传处理.md`

- [ ] **Step 1: 更新接口表格**

```markdown
## 二、涉及接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/design/package/upload?orderId=` | POST | 上传打印文件数据包 |
| `/api/design/package/{packageId}?orderId=` | DELETE | 删除数据包 |
| `/api/design/packages?orderId=` | GET | 获取数据包列表 |
| `/api/design/models/link` | POST | 批量关联可视化模型 |
| `/api/design/model/{modelId}?orderId=` | DELETE | 删除可视化模型 |
| `/api/design/models?orderId=` | GET | 获取可视化模型列表 |
| `/api/design/report/link` | POST | 关联设计报告 |
| `/api/design/report/{fileId}?orderId=` | DELETE | 删除设计报告 |
| `/api/design/report?orderId=` | GET | 获取设计报告 |
```

- [ ] **Step 2: 更新可视化模型处理逻辑**

```markdown
### 3.2 可视化模型文件

**关联流程**（批量接收 fileIds，非直接上传）：
1. 前端先调用 `/api/file/upload` 上传文件（可批量），获取 fileId 列表
2. 前端调用 `/api/design/models/link` 传入 orderId 和 fileIds
3. 后端校验工单状态和操作权限
4. 后端批量校验文件是否存在
5. 后端循环调用 FileService.linkFile 关联文件到业务
6. 后端批量写入 design_model 表（只存 orderId 和 fileId）
7. 后端返回 List<DesignModelVO>（文件详情从 FileVO 填充）

**删除流程**：（保持不变）
```

- [ ] **Step 3: 更新设计报告处理逻辑**

```markdown
### 3.3 设计报告

**关联流程**（接收 fileId，非直接上传）：
1. 前端先调用 `/api/file/upload` 上传文件，获取 fileId
2. 前端调用 `/api/design/report/link` 传入 orderId 和 fileIds（取第一个）
3. 后端校验工单状态和操作权限
4. 后端删除旧报告（每工单仅一份）
5. 后端调用 FileService.linkFile 关联文件到业务
6. 后端返回 FileVO

**说明**：复用现有 file_detail 表，object_type 区分，每工单一份。
```

- [ ] **Step 4: 更新系统配置说明**

```markdown
## 八、系统配置

配置通过 `SystemConfigKeyEnum` 统一管理，兜底默认值在 `DefaultConfigProperties` 和 `application.yml` 中配置。

| 配置键 | SystemConfigKeyEnum | 默认值 | 说明 |
|--------|---------------------|--------|------|
| `design.package.allowed_extensions` | DESIGN_PACKAGE_ALLOWED_EXTENSIONS | `.stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp` | 数据包允许的文件扩展名 |
| `design.package.max_size_mb` | DESIGN_PACKAGE_MAX_SIZE_MB | `500` | 数据包最大大小（MB） |
| `design.model.max_size_mb` | DESIGN_MODEL_MAX_SIZE_MB | `200` | 可视化模型最大大小（MB） |
| `design.report.max_size_mb` | DESIGN_REPORT_MAX_SIZE_MB | `50` | 设计报告最大大小（MB） |
```

- [ ] **Step 5: 全量编译验证**

Run: `cd yigongbao-parent && mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: 运行设计模块全量测试**

Run: `cd yigongbao-parent && mvn test -pl yigongbao-module-design -q`
Expected: Tests run: XX, Failures: 0, Errors: 0

- [ ] **Step 7: 最终 Commit**

```bash
git add .docs/技术实现/design/03_设计文件上传处理.md
git commit -m "docs(design): update design file upload documentation

- Update API paths for new link endpoints
- Document batch model association flow
- Update config management section"
```

---

## 总结

| 变更类型 | 文件数 | 说明 |
|---------|-------|------|
| 新增配置 | 3 | SystemConfigKeyEnum + DefaultConfigProperties + application.yml |
| 新增接口 | 1 | FileService.linkFile |
| 新增 DTO | 1 | LinkFilesDTO（支持批量） |
| 重构 | 5 | Entity + VO + Service + ServiceImpl + Controller |
| 删除 | 1 | DesignConfigConstants |
| 更新 DDL | 2 | ddl.sql + ddl_design.sql |
| 更新测试 | 1 | DesignFileServiceImplTest |
| 更新文档 | 1 | 03_设计文件上传处理.md |

**接口变更对照**：

| 原接口 | 新接口 | 变更说明 |
|--------|--------|----------|
| POST `/design/workorder/{orderId}/package/upload` | POST `/design/package/upload?orderId=` | 路径简化 |
| POST `/design/workorder/{orderId}/visual-model/upload` | POST `/design/models/link` | 改为批量关联接口 |
| POST `/design/workorder/{orderId}/report/upload` | POST `/design/report/link` | 改为关联接口 |

**Commit 节点（共 5 次）**：

| 序号 | 内容 | 备注 |
|------|------|------|
| 1 | 配置层重构 | SystemConfigKeyEnum + DefaultConfigProperties + application.yml |
| 2 | FileService.linkFile | basic 模块独立提交 |
| 3 | design 模块完整重构 | Entity + DTO + Service + ServiceImpl + Controller + DDL |
| 4 | 单元测试更新 | |
| 5 | 文档更新 | |
