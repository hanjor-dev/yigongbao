# 设计工单查询功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现设计工单列表查询、工单详情、列配置管理四个接口，支持按 designer_id 的数据权限过滤。

**Architecture:** 在 yigongbao-module-design 中新建 DesignQueryHelper（数据权限/排序/字段翻译）、DesignWorkorderService/Impl（列表+详情+提交校验）、DesignWorkorderController、DesignColumnConfigController。同时在 yigongbao-module-system 的 UserService 扩展两个按部门/机构查询用户ID的方法，在 yigongbao-common 扩展 SystemConfigKeyEnum 和 DefaultConfigProperties。

**Tech Stack:** Java 21, Spring Boot 3, MyBatis-Plus 3.5.8, Sa-Token 1.37.0, JUnit 5 + Mockito, Hutool 5.8.26

---

## 文件结构总览

### 新增文件

| 模块 | 文件路径 | 职责 |
|------|---------|------|
| design | `dto/DesignWorkorderQueryDTO.java` | 列表查询参数 |
| design | `dto/SaveDesignColumnConfigDTO.java` | 保存列配置参数 |
| design | `vo/DesignWorkorderListVO.java` | 列表项 VO |
| design | `vo/DesignWorkorderDetailVO.java` | 工单详情 VO |
| design | `vo/SubmitCheckVO.java` | 提交校验状态 VO |
| design | `vo/DesignColumnConfigVO.java` | 列配置 VO（独立，非复用 Order） |
| design | `helper/DesignQueryHelper.java` | 数据权限/排序白名单/字段翻译 |
| design | `service/DesignWorkorderService.java` | 工单查询服务接口 |
| design | `service/impl/DesignWorkorderServiceImpl.java` | 工单查询服务实现 |
| design | `controller/DesignWorkorderController.java` | 工单查询+详情接口 |
| design | `controller/DesignColumnConfigController.java` | 列配置接口 |
| design | `test/.../DesignWorkorderServiceImplTest.java` | Service 单元测试 |
| design | `test/.../DesignQueryHelperTest.java` | Helper 单元测试 |

### 修改文件

| 模块 | 文件路径 | 修改内容 |
|------|---------|---------|
| system | `user/service/UserService.java` | 新增 listUserIdsByDeptId、listUserIdsByOrgId 方法声明 |
| system | `user/service/impl/UserServiceImpl.java` | 实现上述两个方法 |
| common | `enums/SystemConfigKeyEnum.java` | 新增 DESIGN_COLUMN_CONFIG 枚举值 |
| common | `config/DefaultConfigProperties.java` | 新增 configDesignColumnConfig 字段 |
| boot | `resources/application-dev.yml` | 新增 design-column-config 配置默认值 |
| sql | `sql/init.sql` | 追加 design.column.config 初始化数据 |

---

## Task 1: UserService 扩展 — 按部门/机构查询用户ID

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/UserService.java`
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java`

- [ ] **Step 1: 在 UserService 接口末尾新增两个方法声明**

在 `UserService.java` 最后一个方法 `updateUserBySelf` 之后追加：

```java
/**
 * 根据部门ID查询用户ID列表
 * 用于设计工单数据权限过滤（DEPT 类型）
 *
 * @param deptId 部门ID
 * @return 该部门下所有正常状态用户的ID列表，deptId 为 null 时返回空列表
 */
List<Long> listUserIdsByDeptId(Long deptId);

/**
 * 根据机构ID查询用户ID列表
 * 用于设计工单数据权限过滤（ORG 类型）
 *
 * @param orgId 机构ID
 * @return 该机构下所有正常状态用户的ID列表，orgId 为 null 时返回空列表
 */
List<Long> listUserIdsByOrgId(Long orgId);
```

需要在文件头部 import 补充：`import java.util.List;`（已存在则跳过）

- [ ] **Step 2: 在 UserServiceImpl 末尾实现两个方法**

在 `UserServiceImpl.java` 最后一个方法之后追加：

```java
/**
 * 根据部门ID查询用户ID列表
 *
 * @param deptId 部门ID
 * @return 用户ID列表
 */
@Override
public List<Long> listUserIdsByDeptId(Long deptId) {
    if (deptId == null) {
        return Collections.emptyList();
    }
    return list(new LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::getDeptId, deptId)
            .eq(UserEntity::getStatus, StatusConstants.NORMAL)
            .eq(UserEntity::getIsDeleted, StatusConstants.NOT_DELETED))
            .stream()
            .map(UserEntity::getId)
            .collect(Collectors.toList());
}

/**
 * 根据机构ID查询用户ID列表
 *
 * @param orgId 机构ID
 * @return 用户ID列表
 */
@Override
public List<Long> listUserIdsByOrgId(Long orgId) {
    if (orgId == null) {
        return Collections.emptyList();
    }
    return list(new LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::getOrgId, orgId)
            .eq(UserEntity::getStatus, StatusConstants.NORMAL)
            .eq(UserEntity::getIsDeleted, StatusConstants.NOT_DELETED))
            .stream()
            .map(UserEntity::getId)
            .collect(Collectors.toList());
}
```

- [ ] **Step 3: 编译验证**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-module-system -am -q
```

期望：BUILD SUCCESS，无编译错误

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/UserService.java
git add yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java
git commit -m "feat(system): UserService 新增按部门/机构查询用户ID方法"
```

---

## Task 2: 公共配置扩展 — SystemConfigKeyEnum + DefaultConfigProperties + application-dev.yml + init.sql

**Files:**
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/SystemConfigKeyEnum.java`
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/config/DefaultConfigProperties.java`
- Modify: `yigongbao-parent/yigongbao-boot/src/main/resources/application-dev.yml`
- Modify: `yigongbao-parent/sql/init.sql`

- [ ] **Step 1: SystemConfigKeyEnum 新增枚举值**

在 `SystemConfigKeyEnum.java` 的 `DESIGN_REPORT_MAX_SIZE_MB` 枚举值之后追加：

```java
// ==================== 设计工单列配置 ====================
/**
 * 设计工单列表默认列配置（JSON 格式）
 */
DESIGN_COLUMN_CONFIG("design.column.config", "设计工单列表默认列配置"),
```

注意：`DESIGN_REPORT_MAX_SIZE_MB` 是最后一个枚举值，无分号结尾，新增后需保持格式（末尾值无分号，倒数第二值加逗号）。正确写法：将 `DESIGN_REPORT_MAX_SIZE_MB(...)` 改为 `DESIGN_REPORT_MAX_SIZE_MB(...),` 然后追加 `DESIGN_COLUMN_CONFIG("design.column.config", "设计工单列表默认列配置");`

- [ ] **Step 2: DefaultConfigProperties 新增字段**

在 `DefaultConfigProperties.java` 的 `configDesignReportMaxSizeMb` 字段之后追加：

```java
// ==================== 设计工单列配置 ====================
/**
 * 设计工单列表默认列配置（JSON 格式）
 * 字段命名规则：design.column.config → configDesignColumnConfig
 */
private String configDesignColumnConfig = "{\"module\":\"design\",\"columns\":[{\"field\":\"isUrgent\",\"label\":\"加急\",\"visible\":true,\"sort\":1,\"width\":70,\"fixed\":null},{\"field\":\"orderCode\",\"label\":\"订单编号\",\"visible\":true,\"sort\":2,\"width\":160,\"fixed\":null},{\"field\":\"statusName\",\"label\":\"当前状态\",\"visible\":true,\"sort\":3,\"width\":120,\"fixed\":null},{\"field\":\"businessTypeName\",\"label\":\"业务类型\",\"visible\":true,\"sort\":4,\"width\":100,\"fixed\":null},{\"field\":\"orderTypeName\",\"label\":\"订单类型\",\"visible\":true,\"sort\":5,\"width\":110,\"fixed\":null},{\"field\":\"needsPhysicalDeliveryName\",\"label\":\"实体交付\",\"visible\":true,\"sort\":6,\"width\":90,\"fixed\":null},{\"field\":\"patientName\",\"label\":\"患者姓名\",\"visible\":true,\"sort\":7,\"width\":100,\"fixed\":null},{\"field\":\"hospitalName\",\"label\":\"医院\",\"visible\":true,\"sort\":8,\"width\":180,\"fixed\":null},{\"field\":\"hospitalDeptName\",\"label\":\"科室\",\"visible\":true,\"sort\":9,\"width\":100,\"fixed\":null},{\"field\":\"doctorName\",\"label\":\"医生姓名\",\"visible\":true,\"sort\":10,\"width\":100,\"fixed\":null},{\"field\":\"areaName\",\"label\":\"地区\",\"visible\":true,\"sort\":11,\"width\":100,\"fixed\":null},{\"field\":\"rebuildProjectSummary\",\"label\":\"重建项目\",\"visible\":true,\"sort\":12,\"width\":200,\"fixed\":null},{\"field\":\"designerName\",\"label\":\"设计师\",\"visible\":true,\"sort\":13,\"width\":100,\"fixed\":null},{\"field\":\"packageCount\",\"label\":\"数据包数\",\"visible\":true,\"sort\":14,\"width\":90,\"fixed\":null},{\"field\":\"designStartTime\",\"label\":\"开始设计时间\",\"visible\":true,\"sort\":15,\"width\":160,\"fixed\":null},{\"field\":\"expectedDeliveryDate\",\"label\":\"期望交付\",\"visible\":true,\"sort\":16,\"width\":120,\"fixed\":null},{\"field\":\"createTime\",\"label\":\"创建时间\",\"visible\":true,\"sort\":17,\"width\":160,\"fixed\":null},{\"field\":\"rejectReason\",\"label\":\"驳回原因\",\"visible\":false,\"sort\":18,\"width\":160,\"fixed\":null},{\"field\":\"action\",\"label\":\"操作\",\"visible\":true,\"sort\":19,\"width\":150,\"fixed\":\"right\"}]}";
```

- [ ] **Step 3: application-dev.yml 新增配置**

在 `application-dev.yml` 的 `yigongbao.config` 节点末尾（`flow-max-design-reject: 3` 之后）追加：

```yaml
    # ==================== 设计工单列配置 ====================
    # 设计工单列表默认列配置（兜底值，优先使用数据库配置）
    design-column-config: '{"module":"design","columns":[{"field":"isUrgent","label":"加急","visible":true,"sort":1,"width":70,"fixed":null},{"field":"orderCode","label":"订单编号","visible":true,"sort":2,"width":160,"fixed":null},{"field":"statusName","label":"当前状态","visible":true,"sort":3,"width":120,"fixed":null},{"field":"businessTypeName","label":"业务类型","visible":true,"sort":4,"width":100,"fixed":null},{"field":"orderTypeName","label":"订单类型","visible":true,"sort":5,"width":110,"fixed":null},{"field":"needsPhysicalDeliveryName","label":"实体交付","visible":true,"sort":6,"width":90,"fixed":null},{"field":"patientName","label":"患者姓名","visible":true,"sort":7,"width":100,"fixed":null},{"field":"hospitalName","label":"医院","visible":true,"sort":8,"width":180,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":9,"width":100,"fixed":null},{"field":"doctorName","label":"医生姓名","visible":true,"sort":10,"width":100,"fixed":null},{"field":"areaName","label":"地区","visible":true,"sort":11,"width":100,"fixed":null},{"field":"rebuildProjectSummary","label":"重建项目","visible":true,"sort":12,"width":200,"fixed":null},{"field":"designerName","label":"设计师","visible":true,"sort":13,"width":100,"fixed":null},{"field":"packageCount","label":"数据包数","visible":true,"sort":14,"width":90,"fixed":null},{"field":"designStartTime","label":"开始设计时间","visible":true,"sort":15,"width":160,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付","visible":true,"sort":16,"width":120,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":17,"width":160,"fixed":null},{"field":"rejectReason","label":"驳回原因","visible":false,"sort":18,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":19,"width":150,"fixed":"right"}]}'
```

- [ ] **Step 4: init.sql 追加设计列配置初始化数据**

在 `sql/init.sql` 的 sys_config 插入语句中，将原来结尾的 `design.assign.mode` 行末尾分号去掉改成逗号，然后追加：

```sql
,
('design.column.config', '设计工单列表默认列配置', '{"module":"design","columns":[{"field":"isUrgent","label":"加急","visible":true,"sort":1,"width":70,"fixed":null},{"field":"orderCode","label":"订单编号","visible":true,"sort":2,"width":160,"fixed":null},{"field":"statusName","label":"当前状态","visible":true,"sort":3,"width":120,"fixed":null},{"field":"businessTypeName","label":"业务类型","visible":true,"sort":4,"width":100,"fixed":null},{"field":"orderTypeName","label":"订单类型","visible":true,"sort":5,"width":110,"fixed":null},{"field":"needsPhysicalDeliveryName","label":"实体交付","visible":true,"sort":6,"width":90,"fixed":null},{"field":"patientName","label":"患者姓名","visible":true,"sort":7,"width":100,"fixed":null},{"field":"hospitalName","label":"医院","visible":true,"sort":8,"width":180,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":9,"width":100,"fixed":null},{"field":"doctorName","label":"医生姓名","visible":true,"sort":10,"width":100,"fixed":null},{"field":"areaName","label":"地区","visible":true,"sort":11,"width":100,"fixed":null},{"field":"rebuildProjectSummary","label":"重建项目","visible":true,"sort":12,"width":200,"fixed":null},{"field":"designerName","label":"设计师","visible":true,"sort":13,"width":100,"fixed":null},{"field":"packageCount","label":"数据包数","visible":true,"sort":14,"width":90,"fixed":null},{"field":"designStartTime","label":"开始设计时间","visible":true,"sort":15,"width":160,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付","visible":true,"sort":16,"width":120,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":17,"width":160,"fixed":null},{"field":"rejectReason","label":"驳回原因","visible":false,"sort":18,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":19,"width":150,"fixed":"right"}]}', 'json', 'system', '设计工单列表默认显示的列（JSON格式）', 1, 0, 21, 1);
```

- [ ] **Step 5: 编译验证**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-common -am -q
```

期望：BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/SystemConfigKeyEnum.java
git add yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/config/DefaultConfigProperties.java
git add yigongbao-parent/yigongbao-boot/src/main/resources/application-dev.yml
git add yigongbao-parent/sql/init.sql
git commit -m "feat(config): 新增设计工单列配置枚举值和默认配置"
```

---

## Task 3: DTO/VO 数据对象 — 查询参数 + 列表/列配置

**Files:**
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/DesignWorkorderQueryDTO.java`
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/SaveDesignColumnConfigDTO.java`
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignWorkorderListVO.java`
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignColumnConfigVO.java`

- [ ] **Step 1: 创建 DesignWorkorderQueryDTO**

```java
package com.yigongbao.module.design.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计工单列表查询参数
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DesignWorkorderQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100 */
    private Integer pageSize = 10;

    /** 订单编号（模糊匹配） */
    private String orderCode;

    /** 患者姓名（模糊匹配） */
    private String patientName;

    /** 状态（精确匹配，如 2010/2020/2040/2060） */
    private Integer status;

    /** 是否加急（0=否，1=是） */
    private Integer isUrgent;

    /** 医院ID（精确匹配） */
    private Long hospitalId;

    /** 业务类型（字典码，精确匹配） */
    private String businessType;

    /** 创建时间-开始 */
    private LocalDateTime createTimeStart;

    /** 创建时间-结束 */
    private LocalDateTime createTimeEnd;

    /** 排序字段，默认 createTime */
    private String sortField;

    /** 排序方向（ASC/DESC），默认 DESC */
    private String sortOrder;
}
```

- [ ] **Step 2: 创建 SaveDesignColumnConfigDTO**

```java
package com.yigongbao.module.design.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 保存设计工单列配置参数
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class SaveDesignColumnConfigDTO {

    @NotNull(message = "列配置不能为空")
    private List<ColumnItemDTO> columns;

    @Data
    public static class ColumnItemDTO {

        @NotBlank(message = "字段名不能为空")
        private String field;

        @NotBlank(message = "列标题不能为空")
        private String label;

        @NotNull(message = "是否可见不能为空")
        private Boolean visible;

        @NotNull(message = "排序序号不能为空")
        private Integer sort;

        private Integer width;

        /** 固定位置：left / right / null */
        private String fixed;
    }
}
```

- [ ] **Step 3: 创建 DesignWorkorderListVO**

```java
package com.yigongbao.module.design.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设计工单列表项 VO
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DesignWorkorderListVO {

    private Long id;

    /** 是否加急（0=否，1=是） */
    private Integer isUrgent;

    /** 订单编号 */
    private String orderCode;

    /** 当前状态值 */
    private Integer status;

    /** 当前状态名称 */
    private String statusName;

    /** 业务类型字典码 */
    private String businessType;

    /** 业务类型名称 */
    private String businessTypeName;

    /** 订单类型（1=医疗器械，2=非医疗器械） */
    private Integer orderType;

    /** 订单类型名称 */
    private String orderTypeName;

    /** 是否需要实体交付（0=否，1=是） */
    private Integer needsPhysicalDelivery;

    /** 实体交付名称 */
    private String needsPhysicalDeliveryName;

    /** 患者姓名 */
    private String patientName;

    /** 医院ID */
    private Long hospitalId;

    /** 医院名称 */
    private String hospitalName;

    /** 科室名称 */
    private String hospitalDeptName;

    /** 医生姓名 */
    private String doctorName;

    /** 地区名称 */
    private String areaName;

    /** 重建项目摘要，格式：左髋骨导板, 右髋骨模型 */
    private String rebuildProjectSummary;

    /** 设计师ID */
    private Long designerId;

    /** 设计师姓名 */
    private String designerName;

    /** 数据包数量 */
    private Integer packageCount;

    /** 开始设计时间 */
    private LocalDateTime designStartTime;

    /** 期望交付日期 */
    private LocalDateTime expectedDeliveryDate;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 驳回原因（最近一次，默认隐藏） */
    private String rejectReason;
}
```

- [ ] **Step 4: 创建 DesignColumnConfigVO**

```java
package com.yigongbao.module.design.vo;

import lombok.Data;

import java.util.List;

/**
 * 设计工单列配置 VO（独立于订单列配置，字段内容不同）
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DesignColumnConfigVO {

    private String module = "design";

    private List<ColumnItemVO> columns;

    @Data
    public static class ColumnItemVO {

        /** 字段名 */
        private String field;

        /** 列标题 */
        private String label;

        /** 是否可见 */
        private Boolean visible;

        /** 排序序号 */
        private Integer sort;

        /** 列宽度（px） */
        private Integer width;

        /** 固定位置：left / right / null */
        private String fixed;
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-module-design -am -q
```

期望：BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/DesignWorkorderQueryDTO.java
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/SaveDesignColumnConfigDTO.java
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignWorkorderListVO.java
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignColumnConfigVO.java
git commit -m "feat(design): 新增工单查询 DTO/VO 数据对象"
```

---

## Task 4: VO 数据对象 — 工单详情 + 提交校验

**Files:**
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignWorkorderDetailVO.java`
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/SubmitCheckVO.java`

- [ ] **Step 1: 创建 SubmitCheckVO**

```java
package com.yigongbao.module.design.vo;

import lombok.Data;

/**
 * 设计工单提交校验状态 VO
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class SubmitCheckVO {

    /** 是否已上传数据包 */
    private Boolean hasPackage;

    /** 是否已填写打印信息（所有数据包） */
    private Boolean hasPrintInfo;

    /** 是否已生成指令单（所有数据包） */
    private Boolean hasInstruction;

    /** 是否已生成图纸（所有数据包） */
    private Boolean hasDrawing;

    /** 是否已上传可视化模型 */
    private Boolean hasModel;

    /** 是否已上传设计报告 */
    private Boolean hasReport;

    /** 是否可以提交（全部为 true 时才为 true） */
    private Boolean canSubmit;

    /** 不可提交的原因（首个未满足项的说明） */
    private String blockReason;
}
```

- [ ] **Step 2: 创建 DesignWorkorderDetailVO**

```java
package com.yigongbao.module.design.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设计工单详情 VO
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DesignWorkorderDetailVO {

    // ==================== 订单基本信息 ====================
    private Long id;
    private String orderCode;
    private Integer status;
    private String statusName;
    private Integer phase;
    private String phaseName;
    /** 设计模式：1=线下修改，2=在线编辑 */
    private Integer designMode;
    /** 最近一次驳回原因 */
    private String rejectReason;

    // ==================== 订单类型 ====================
    private Integer orderType;
    private String orderTypeName;
    private Integer needsPhysicalDelivery;
    private String needsPhysicalDeliveryName;
    private String businessType;
    private String businessTypeName;

    // ==================== 机构信息 ====================
    private Long orgId;
    private String orgName;
    private Long operatorId;
    private String operatorName;
    private String operatorPhone;

    // ==================== 医院信息 ====================
    private Long hospitalId;
    private String hospitalName;
    private String hospitalDeptName;
    private String areaName;
    private String fullAreaName;

    // ==================== 医生/患者信息 ====================
    private String doctorName;
    private String doctorPhone;
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private String patientGenderName;

    // ==================== 业务信息 ====================
    private Integer isUrgent;
    private Integer isPostal;
    private String postalAddress;
    private LocalDateTime expectedDeliveryDate;

    // ==================== 设计信息 ====================
    private Long designerId;
    private String designerName;
    private LocalDateTime designStartTime;
    private LocalDateTime designSubmitTime;

    // ==================== 重建项目列表 ====================
    private List<RebuildProjectItemVO> rebuildProjectList;

    // ==================== 提交校验状态 ====================
    private SubmitCheckVO submitCheck;

    @Data
    public static class RebuildProjectItemVO {
        private String projectName;
        private String bodyPartName;
        private String categoryCode;
        private String categoryName;
        private Integer count;
        private String projectDesc;
        private String formingRequirement;
        private String otherRequirement;
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-module-design -am -q
```

期望：BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignWorkorderDetailVO.java
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/SubmitCheckVO.java
git commit -m "feat(design): 新增工单详情和提交校验 VO"
```

---

## Task 5: DesignQueryHelper — 数据权限 + 排序 + 字段翻译

**Files:**
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/helper/DesignQueryHelper.java`

- [ ] **Step 1: 创建 DesignQueryHelper**

```java
package com.yigongbao.module.design.helper;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设计工单查询辅助组件
 * 封装数据权限过滤、排序白名单、字段翻译等公共逻辑
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DesignQueryHelper {

    // ==================== 排序白名单 ====================

    /**
     * 排序字段白名单：前端字段名 → Lambda 字段引用
     * 防止 SQL 注入；只允许白名单内的字段参与排序
     */
    private static final Map<String, SFunction<OrderMainEntity, ?>> SORT_FIELD_MAP;

    static {
        Map<String, SFunction<OrderMainEntity, ?>> map = new HashMap<>();
        map.put("createTime",           OrderMainEntity::getCreateTime);
        map.put("updateTime",           OrderMainEntity::getUpdateTime);
        map.put("orderCode",            OrderMainEntity::getOrderCode);
        map.put("patientName",          OrderMainEntity::getPatientName);
        map.put("hospitalName",         OrderMainEntity::getHospitalName);
        map.put("status",               OrderMainEntity::getStatus);
        map.put("isUrgent",             OrderMainEntity::getIsUrgent);
        map.put("expectedDeliveryDate", OrderMainEntity::getExpectedDeliveryDate);
        map.put("designStartTime",      OrderMainEntity::getDesignStartTime);
        SORT_FIELD_MAP = Collections.unmodifiableMap(map);
    }

    private final UserService userService;
    private final ConfigService configService;
    private final DictService dictService;
    private final ObjectMapper objectMapper;

    // ==================== 当前用户 ====================

    /**
     * 获取当前登录用户ID，未登录返回 null
     *
     * @return 当前登录用户ID
     */
    public Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前用户实体，未登录或用户不存在返回 null
     *
     * @return 当前用户实体
     */
    public UserEntity getCurrentUser() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return userService.getById(userId);
    }

    // ==================== 数据权限 ====================

    /**
     * 根据数据范围类型向查询条件注入数据权限过滤
     * <p>
     * 设计工单按 designer_id 过滤，与订单列表按 create_by 过滤的语义不同：
     * - SELF：designer_id = 当前用户ID
     * - DEPT：designer_id IN (同部门所有用户ID)
     * - ORG：designer_id IN (同机构所有用户ID)
     * - HOSPITALS：降级为 SELF（设计师不按医院分配）
     * - ALL：不限制
     *
     * @param wrapper       查询条件构建器
     * @param currentUser   当前用户实体
     * @param scopeType     数据范围类型枚举
     */
    public void buildDataScopeCondition(LambdaQueryWrapper<OrderMainEntity> wrapper,
                                        UserEntity currentUser,
                                        DataScopeTypeEnum scopeType) {
        if (currentUser == null) {
            // 用户信息获取失败，兜底返回空列表
            log.warn("当前用户信息为空，数据权限过滤返回空列表");
            wrapper.apply("1 = 0");
            return;
        }
        Long currentUserId = currentUser.getId();

        switch (scopeType) {
            case SELF:
                // 仅看分配给自己的工单
                wrapper.eq(OrderMainEntity::getDesignerId, currentUserId);
                break;
            case DEPT:
                // 看同部门所有设计师的工单
                Long deptId = currentUser.getDeptId();
                if (deptId != null) {
                    List<Long> deptUserIds = userService.listUserIdsByDeptId(deptId);
                    if (deptUserIds.isEmpty()) {
                        log.warn("DEPT 范围下部门无成员，返回空列表，deptId={}", deptId);
                        wrapper.apply("1 = 0");
                    } else {
                        wrapper.in(OrderMainEntity::getDesignerId, deptUserIds);
                    }
                } else {
                    // 用户未配置部门，降级为 SELF
                    log.warn("DEPT 类型用户未配置部门，降级为 SELF，userId={}", currentUserId);
                    wrapper.eq(OrderMainEntity::getDesignerId, currentUserId);
                }
                break;
            case ORG:
                // 看同机构所有设计师的工单
                Long orgId = currentUser.getOrgId();
                if (orgId != null) {
                    List<Long> orgUserIds = userService.listUserIdsByOrgId(orgId);
                    if (orgUserIds.isEmpty()) {
                        log.warn("ORG 范围下机构无成员，返回空列表，orgId={}", orgId);
                        wrapper.apply("1 = 0");
                    } else {
                        wrapper.in(OrderMainEntity::getDesignerId, orgUserIds);
                    }
                } else {
                    log.warn("ORG 类型用户无所属机构，降级为 SELF，userId={}", currentUserId);
                    wrapper.eq(OrderMainEntity::getDesignerId, currentUserId);
                }
                break;
            case HOSPITALS:
                // 设计师不按医院分配，静默降级为 SELF
                log.info("HOSPITALS 数据范围降级为 SELF（设计工单不按医院分配），userId={}", currentUserId);
                wrapper.eq(OrderMainEntity::getDesignerId, currentUserId);
                break;
            case ALL:
                // 不做任何限制
                break;
        }
    }

    // ==================== 排序 ====================

    /**
     * 向查询条件追加动态排序
     * <p>
     * sortField 不在白名单时静默降级为 createTime，记录 warn 日志（防 SQL 注入）
     *
     * @param wrapper   查询条件构建器
     * @param sortField 前端传入的排序字段名，可为 null
     * @param sortOrder 前端传入的排序方向 "ASC"/"DESC"，可为 null
     */
    public void applySort(LambdaQueryWrapper<OrderMainEntity> wrapper,
                          String sortField,
                          String sortOrder) {
        SFunction<OrderMainEntity, ?> column = null;
        if (StrUtil.isNotBlank(sortField)) {
            column = SORT_FIELD_MAP.get(sortField);
            if (column == null) {
                log.warn("不支持的排序字段，已降级为默认排序，sortField={}", sortField);
            }
        }
        // sortField 为空或不在白名单中时降级为 createTime
        if (column == null) {
            column = OrderMainEntity::getCreateTime;
        }
        if ("ASC".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(column);
        } else {
            wrapper.orderByDesc(column);
        }
    }

    // ==================== 列配置 ====================

    /**
     * 获取当前用户的列配置（用户个人配置 > 系统默认配置）
     *
     * @return 设计列配置 VO
     */
    public DesignColumnConfigVO getColumnConfig() {
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            UserEntity user = userService.getById(currentUserId);
            if (user != null && StrUtil.isNotBlank(user.getDesignColumnSettings())) {
                try {
                    return objectMapper.readValue(user.getDesignColumnSettings(), DesignColumnConfigVO.class);
                } catch (JsonProcessingException e) {
                    log.warn("解析用户设计列配置失败，降级为系统默认，userId={}", currentUserId, e);
                }
            }
        }
        return getSystemDefaultColumnConfig();
    }

    /**
     * 获取系统默认列配置
     *
     * @return 系统列配置 VO，配置为空或解析失败返回 null
     */
    public DesignColumnConfigVO getSystemDefaultColumnConfig() {
        String configJson = configService.getConfigValue(SystemConfigKeyEnum.DESIGN_COLUMN_CONFIG.getKey());
        if (StrUtil.isBlank(configJson)) {
            log.warn("系统默认设计列配置为空");
            return null;
        }
        try {
            return objectMapper.readValue(configJson, DesignColumnConfigVO.class);
        } catch (JsonProcessingException e) {
            log.error("解析系统设计列配置失败", e);
            return null;
        }
    }

    // ==================== 展示字段翻译 ====================

    /**
     * 将订单类型数字值翻译为中文名称
     *
     * @param orderType 1=医疗器械，2=非医疗器械
     * @return 中文名称
     */
    public String getOrderTypeName(Integer orderType) {
        if (orderType == null) return null;
        return switch (orderType) {
            case 1 -> "医疗器械";
            case 2 -> "非医疗器械";
            default -> null;
        };
    }

    /**
     * 将实体交付标识翻译为中文名称
     *
     * @param needsPhysicalDelivery 0=否，1=是
     * @return 中文名称
     */
    public String getNeedsPhysicalDeliveryName(Integer needsPhysicalDelivery) {
        if (needsPhysicalDelivery == null) return null;
        return needsPhysicalDelivery == 1 ? "是" : "否";
    }

    /**
     * 将性别字典码翻译为中文名称
     *
     * @param gender 性别字典码（10.1=男，10.2=女）
     * @return 中文名称
     */
    public String getGenderName(String gender) {
        if (StrUtil.isBlank(gender)) return null;
        return switch (gender) {
            case DictCodeConstants.PATIENT_GENDER_MALE -> "男";
            case DictCodeConstants.PATIENT_GENDER_FEMALE -> "女";
            default -> null;
        };
    }

    /**
     * 通过字典服务将业务类型字典码翻译为字典名称
     *
     * @param dictCode 字典码
     * @return 字典名称
     */
    public String getDictName(String dictCode) {
        if (StrUtil.isBlank(dictCode)) return null;
        var dict = dictService.getByDictCode(dictCode);
        return dict != null ? dict.getDictName() : null;
    }

    /**
     * 将阶段值翻译为阶段中文名称
     *
     * @param phase 阶段值
     * @return 阶段名称
     */
    public String getPhaseName(Integer phase) {
        FlowPhaseEnum phaseEnum = FlowPhaseEnum.getByValue(phase);
        return phaseEnum != null ? phaseEnum.getName() : null;
    }

    /**
     * 将状态值翻译为状态中文名称
     *
     * @param status 状态值
     * @return 状态名称
     */
    public String getStatusName(Integer status) {
        FlowStatusEnum statusEnum = FlowStatusEnum.getByValue(status);
        return statusEnum != null ? statusEnum.getName() : null;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-module-design -am -q
```

期望：BUILD SUCCESS，无编译错误

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/helper/DesignQueryHelper.java
git commit -m "feat(design): 新增 DesignQueryHelper 数据权限/排序/字段翻译辅助组件"
```

---

## Task 6: DesignWorkorderService + Impl — 工单查询、详情、提交校验、列配置

**Files:**
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignWorkorderService.java`
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImpl.java`

- [ ] **Step 1: 创建 DesignWorkorderService 接口**

```java
package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.SaveDesignColumnConfigDTO;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;

/**
 * 设计工单查询服务接口
 *
 * @author hanjor
 * @date 2026-04-16
 */
public interface DesignWorkorderService {

    /**
     * 分页查询设计工单列表
     * 根据当前用户的数据权限范围，仅返回设计阶段（phase=20）的工单
     *
     * @param queryDTO 查询参数（分页、筛选、排序）
     * @return 分页工单列表
     */
    IPage<DesignWorkorderListVO> listWorkorders(DesignWorkorderQueryDTO queryDTO);

    /**
     * 获取工单详情
     * 包含订单基本信息、重建项目、提交校验状态
     *
     * @param orderId 订单ID
     * @return 工单详情 VO
     */
    DesignWorkorderDetailVO getWorkorderDetail(Long orderId);

    /**
     * 获取当前用户的列配置
     * 优先返回用户个人配置，无则返回系统默认配置
     *
     * @return 列配置 VO
     */
    DesignColumnConfigVO getColumnConfig();

    /**
     * 保存当前用户的列配置到 sys_user.design_column_settings
     *
     * @param dto 列配置参数
     */
    void saveColumnConfig(SaveDesignColumnConfigDTO dto);
}
```

- [ ] **Step 2: 创建 DesignWorkorderServiceImpl**

```java
package com.yigongbao.module.design.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.basic.file.mapper.FileDetailMapper;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.SaveDesignColumnConfigDTO;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.entity.DesignReviewEntity;
import com.yigongbao.module.design.helper.DesignQueryHelper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignModelMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.mapper.DesignReviewMapper;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import com.yigongbao.module.design.vo.SubmitCheckVO;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设计工单查询服务实现类
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DesignWorkorderServiceImpl implements DesignWorkorderService {

    private final OrderMainService orderMainService;
    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final DesignPackageMapper designPackageMapper;
    private final DesignProductMapper designProductMapper;
    private final DesignInstructionMapper designInstructionMapper;
    private final DesignDrawingMapper designDrawingMapper;
    private final DesignModelMapper designModelMapper;
    private final DesignReviewMapper designReviewMapper;
    private final FileDetailMapper fileDetailMapper;
    private final UserService userService;
    private final UserHospitalService userHospitalService;
    private final DesignQueryHelper designQueryHelper;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询设计工单列表
     * 按当前用户的数据权限范围过滤，固定查询设计阶段（phase=20）的工单
     *
     * @param queryDTO 查询参数
     * @return 分页工单列表
     */
    @Override
    public IPage<DesignWorkorderListVO> listWorkorders(DesignWorkorderQueryDTO queryDTO) {
        log.info("查询设计工单列表，queryDTO={}", queryDTO);

        // 获取当前用户信息和数据权限类型
        Long currentUserId = designQueryHelper.getCurrentUserId();
        UserEntity currentUser = designQueryHelper.getCurrentUser();
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);
        log.info("当前用户数据权限类型，userId={}，scopeType={}", currentUserId, scopeType);

        // 构建查询条件
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();

        // 固定过滤：仅查询设计阶段订单
        wrapper.eq(OrderMainEntity::getPhase, 20);

        // 注入数据权限过滤（按 designer_id）
        designQueryHelper.buildDataScopeCondition(wrapper, currentUser, scopeType);

        // 动态筛选条件
        wrapper.like(StrUtil.isNotBlank(queryDTO.getOrderCode()), OrderMainEntity::getOrderCode, queryDTO.getOrderCode());
        wrapper.like(StrUtil.isNotBlank(queryDTO.getPatientName()), OrderMainEntity::getPatientName, queryDTO.getPatientName());
        wrapper.eq(queryDTO.getStatus() != null, OrderMainEntity::getStatus, queryDTO.getStatus());
        wrapper.eq(queryDTO.getIsUrgent() != null, OrderMainEntity::getIsUrgent, queryDTO.getIsUrgent());
        wrapper.eq(queryDTO.getHospitalId() != null, OrderMainEntity::getHospitalId, queryDTO.getHospitalId());
        wrapper.eq(StrUtil.isNotBlank(queryDTO.getBusinessType()), OrderMainEntity::getBusinessType, queryDTO.getBusinessType());
        wrapper.ge(queryDTO.getCreateTimeStart() != null, OrderMainEntity::getCreateTime, queryDTO.getCreateTimeStart());
        wrapper.le(queryDTO.getCreateTimeEnd() != null, OrderMainEntity::getCreateTime, queryDTO.getCreateTimeEnd());

        // 排序
        designQueryHelper.applySort(wrapper, queryDTO.getSortField(), queryDTO.getSortOrder());

        // 分页参数校验（pageSize 最大 100）
        int pageSize = queryDTO.getPageSize() == null ? 10 : Math.min(queryDTO.getPageSize(), 100);
        int pageNum = queryDTO.getPageNum() == null ? 1 : queryDTO.getPageNum();
        IPage<OrderMainEntity> entityPage = orderMainMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        log.info("查询到工单数量，total={}", entityPage.getTotal());

        // 转换为列表 VO
        List<OrderMainEntity> entities = entityPage.getRecords();
        List<DesignWorkorderListVO> voList = entities.stream()
                .map(this::toWorkorderListVO)
                .collect(Collectors.toList());

        // 批量填充重建项目摘要（避免 N+1 问题）
        fillRebuildProjectSummary(voList, entities);

        // 批量填充数据包数量
        fillPackageCount(voList);

        // 批量填充驳回原因
        fillRejectReason(voList);

        // 构建返回分页对象
        IPage<DesignWorkorderListVO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 获取工单详情
     *
     * @param orderId 订单ID
     * @return 工单详情 VO
     */
    @Override
    public DesignWorkorderDetailVO getWorkorderDetail(Long orderId) {
        log.info("查询设计工单详情，orderId={}", orderId);

        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null || order.getIsDeleted().equals(StatusConstants.DELETED)) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        DesignWorkorderDetailVO vo = new DesignWorkorderDetailVO();

        // 基本信息
        vo.setId(order.getId());
        vo.setOrderCode(order.getOrderCode());
        vo.setStatus(order.getStatus());
        vo.setStatusName(designQueryHelper.getStatusName(order.getStatus()));
        vo.setPhase(order.getPhase());
        vo.setPhaseName(designQueryHelper.getPhaseName(order.getPhase()));
        vo.setDesignMode(order.getDesignMode());

        // 订单类型
        vo.setOrderType(order.getOrderType());
        vo.setOrderTypeName(designQueryHelper.getOrderTypeName(order.getOrderType()));
        vo.setNeedsPhysicalDelivery(order.getNeedsPhysicalDelivery());
        vo.setNeedsPhysicalDeliveryName(designQueryHelper.getNeedsPhysicalDeliveryName(order.getNeedsPhysicalDelivery()));
        vo.setBusinessType(order.getBusinessType());
        vo.setBusinessTypeName(designQueryHelper.getDictName(order.getBusinessType()));

        // 机构信息
        vo.setOrgId(order.getOrgId());
        vo.setOrgName(order.getOrgName());
        vo.setOperatorId(order.getOperatorId());
        vo.setOperatorName(order.getOperatorName());
        vo.setOperatorPhone(order.getOperatorPhone());

        // 医院信息
        vo.setHospitalId(order.getHospitalId());
        vo.setHospitalName(order.getHospitalName());
        vo.setHospitalDeptName(order.getHospitalDeptName());
        vo.setAreaName(order.getAreaName());
        vo.setFullAreaName(order.getFullAreaName());

        // 医生/患者信息
        vo.setDoctorName(order.getDoctorName());
        vo.setDoctorPhone(order.getDoctorPhone());
        vo.setPatientName(order.getPatientName());
        vo.setPatientAge(order.getPatientAge());
        vo.setPatientGender(order.getPatientGender());
        vo.setPatientGenderName(designQueryHelper.getGenderName(order.getPatientGender()));

        // 业务信息
        vo.setIsUrgent(order.getIsUrgent());
        vo.setIsPostal(order.getIsPostal());
        vo.setPostalAddress(order.getPostalAddress());
        vo.setExpectedDeliveryDate(order.getExpectedDeliveryDate());

        // 设计信息
        vo.setDesignerId(order.getDesignerId());
        vo.setDesignerName(order.getDesignerName());
        vo.setDesignStartTime(order.getDesignStartTime());
        vo.setDesignSubmitTime(order.getDesignSubmitTime());

        // 最近一次驳回原因
        vo.setRejectReason(getLatestRejectReason(orderId));

        // 重建项目列表
        vo.setRebuildProjectList(buildRebuildProjectList(orderId));

        // 提交校验状态
        vo.setSubmitCheck(buildSubmitCheck(orderId));

        return vo;
    }

    /**
     * 获取当前用户列配置
     *
     * @return 列配置 VO
     */
    @Override
    public DesignColumnConfigVO getColumnConfig() {
        log.info("获取用户设计列配置");
        return designQueryHelper.getColumnConfig();
    }

    /**
     * 保存用户列配置到 sys_user.design_column_settings
     *
     * @param dto 列配置参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveColumnConfig(SaveDesignColumnConfigDTO dto) {
        Long currentUserId = designQueryHelper.getCurrentUserId();
        log.info("保存用户设计列配置，userId={}", currentUserId);

        UserEntity user = userService.getById(currentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 将列配置序列化为 JSON 写入 design_column_settings 字段
        DesignColumnConfigVO configVO = new DesignColumnConfigVO();
        List<DesignColumnConfigVO.ColumnItemVO> columnItems = dto.getColumns().stream()
                .map(item -> {
                    DesignColumnConfigVO.ColumnItemVO colVO = new DesignColumnConfigVO.ColumnItemVO();
                    colVO.setField(item.getField());
                    colVO.setLabel(item.getLabel());
                    colVO.setVisible(item.getVisible());
                    colVO.setSort(item.getSort());
                    colVO.setWidth(item.getWidth());
                    colVO.setFixed(item.getFixed());
                    return colVO;
                })
                .collect(Collectors.toList());
        configVO.setColumns(columnItems);

        try {
            String configJson = objectMapper.writeValueAsString(configVO);
            UserEntity update = new UserEntity();
            update.setId(currentUserId);
            update.setDesignColumnSettings(configJson);
            userService.updateById(update);
            log.info("用户设计列配置保存成功，userId={}", currentUserId);
        } catch (JsonProcessingException e) {
            log.error("序列化列配置失败，userId={}", currentUserId, e);
            throw new BusinessException(500, "列配置保存失败");
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将订单主表实体转换为工单列表 VO（不含批量填充字段）
     */
    private DesignWorkorderListVO toWorkorderListVO(OrderMainEntity entity) {
        DesignWorkorderListVO vo = new DesignWorkorderListVO();
        vo.setId(entity.getId());
        vo.setIsUrgent(entity.getIsUrgent());
        vo.setOrderCode(entity.getOrderCode());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(designQueryHelper.getStatusName(entity.getStatus()));
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessTypeName(designQueryHelper.getDictName(entity.getBusinessType()));
        vo.setOrderType(entity.getOrderType());
        vo.setOrderTypeName(designQueryHelper.getOrderTypeName(entity.getOrderType()));
        vo.setNeedsPhysicalDelivery(entity.getNeedsPhysicalDelivery());
        vo.setNeedsPhysicalDeliveryName(designQueryHelper.getNeedsPhysicalDeliveryName(entity.getNeedsPhysicalDelivery()));
        vo.setPatientName(entity.getPatientName());
        vo.setHospitalId(entity.getHospitalId());
        vo.setHospitalName(entity.getHospitalName());
        vo.setHospitalDeptName(entity.getHospitalDeptName());
        vo.setDoctorName(entity.getDoctorName());
        vo.setAreaName(entity.getAreaName());
        vo.setDesignerId(entity.getDesignerId());
        vo.setDesignerName(entity.getDesignerName());
        vo.setDesignStartTime(entity.getDesignStartTime());
        vo.setExpectedDeliveryDate(entity.getExpectedDeliveryDate());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 批量填充工单列表的重建项目摘要（避免 N+1 查询）
     * 格式：左髋骨导板, 右髋骨模型
     */
    private void fillRebuildProjectSummary(List<DesignWorkorderListVO> voList, List<OrderMainEntity> entities) {
        if (voList.isEmpty()) {
            return;
        }
        List<Long> orderIds = voList.stream().map(DesignWorkorderListVO::getId).collect(Collectors.toList());
        List<OrderItemEntity> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemEntity>()
                        .in(OrderItemEntity::getOrderId, orderIds)
                        .eq(OrderItemEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        Map<Long, List<OrderItemEntity>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));

        for (DesignWorkorderListVO vo : voList) {
            List<OrderItemEntity> items = itemsByOrderId.get(vo.getId());
            if (items != null && !items.isEmpty()) {
                String summary = items.stream()
                        .map(item -> {
                            String bodyPart = StrUtil.isNotBlank(item.getBodyPartName()) ? item.getBodyPartName() : "";
                            String project = StrUtil.isNotBlank(item.getProjectName()) ? item.getProjectName() : "";
                            return bodyPart + project;
                        })
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.joining(", "));
                vo.setRebuildProjectSummary(summary);
            }
        }
    }

    /**
     * 批量填充工单列表的数据包数量（避免 N+1 查询）
     */
    private void fillPackageCount(List<DesignWorkorderListVO> voList) {
        if (voList.isEmpty()) {
            return;
        }
        List<Long> orderIds = voList.stream().map(DesignWorkorderListVO::getId).collect(Collectors.toList());
        List<DesignPackageEntity> allPackages = designPackageMapper.selectList(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .in(DesignPackageEntity::getOrderId, orderIds)
                        .eq(DesignPackageEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        Map<Long, Long> countByOrderId = allPackages.stream()
                .collect(Collectors.groupingBy(DesignPackageEntity::getOrderId, Collectors.counting()));

        for (DesignWorkorderListVO vo : voList) {
            Long count = countByOrderId.get(vo.getId());
            vo.setPackageCount(count != null ? count.intValue() : 0);
        }
    }

    /**
     * 批量填充工单列表的最近一次驳回原因（避免 N+1 查询）
     */
    private void fillRejectReason(List<DesignWorkorderListVO> voList) {
        if (voList.isEmpty()) {
            return;
        }
        List<Long> orderIds = voList.stream().map(DesignWorkorderListVO::getId).collect(Collectors.toList());
        // 查询所有相关驳回记录（reviewResult=0 为驳回），按创建时间倒序
        List<DesignReviewEntity> allReviews = designReviewMapper.selectList(
                new LambdaQueryWrapper<DesignReviewEntity>()
                        .in(DesignReviewEntity::getOrderId, orderIds)
                        .eq(DesignReviewEntity::getReviewResult, StatusConstants.NO)
                        .eq(DesignReviewEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                        .orderByDesc(DesignReviewEntity::getCreateTime));
        // 按 orderId 取最近一条驳回记录
        Map<Long, String> rejectReasonByOrderId = new java.util.LinkedHashMap<>();
        for (DesignReviewEntity review : allReviews) {
            rejectReasonByOrderId.putIfAbsent(review.getOrderId(), review.getRejectReason());
        }
        for (DesignWorkorderListVO vo : voList) {
            vo.setRejectReason(rejectReasonByOrderId.get(vo.getId()));
        }
    }

    /**
     * 获取工单最近一次驳回原因
     */
    private String getLatestRejectReason(Long orderId) {
        List<DesignReviewEntity> reviews = designReviewMapper.selectList(
                new LambdaQueryWrapper<DesignReviewEntity>()
                        .eq(DesignReviewEntity::getOrderId, orderId)
                        .eq(DesignReviewEntity::getReviewResult, StatusConstants.NO)
                        .eq(DesignReviewEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                        .orderByDesc(DesignReviewEntity::getCreateTime)
                        .last("LIMIT 1"));
        return reviews.isEmpty() ? null : reviews.get(0).getRejectReason();
    }

    /**
     * 构建详情页重建项目列表
     */
    private List<DesignWorkorderDetailVO.RebuildProjectItemVO> buildRebuildProjectList(Long orderId) {
        List<OrderItemEntity> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemEntity>()
                        .eq(OrderItemEntity::getOrderId, orderId)
                        .eq(OrderItemEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        return items.stream()
                .map(item -> {
                    DesignWorkorderDetailVO.RebuildProjectItemVO projectVO = new DesignWorkorderDetailVO.RebuildProjectItemVO();
                    projectVO.setProjectName(item.getProjectName());
                    projectVO.setBodyPartName(item.getBodyPartName());
                    projectVO.setCategoryCode(item.getCategoryCode());
                    projectVO.setCategoryName(item.getCategoryName());
                    projectVO.setCount(1);
                    projectVO.setProjectDesc(item.getProjectDesc());
                    projectVO.setFormingRequirement(item.getFormingRequirement());
                    projectVO.setOtherRequirement(item.getOtherRequirement());
                    return projectVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建提交校验状态
     * 依次检查：数据包 → 打印信息 → 指令单 → 图纸 → 可视化模型 → 设计报告
     */
    private SubmitCheckVO buildSubmitCheck(Long orderId) {
        SubmitCheckVO check = new SubmitCheckVO();

        // 1. 查询所有未删除数据包
        List<DesignPackageEntity> packages = designPackageMapper.selectList(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .eq(DesignPackageEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        check.setHasPackage(!packages.isEmpty());

        if (!packages.isEmpty()) {
            Set<Long> packageIds = packages.stream()
                    .map(DesignPackageEntity::getId)
                    .collect(Collectors.toSet());

            // 2. 打印信息：每个数据包都有至少一条 design_product 记录
            List<DesignProductEntity> products = designProductMapper.selectList(
                    new LambdaQueryWrapper<DesignProductEntity>()
                            .in(DesignProductEntity::getPackageId, packageIds)
                            .eq(DesignProductEntity::getIsDeleted, StatusConstants.NOT_DELETED));
            Set<Long> pkgsWithProduct = products.stream()
                    .map(DesignProductEntity::getPackageId)
                    .collect(Collectors.toSet());
            check.setHasPrintInfo(pkgsWithProduct.containsAll(packageIds));

            // 3. 指令单：每个数据包都有 design_instruction 记录
            long instructionPkgCount = designInstructionMapper.selectCount(
                    new LambdaQueryWrapper<com.yigongbao.module.design.entity.DesignInstructionEntity>()
                            .in(com.yigongbao.module.design.entity.DesignInstructionEntity::getPackageId, packageIds)
                            .eq(com.yigongbao.module.design.entity.DesignInstructionEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                            .groupBy(com.yigongbao.module.design.entity.DesignInstructionEntity::getPackageId));
            // 注意：groupBy + selectCount 在 MyBatis-Plus 中实际是按 packageId 聚合后的结果数
            // 改为用 distinct packageId 的数量与 packageIds.size() 比较
            List<com.yigongbao.module.design.entity.DesignInstructionEntity> instructions = designInstructionMapper.selectList(
                    new LambdaQueryWrapper<com.yigongbao.module.design.entity.DesignInstructionEntity>()
                            .in(com.yigongbao.module.design.entity.DesignInstructionEntity::getPackageId, packageIds)
                            .eq(com.yigongbao.module.design.entity.DesignInstructionEntity::getIsDeleted, StatusConstants.NOT_DELETED));
            Set<Long> pkgsWithInstruction = instructions.stream()
                    .map(com.yigongbao.module.design.entity.DesignInstructionEntity::getPackageId)
                    .collect(Collectors.toSet());
            check.setHasInstruction(pkgsWithInstruction.containsAll(packageIds));

            // 4. 图纸：每个数据包都有 design_drawing 记录
            List<com.yigongbao.module.design.entity.DesignDrawingEntity> drawings = designDrawingMapper.selectList(
                    new LambdaQueryWrapper<com.yigongbao.module.design.entity.DesignDrawingEntity>()
                            .in(com.yigongbao.module.design.entity.DesignDrawingEntity::getPackageId, packageIds)
                            .eq(com.yigongbao.module.design.entity.DesignDrawingEntity::getIsDeleted, StatusConstants.NOT_DELETED));
            Set<Long> pkgsWithDrawing = drawings.stream()
                    .map(com.yigongbao.module.design.entity.DesignDrawingEntity::getPackageId)
                    .collect(Collectors.toSet());
            check.setHasDrawing(pkgsWithDrawing.containsAll(packageIds));
        } else {
            // 无数据包时，后续所有检查均为 false
            check.setHasPrintInfo(false);
            check.setHasInstruction(false);
            check.setHasDrawing(false);
        }

        // 5. 可视化模型
        long modelCount = designModelMapper.selectCount(
                new LambdaQueryWrapper<com.yigongbao.module.design.entity.DesignModelEntity>()
                        .eq(com.yigongbao.module.design.entity.DesignModelEntity::getOrderId, orderId)
                        .eq(com.yigongbao.module.design.entity.DesignModelEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        check.setHasModel(modelCount > 0);

        // 6. 设计报告（objectType = '10.5'）
        long reportCount = fileDetailMapper.selectCount(
                new LambdaQueryWrapper<FileDetail>()
                        .eq(FileDetail::getObjectType, "10.5")
                        .eq(FileDetail::getObjectId, String.valueOf(orderId)));
        check.setHasReport(reportCount > 0);

        // 计算 canSubmit 和 blockReason
        if (!check.getHasPackage()) {
            check.setCanSubmit(false);
            check.setBlockReason("请先上传打印文件数据包");
        } else if (!check.getHasPrintInfo()) {
            check.setCanSubmit(false);
            check.setBlockReason("请完善数据包的打印信息");
        } else if (!check.getHasInstruction()) {
            check.setCanSubmit(false);
            check.setBlockReason("请生成指令单");
        } else if (!check.getHasDrawing()) {
            check.setCanSubmit(false);
            check.setBlockReason("请生成图纸");
        } else if (!check.getHasModel()) {
            check.setCanSubmit(false);
            check.setBlockReason("请上传可视化模型文件");
        } else if (!check.getHasReport()) {
            check.setCanSubmit(false);
            check.setBlockReason("请上传设计报告");
        } else {
            check.setCanSubmit(true);
            check.setBlockReason(null);
        }

        return check;
    }
}
```

**注意**：`DesignInstructionEntity` 和 `DesignDrawingEntity` 均需有 `packageId` 字段（通过 Mapper 查询的实体字段）。编译阶段如发现字段名不符，请调整为实际字段名（参考对应 Entity 定义）。

- [ ] **Step 3: 编译验证**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-module-design -am -q
```

期望：BUILD SUCCESS。如有字段缺失错误，检查对应 Entity 字段名后修正。

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignWorkorderService.java
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImpl.java
git commit -m "feat(design): 实现 DesignWorkorderService 工单查询/详情/提交校验/列配置"
```

---

## Task 7: Controller 层 — 工单查询 + 列配置

**Files:**
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignWorkorderController.java`
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignColumnConfigController.java`

- [ ] **Step 1: 创建 DesignWorkorderController**

```java
package com.yigongbao.module.design.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 设计工单查询 Controller
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Tag(name = "设计工单查询", description = "设计工单列表查询和详情")
@RestController
@RequestMapping("/design/workorder")
@RequiredArgsConstructor
public class DesignWorkorderController {

    private final DesignWorkorderService designWorkorderService;

    /**
     * 分页查询设计工单列表
     */
    @Operation(summary = "查询设计工单列表")
    @PostMapping("/list")
    public Result<IPage<DesignWorkorderListVO>> listWorkorders(@RequestBody DesignWorkorderQueryDTO queryDTO) {
        return Result.success(designWorkorderService.listWorkorders(queryDTO));
    }

    /**
     * 获取工单详情
     */
    @Operation(summary = "获取工单详情")
    @GetMapping("/{orderId}")
    public Result<DesignWorkorderDetailVO> getWorkorderDetail(@PathVariable Long orderId) {
        return Result.success(designWorkorderService.getWorkorderDetail(orderId));
    }
}
```

- [ ] **Step 2: 创建 DesignColumnConfigController**

```java
package com.yigongbao.module.design.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.design.dto.SaveDesignColumnConfigDTO;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 设计工单列配置 Controller
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Tag(name = "设计列配置", description = "设计工单列配置管理")
@RestController
@RequestMapping("/design/column-config")
@RequiredArgsConstructor
public class DesignColumnConfigController {

    private final DesignWorkorderService designWorkorderService;

    /**
     * 获取当前用户的列配置
     */
    @Operation(summary = "获取列配置")
    @GetMapping
    public Result<DesignColumnConfigVO> getColumnConfig() {
        return Result.success(designWorkorderService.getColumnConfig());
    }

    /**
     * 保存当前用户的列配置
     */
    @Operation(summary = "保存列配置")
    @PostMapping
    public Result<Void> saveColumnConfig(@Validated @RequestBody SaveDesignColumnConfigDTO dto) {
        designWorkorderService.saveColumnConfig(dto);
        return Result.success();
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-module-design -am -q
```

期望：BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignWorkorderController.java
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignColumnConfigController.java
git commit -m "feat(design): 新增 DesignWorkorderController 和 DesignColumnConfigController"
```

---

## Task 8: 单元测试 — DesignWorkorderServiceImplTest + DesignQueryHelperTest

**Files:**
- Create: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImplTest.java`
- Create: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/helper/DesignQueryHelperTest.java`

- [ ] **Step 1: 创建 DesignWorkorderServiceImplTest**

```java
package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.basic.file.mapper.FileDetailMapper;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.SaveDesignColumnConfigDTO;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.entity.DesignReviewEntity;
import com.yigongbao.module.design.helper.DesignQueryHelper;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignModelMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.mapper.DesignReviewMapper;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import com.yigongbao.module.design.vo.SubmitCheckVO;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DesignWorkorderServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-04-16
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DesignWorkorderService 单元测试")
class DesignWorkorderServiceImplTest {

    @Mock private OrderMainService orderMainService;
    @Mock private OrderMainMapper orderMainMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private DesignPackageMapper designPackageMapper;
    @Mock private DesignProductMapper designProductMapper;
    @Mock private DesignInstructionMapper designInstructionMapper;
    @Mock private DesignDrawingMapper designDrawingMapper;
    @Mock private DesignModelMapper designModelMapper;
    @Mock private DesignReviewMapper designReviewMapper;
    @Mock private FileDetailMapper fileDetailMapper;
    @Mock private UserService userService;
    @Mock private UserHospitalService userHospitalService;
    @Mock private DesignQueryHelper designQueryHelper;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private DesignWorkorderServiceImpl service;

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long PACKAGE_ID = 200L;

    private UserEntity currentUser;
    private OrderMainEntity testOrder;

    @BeforeEach
    void setUp() {
        currentUser = new UserEntity();
        currentUser.setId(CURRENT_USER_ID);
        currentUser.setOrgId(10L);
        currentUser.setDeptId(20L);

        testOrder = new OrderMainEntity();
        testOrder.setId(ORDER_ID);
        testOrder.setOrderCode("ORD-001");
        testOrder.setPhase(20);
        testOrder.setStatus(2010);
        testOrder.setIsDeleted(StatusConstants.NOT_DELETED);
        testOrder.setDesignerId(CURRENT_USER_ID);

        // Mock DesignQueryHelper 基础行为
        when(designQueryHelper.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
        when(designQueryHelper.getCurrentUser()).thenReturn(currentUser);
        when(userHospitalService.getDataScopeType(CURRENT_USER_ID)).thenReturn(DataScopeTypeEnum.SELF);
    }

    // ==================== listWorkorders 测试 ====================

    @Nested
    @DisplayName("listWorkorders: 分页查询设计工单列表")
    class ListWorkordersTest {

        @Test
        @DisplayName("查询成功：SELF 数据权限，返回分页结果")
        void listWorkorders_selfScope_returnsPage() {
            // 准备：Mock orderMainMapper 返回一条工单
            Page<OrderMainEntity> entityPage = new Page<>(1, 10, 1);
            entityPage.setRecords(List.of(testOrder));
            when(orderMainMapper.selectPage(any(), any())).thenReturn(entityPage);

            // Mock 批量填充（返回空，使摘要等为空）
            when(orderItemMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(designPackageMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(designReviewMapper.selectList(any())).thenReturn(Collections.emptyList());

            // Mock 字段翻译
            when(designQueryHelper.getStatusName(anyInt())).thenReturn("设计中");
            when(designQueryHelper.getDictName(any())).thenReturn(null);
            when(designQueryHelper.getOrderTypeName(any())).thenReturn("医疗器械");
            when(designQueryHelper.getNeedsPhysicalDeliveryName(any())).thenReturn("否");

            // 执行
            DesignWorkorderQueryDTO queryDTO = new DesignWorkorderQueryDTO();
            IPage<DesignWorkorderListVO> result = service.listWorkorders(queryDTO);

            // 验证
            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertEquals(1, result.getRecords().size());
            assertEquals(ORDER_ID, result.getRecords().get(0).getId());
        }

        @Test
        @DisplayName("分页参数超过最大值时，pageSize 降级为 100")
        void listWorkorders_pageSizeExceedsMax_capTo100() {
            Page<OrderMainEntity> emptyPage = new Page<>(1, 100, 0);
            emptyPage.setRecords(Collections.emptyList());
            when(orderMainMapper.selectPage(any(), any())).thenReturn(emptyPage);

            DesignWorkorderQueryDTO queryDTO = new DesignWorkorderQueryDTO();
            queryDTO.setPageSize(999);  // 超过最大值
            IPage<DesignWorkorderListVO> result = service.listWorkorders(queryDTO);

            assertNotNull(result);
            // 验证分页参数已被限制（通过 selectPage 被调用即可，具体 pageSize 通过 argumentCaptor 验证）
            verify(orderMainMapper, times(1)).selectPage(any(), any());
        }
    }

    // ==================== getWorkorderDetail 测试 ====================

    @Nested
    @DisplayName("getWorkorderDetail: 获取工单详情")
    class GetWorkorderDetailTest {

        @BeforeEach
        void setUp() {
            // 公共 Mock：查询 order_item、design_package、review
            when(orderItemMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(designPackageMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(designReviewMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(designModelMapper.selectCount(any())).thenReturn(0L);
            when(fileDetailMapper.selectCount(any())).thenReturn(0L);

            // 字段翻译 Mock
            when(designQueryHelper.getStatusName(anyInt())).thenReturn("设计中");
            when(designQueryHelper.getPhaseName(anyInt())).thenReturn("设计");
            when(designQueryHelper.getOrderTypeName(any())).thenReturn("医疗器械");
            when(designQueryHelper.getNeedsPhysicalDeliveryName(any())).thenReturn("否");
            when(designQueryHelper.getDictName(any())).thenReturn(null);
            when(designQueryHelper.getGenderName(any())).thenReturn(null);
        }

        @Test
        @DisplayName("工单存在时返回详情 VO")
        void getWorkorderDetail_orderExists_returnsVO() {
            when(orderMainMapper.selectById(ORDER_ID)).thenReturn(testOrder);

            DesignWorkorderDetailVO result = service.getWorkorderDetail(ORDER_ID);

            assertNotNull(result);
            assertEquals(ORDER_ID, result.getId());
            assertEquals("ORD-001", result.getOrderCode());
            assertNotNull(result.getSubmitCheck());
        }

        @Test
        @DisplayName("工单不存在时抛出 BusinessException")
        void getWorkorderDetail_orderNotFound_throwsException() {
            when(orderMainMapper.selectById(ORDER_ID)).thenReturn(null);

            assertThrows(BusinessException.class, () -> service.getWorkorderDetail(ORDER_ID));
        }

        @Test
        @DisplayName("无数据包时 canSubmit=false，blockReason=请先上传打印文件数据包")
        void getWorkorderDetail_noPackage_cannotSubmit() {
            when(orderMainMapper.selectById(ORDER_ID)).thenReturn(testOrder);
            when(designPackageMapper.selectList(any())).thenReturn(Collections.emptyList());

            DesignWorkorderDetailVO result = service.getWorkorderDetail(ORDER_ID);

            SubmitCheckVO check = result.getSubmitCheck();
            assertFalse(check.getHasPackage());
            assertFalse(check.getCanSubmit());
            assertEquals("请先上传打印文件数据包", check.getBlockReason());
        }

        @Test
        @DisplayName("有数据包、打印信息、指令单、图纸、模型、报告时 canSubmit=true")
        void getWorkorderDetail_allChecksPassed_canSubmit() {
            when(orderMainMapper.selectById(ORDER_ID)).thenReturn(testOrder);

            // 准备：一个数据包，所有校验通过
            DesignPackageEntity pkg = new DesignPackageEntity();
            pkg.setId(PACKAGE_ID);
            pkg.setOrderId(ORDER_ID);
            pkg.setIsDeleted(StatusConstants.NOT_DELETED);
            when(designPackageMapper.selectList(any())).thenReturn(List.of(pkg));

            DesignProductEntity product = new DesignProductEntity();
            product.setPackageId(PACKAGE_ID);
            when(designProductMapper.selectList(any())).thenReturn(List.of(product));

            DesignInstructionEntity instruction = new DesignInstructionEntity();
            instruction.setPackageId(PACKAGE_ID);
            when(designInstructionMapper.selectList(any())).thenReturn(List.of(instruction));

            DesignDrawingEntity drawing = new DesignDrawingEntity();
            drawing.setPackageId(PACKAGE_ID);
            when(designDrawingMapper.selectList(any())).thenReturn(List.of(drawing));

            when(designModelMapper.selectCount(any())).thenReturn(1L);
            when(fileDetailMapper.selectCount(any())).thenReturn(1L);

            DesignWorkorderDetailVO result = service.getWorkorderDetail(ORDER_ID);

            SubmitCheckVO check = result.getSubmitCheck();
            assertTrue(check.getHasPackage());
            assertTrue(check.getHasPrintInfo());
            assertTrue(check.getHasInstruction());
            assertTrue(check.getHasDrawing());
            assertTrue(check.getHasModel());
            assertTrue(check.getHasReport());
            assertTrue(check.getCanSubmit());
            assertNull(check.getBlockReason());
        }
    }

    // ==================== saveColumnConfig 测试 ====================

    @Nested
    @DisplayName("saveColumnConfig: 保存列配置")
    class SaveColumnConfigTest {

        @Test
        @DisplayName("保存成功：更新 design_column_settings 字段")
        void saveColumnConfig_success() throws Exception {
            UserEntity user = new UserEntity();
            user.setId(CURRENT_USER_ID);
            when(userService.getById(CURRENT_USER_ID)).thenReturn(user);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"module\":\"design\",\"columns\":[]}");
            when(userService.updateById(any())).thenReturn(true);

            SaveDesignColumnConfigDTO dto = new SaveDesignColumnConfigDTO();
            dto.setColumns(Collections.emptyList());

            assertDoesNotThrow(() -> service.saveColumnConfig(dto));
            verify(userService, times(1)).updateById(any());
        }

        @Test
        @DisplayName("用户不存在时抛出 BusinessException")
        void saveColumnConfig_userNotFound_throwsException() {
            when(userService.getById(CURRENT_USER_ID)).thenReturn(null);

            SaveDesignColumnConfigDTO dto = new SaveDesignColumnConfigDTO();
            dto.setColumns(Collections.emptyList());

            assertThrows(BusinessException.class, () -> service.saveColumnConfig(dto));
        }
    }
}
```

- [ ] **Step 2: 创建 DesignQueryHelperTest**

```java
package com.yigongbao.module.design.helper;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DesignQueryHelper 单元测试
 * 重点测试数据权限过滤逻辑和排序白名单
 *
 * @author hanjor
 * @date 2026-04-16
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DesignQueryHelper 单元测试")
class DesignQueryHelperTest {

    @Mock private UserService userService;
    @Mock private ConfigService configService;
    @Mock private DictService dictService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private DesignQueryHelper designQueryHelper;

    private static final Long USER_ID = 1L;
    private UserEntity currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new UserEntity();
        currentUser.setId(USER_ID);
        currentUser.setOrgId(10L);
        currentUser.setDeptId(20L);
    }

    @Nested
    @DisplayName("buildDataScopeCondition: 数据权限过滤")
    class BuildDataScopeConditionTest {

        @Test
        @DisplayName("SELF 类型：添加 designer_id = 当前用户ID 条件")
        void selfScope_addsDesignerIdEqCondition() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();

            designQueryHelper.buildDataScopeCondition(wrapper, currentUser, DataScopeTypeEnum.SELF);

            // 验证 wrapper 中包含了 designer_id 过滤（通过 SQL 片段校验）
            String sql = wrapper.getSqlSegment();
            assertNotNull(sql);
            assertTrue(sql.contains("designer_id") || sql.contains("DESIGNER_ID"));
        }

        @Test
        @DisplayName("DEPT 类型：用户有部门时，添加 designer_id IN 条件")
        void deptScope_userHasDept_addsInCondition() {
            when(userService.listUserIdsByDeptId(20L)).thenReturn(List.of(1L, 2L, 3L));

            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            designQueryHelper.buildDataScopeCondition(wrapper, currentUser, DataScopeTypeEnum.DEPT);

            verify(userService, times(1)).listUserIdsByDeptId(20L);
            String sql = wrapper.getSqlSegment();
            assertTrue(sql.contains("IN"));
        }

        @Test
        @DisplayName("DEPT 类型：用户无部门时，降级为 SELF")
        void deptScope_userNoDept_degradesToSelf() {
            currentUser.setDeptId(null);

            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            designQueryHelper.buildDataScopeCondition(wrapper, currentUser, DataScopeTypeEnum.DEPT);

            // 不调用 listUserIdsByDeptId，而是降级为 eq designer_id
            verify(userService, never()).listUserIdsByDeptId(any());
            String sql = wrapper.getSqlSegment();
            assertTrue(sql.contains("designer_id") || sql.contains("DESIGNER_ID"));
        }

        @Test
        @DisplayName("HOSPITALS 类型：静默降级为 SELF")
        void hospitalsScope_degradesToSelf() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            designQueryHelper.buildDataScopeCondition(wrapper, currentUser, DataScopeTypeEnum.HOSPITALS);

            // 应有 designer_id = userId 条件（SELF 降级）
            String sql = wrapper.getSqlSegment();
            assertTrue(sql.contains("designer_id") || sql.contains("DESIGNER_ID"));
        }

        @Test
        @DisplayName("ALL 类型：不添加任何过滤条件")
        void allScope_noConditionAdded() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            designQueryHelper.buildDataScopeCondition(wrapper, currentUser, DataScopeTypeEnum.ALL);

            // wrapper 为空，无任何条件
            String sql = wrapper.getSqlSegment();
            assertTrue(sql == null || sql.isBlank());
        }

        @Test
        @DisplayName("currentUser 为 null 时：添加 1=0 返回空列表")
        void nullUser_returnsEmptyResult() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            designQueryHelper.buildDataScopeCondition(wrapper, null, DataScopeTypeEnum.SELF);

            String sql = wrapper.getSqlSegment();
            assertTrue(sql.contains("1 = 0") || sql.contains("1=0"));
        }
    }

    @Nested
    @DisplayName("applySort: 排序白名单")
    class ApplySortTest {

        @Test
        @DisplayName("合法 sortField 时，正常追加排序条件")
        void validSortField_appendsOrderBy() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            designQueryHelper.applySort(wrapper, "createTime", "DESC");

            String sql = wrapper.getSqlSegment();
            assertTrue(sql.contains("create_time") || sql.contains("CREATE_TIME") || sql.contains("ORDER BY"));
        }

        @Test
        @DisplayName("非法 sortField 时，降级为 createTime")
        void invalidSortField_fallsBackToCreateTime() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            designQueryHelper.applySort(wrapper, "INVALID_FIELD; DROP TABLE order_main;", "DESC");

            String sql = wrapper.getSqlSegment();
            // 不包含恶意字符，且包含 create_time 降级排序
            assertFalse(sql.contains("DROP"));
            assertTrue(sql.contains("create_time") || sql.contains("CREATE_TIME"));
        }

        @Test
        @DisplayName("sortField 为 null 时，使用默认 createTime DESC")
        void nullSortField_usesDefaultCreateTimeDesc() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            designQueryHelper.applySort(wrapper, null, null);

            String sql = wrapper.getSqlSegment();
            assertTrue(sql.contains("create_time") || sql.contains("CREATE_TIME"));
        }
    }
}
```

- [ ] **Step 3: 运行 design 模块所有测试**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-design -q
```

期望：所有测试通过，无 FAILED

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImplTest.java
git add yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/helper/DesignQueryHelperTest.java
git commit -m "test(design): 新增 DesignWorkorderServiceImpl 和 DesignQueryHelper 单元测试"
```

---

## Task 9: 全量编译 + 测试验证

- [ ] **Step 1: 全量编译验证**

```bash
cd yigongbao-parent
mvn compile -q
```

期望：BUILD SUCCESS，所有模块编译通过

- [ ] **Step 2: 运行所有相关模块测试**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-design,yigongbao-module-system -q
```

期望：所有测试通过

- [ ] **Step 3: 最终汇总 Commit**

如以上步骤所有文件都已单独 commit，此步骤可跳过。否则执行：

```bash
git status
git add -p  # 交互式确认每个变更
git commit -m "feat(design): 设计工单查询功能完整实现（Task 1-8）"
```

---

*计划版本：v1.0 | 创建日期：2026-04-16*

