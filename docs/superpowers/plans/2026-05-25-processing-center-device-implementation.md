# 加工中心与设备管理模块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现加工中心管理和设备管理两个模块,支持多加工中心协同生产场景,通过WebSocket实时接收设备状态推送

**Architecture:** 
- 基于yigongbao-module-basic模块扩展,新增processingCenter和device两个子模块
- 使用Spring WebSocket(原生)接收设备状态推送
- 采用标准分层架构: Controller → Service → Mapper → Entity
- 设备离线检测使用@Scheduled定时任务

**Tech Stack:** 
- Spring Boot 3.x
- MyBatis Plus 3.5.8
- Spring WebSocket
- MySQL 8.0
- Lombok

**参考文档:**
- PRD: `docs/superpowers/plans/2026-05-25-processing-center-device-management-prd.md`
- 编码规范: `.claude/rules/java-coding-standards.md`
- 日志规范: `.claude/rules/logging-standards.md`

---

## 任务概览

**Phase 1: 数据库与基础框架 (Tasks 1-3)**
- Task 1: 创建数据库表
- Task 2: 创建基础Entity和Mapper
- Task 3: 创建枚举类和常量

**Phase 2: 加工中心管理 (Tasks 4-6)**
- Task 4: 加工中心Service层
- Task 5: 加工中心Controller层
- Task 6: 加工中心单元测试

**Phase 3: 设备管理基础 (Tasks 7-9)**
- Task 7: 设备Service层(基础CRUD)
- Task 8: 设备Controller层
- Task 9: 设备单元测试

**Phase 4: WebSocket功能 (Tasks 10-12)**
- Task 10: WebSocket配置和连接管理器
- Task 11: WebSocket处理器和批量更新
- Task 12: WebSocket集成测试

**Phase 5: 设备离线检测 (Task 13)**
- Task 13: 定时任务和离线检测

**Phase 6: 集成测试 (Task 14)**
- Task 14: 端到端集成测试

---

## Task 1: 创建数据库表

**目标:** 创建processing_center、device、device_state_log三张表

**Files:**
- Modify: `yigongbao-parent/sql/ddl.sql` (追加内容)

- [ ] **Step 1: 添加processing_center表DDL**

在`sql/ddl.sql`文件末尾追加:

```sql
-- 加工中心表
CREATE TABLE processing_center (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    center_code VARCHAR(50) NOT NULL COMMENT '中心编码',
    center_name VARCHAR(100) NOT NULL COMMENT '中心名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(200) COMMENT '地址',
    device_id_ranges TEXT COMMENT '可用设备ID范围（JSON）',
    status TINYINT DEFAULT 1 COMMENT '状态（0=禁用，1=启用）',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    KEY idx_status (status),
    KEY idx_center_name (center_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加工中心表';

CREATE UNIQUE INDEX uk_center_code
    ON processing_center ((CASE WHEN is_deleted = 0 THEN center_code ELSE NULL END));
```

- [ ] **Step 2: 添加device表DDL**

```sql
-- 设备表
CREATE TABLE device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id VARCHAR(50) NOT NULL COMMENT '设备编号',
    device_name VARCHAR(100) COMMENT '设备名称',
    device_type VARCHAR(50) COMMENT '设备类型',
    center_id BIGINT COMMENT '所属加工中心ID',
    center_name VARCHAR(100) COMMENT '所属加工中心名称',
    state TINYINT DEFAULT 0 COMMENT '设备状态（0=空闲，1=占用）',
    connection_status TINYINT DEFAULT 0 COMMENT '连接状态（0=离线，1=在线）',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    KEY idx_center_id (center_id),
    KEY idx_device_type (device_type),
    KEY idx_state (state),
    KEY idx_connection_status (connection_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

CREATE UNIQUE INDEX uk_device_id
    ON device ((CASE WHEN is_deleted = 0 THEN device_id ELSE NULL END));
```

- [ ] **Step 3: 添加device_state_log表DDL**

```sql
-- 设备状态变更日志表
CREATE TABLE device_state_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id VARCHAR(50) NOT NULL COMMENT '设备编号',
    old_state TINYINT COMMENT '旧状态',
    new_state TINYINT NOT NULL COMMENT '新状态',
    change_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    change_type VARCHAR(20) COMMENT '变更类型（auto=自动，manual=手动）',
    operator_id BIGINT COMMENT '操作人ID',
    KEY idx_device_id (device_id),
    KEY idx_change_time (change_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态变更日志';
```

- [ ] **Step 4: Commit**

```bash
git add sql/ddl.sql
git commit -m "feat: 添加加工中心和设备管理表结构"
```

---

## Task 2: 创建基础Entity和Mapper

**目标:** 创建ProcessingCenterEntity、DeviceEntity、DeviceStateLogEntity及对应的Mapper接口

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/entity/ProcessingCenterEntity.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/mapper/ProcessingCenterMapper.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/entity/DeviceEntity.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/entity/DeviceStateLogEntity.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/mapper/DeviceMapper.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/mapper/DeviceStateLogMapper.java`

- [ ] **Step 1: 创建ProcessingCenterEntity**

```java
package com.yigongbao.module.basic.processingCenter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("processing_center")
public class ProcessingCenterEntity extends BaseEntity {
    private String centerCode;
    private String centerName;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String deviceIdRanges;
    private Integer status;
    private String remark;
}
```

- [ ] **Step 2: 创建ProcessingCenterMapper**

```java
package com.yigongbao.module.basic.processingCenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessingCenterMapper extends BaseMapper<ProcessingCenterEntity> {
}
```

- [ ] **Step 3: 创建DeviceEntity**

```java
package com.yigongbao.module.basic.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device")
public class DeviceEntity extends BaseEntity {
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private Long centerId;
    private String centerName;
    private Integer state;
    private Integer connectionStatus;
    private LocalDateTime lastHeartbeat;
    private String remark;
}
```

- [ ] **Step 4: 创建DeviceStateLogEntity**

```java
package com.yigongbao.module.basic.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_state_log")
public class DeviceStateLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private Integer oldState;
    private Integer newState;
    private LocalDateTime changeTime;
    private String changeType;
    private Long operatorId;
}
```

- [ ] **Step 5: 创建DeviceMapper和DeviceStateLogMapper**

```java
package com.yigongbao.module.basic.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceMapper extends BaseMapper<DeviceEntity> {
}
```

```java
package com.yigongbao.module.basic.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.device.entity.DeviceStateLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceStateLogMapper extends BaseMapper<DeviceStateLogEntity> {
}
```

- [ ] **Step 6: Commit**

```bash
git add yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/
git add yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/
git commit -m "feat: 添加加工中心和设备Entity及Mapper"
```

---

## Task 3: 创建枚举类

**目标:** 创建DeviceTypeEnum设备类型枚举

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/enums/DeviceTypeEnum.java`

- [ ] **Step 1: 创建DeviceTypeEnum**

```java
package com.yigongbao.module.basic.device.enums;

import lombok.Getter;

@Getter
public enum DeviceTypeEnum {
    PRINTER_SLA("PRINTER_SLA", "光固化3D打印机"),
    WASH_CONTAINER("WASH_CONTAINER", "酒精容器"),
    UV_CURING("UV_CURING", "UV固化机"),
    ULTRASONIC_CLEANER("ULTRASONIC_CLEANER", "超声清洗机"),
    AIR_COMPRESSOR("AIR_COMPRESSOR", "空气压缩机"),
    DRYER("DRYER", "烘干设备"),
    SEALING_MACHINE("SEALING_MACHINE", "封口机");
    
    private final String code;
    private final String name;
    
    DeviceTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/enums/
git commit -m "feat: 添加设备类型枚举"
```

---

## Task 4: 加工中心Service层

**目标:** 实现加工中心的业务逻辑(CRUD操作)

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/service/IProcessingCenterService.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/service/impl/ProcessingCenterServiceImpl.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/dto/ProcessingCenterPageDTO.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/dto/CreateProcessingCenterDTO.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/dto/UpdateProcessingCenterDTO.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/vo/ProcessingCenterVO.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/convert/ProcessingCenterConvert.java`

- [ ] **Step 1: 创建DTO类**

ProcessingCenterPageDTO.java:
```java
package com.yigongbao.module.basic.processingCenter.dto;

import com.yigongbao.common.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessingCenterPageDTO extends PageDTO {
    private String centerName;
    private Integer status;
}
```

CreateProcessingCenterDTO.java:
```java
package com.yigongbao.module.basic.processingCenter.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class CreateProcessingCenterDTO {
    @NotBlank(message = "中心编码不能为空")
    private String centerCode;
    
    @NotBlank(message = "中心名称不能为空")
    private String centerName;
    
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String deviceIdRanges;
    private String remark;
}
```

UpdateProcessingCenterDTO.java:
```java
package com.yigongbao.module.basic.processingCenter.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class UpdateProcessingCenterDTO {
    @NotNull(message = "ID不能为空")
    private Long id;
    
    private String centerName;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String deviceIdRanges;
    private Integer status;
    private String remark;
}
```

- [ ] **Step 2: 创建VO类**

```java
package com.yigongbao.module.basic.processingCenter.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProcessingCenterVO {
    private Long id;
    private String centerCode;
    private String centerName;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String deviceIdRanges;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 3: 创建Convert转换器**

```java
package com.yigongbao.module.basic.processingCenter.convert;

import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import org.springframework.beans.BeanUtils;

public class ProcessingCenterConvert {
    
    public static ProcessingCenterEntity toEntity(CreateProcessingCenterDTO dto) {
        ProcessingCenterEntity entity = new ProcessingCenterEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
    
    public static ProcessingCenterVO toVO(ProcessingCenterEntity entity) {
        ProcessingCenterVO vo = new ProcessingCenterVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
```

- [ ] **Step 4: 创建Service接口**

```java
package com.yigongbao.module.basic.processingCenter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.dto.ProcessingCenterPageDTO;
import com.yigongbao.module.basic.processingCenter.dto.UpdateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import java.util.List;

public interface IProcessingCenterService extends IService<ProcessingCenterEntity> {
    IPage<ProcessingCenterVO> listProcessingCenters(ProcessingCenterPageDTO dto);
    ProcessingCenterVO getProcessingCenterById(Long id);
    Long createProcessingCenter(CreateProcessingCenterDTO dto);
    void updateProcessingCenter(UpdateProcessingCenterDTO dto);
    void deleteProcessingCenter(Long id);
    List<ProcessingCenterVO> listAllEnabled();
}
```

- [ ] **Step 5: 创建Service实现类**

```java
package com.yigongbao.module.basic.processingCenter.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.exception.ErrorCodeEnum;
import com.yigongbao.module.basic.processingCenter.convert.ProcessingCenterConvert;
import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.dto.ProcessingCenterPageDTO;
import com.yigongbao.module.basic.processingCenter.dto.UpdateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import com.yigongbao.module.basic.processingCenter.service.IProcessingCenterService;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingCenterServiceImpl extends ServiceImpl<ProcessingCenterMapper, ProcessingCenterEntity> 
        implements IProcessingCenterService {

    @Override
    public IPage<ProcessingCenterVO> listProcessingCenters(ProcessingCenterPageDTO dto) {
        LambdaQueryWrapper<ProcessingCenterEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getCenterName()), ProcessingCenterEntity::getCenterName, dto.getCenterName())
               .eq(dto.getStatus() != null, ProcessingCenterEntity::getStatus, dto.getStatus())
               .orderByDesc(ProcessingCenterEntity::getCreateTime);
        
        IPage<ProcessingCenterEntity> page = page(new Page<>(dto.getCurrent(), dto.getSize()), wrapper);
        return page.convert(ProcessingCenterConvert::toVO);
    }

    @Override
    public ProcessingCenterVO getProcessingCenterById(Long id) {
        ProcessingCenterEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        return ProcessingCenterConvert.toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProcessingCenter(CreateProcessingCenterDTO dto) {
        // 检查编码是否重复
        LambdaQueryWrapper<ProcessingCenterEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessingCenterEntity::getCenterCode, dto.getCenterCode());
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCodeEnum.DATA_ALREADY_EXISTS, "中心编码已存在");
        }
        
        ProcessingCenterEntity entity = ProcessingCenterConvert.toEntity(dto);
        entity.setStatus(StatusConstants.STATUS_ENABLED);
        save(entity);
        
        log.info("创建加工中心: id={}, centerCode={}, centerName={}", 
            entity.getId(), entity.getCenterCode(), entity.getCenterName());
        
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessingCenter(UpdateProcessingCenterDTO dto) {
        ProcessingCenterEntity entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        
        if (StrUtil.isNotBlank(dto.getCenterName())) {
            entity.setCenterName(dto.getCenterName());
        }
        if (StrUtil.isNotBlank(dto.getContactPerson())) {
            entity.setContactPerson(dto.getContactPerson());
        }
        if (StrUtil.isNotBlank(dto.getContactPhone())) {
            entity.setContactPhone(dto.getContactPhone());
        }
        if (StrUtil.isNotBlank(dto.getAddress())) {
            entity.setAddress(dto.getAddress());
        }
        if (StrUtil.isNotBlank(dto.getDeviceIdRanges())) {
            entity.setDeviceIdRanges(dto.getDeviceIdRanges());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (StrUtil.isNotBlank(dto.getRemark())) {
            entity.setRemark(dto.getRemark());
        }
        
        updateById(entity);
        log.info("更新加工中心: id={}, centerCode={}", entity.getId(), entity.getCenterCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessingCenter(Long id) {
        ProcessingCenterEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        
        removeById(id);
        log.info("删除加工中心: id={}, centerCode={}", id, entity.getCenterCode());
    }

    @Override
    public List<ProcessingCenterVO> listAllEnabled() {
        LambdaQueryWrapper<ProcessingCenterEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessingCenterEntity::getStatus, StatusConstants.STATUS_ENABLED)
               .orderByAsc(ProcessingCenterEntity::getCenterCode);
        
        return list(wrapper).stream()
                .map(ProcessingCenterConvert::toVO)
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/
git commit -m "feat: 实现加工中心Service层"
```

---

## Task 5: 加工中心Controller层

**目标:** 实现加工中心的REST API接口

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/controller/ProcessingCenterController.java`

- [ ] **Step 1: 创建Controller**

```java
package com.yigongbao.module.basic.processingCenter.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.dto.ProcessingCenterPageDTO;
import com.yigongbao.module.basic.processingCenter.dto.UpdateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.service.IProcessingCenterService;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/basic/processing-center")
@RequiredArgsConstructor
public class ProcessingCenterController {

    private final IProcessingCenterService processingCenterService;

    @PostMapping("/list")
    public Result<IPage<ProcessingCenterVO>> list(@RequestBody ProcessingCenterPageDTO dto) {
        return Result.success(processingCenterService.listProcessingCenters(dto));
    }

    @GetMapping("/{id}")
    public Result<ProcessingCenterVO> getById(@PathVariable Long id) {
        return Result.success(processingCenterService.getProcessingCenterById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateProcessingCenterDTO dto) {
        return Result.success(processingCenterService.createProcessingCenter(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateProcessingCenterDTO dto) {
        dto.setId(id);
        processingCenterService.updateProcessingCenter(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        processingCenterService.deleteProcessingCenter(id);
        return Result.success();
    }

    @GetMapping("/all")
    public Result<List<ProcessingCenterVO>> listAll() {
        return Result.success(processingCenterService.listAllEnabled());
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd yigongbao-parent
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/processingCenter/controller/
git commit -m "feat: 实现加工中心Controller层"
```

---

## Task 6: 加工中心单元测试

**目标:** 为ProcessingCenterService编写单元测试

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/processingCenter/service/ProcessingCenterServiceImplTest.java`

- [ ] **Step 1: 创建测试类**

```java
package com.yigongbao.module.basic.processingCenter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.dto.ProcessingCenterPageDTO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import com.yigongbao.module.basic.processingCenter.service.impl.ProcessingCenterServiceImpl;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessingCenterServiceImplTest {

    @Mock
    private ProcessingCenterMapper processingCenterMapper;

    @InjectMocks
    private ProcessingCenterServiceImpl processingCenterService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(processingCenterService, processingCenterMapper);
    }

    @Test
    void testCreateProcessingCenter_Success() {
        CreateProcessingCenterDTO dto = new CreateProcessingCenterDTO();
        dto.setCenterCode("WH001");
        dto.setCenterName("武汉嘉一");

        when(processingCenterMapper.selectCount(any())).thenReturn(0L);
        when(processingCenterMapper.insert(any())).thenReturn(1);

        Long id = processingCenterService.createProcessingCenter(dto);

        assertNotNull(id);
        verify(processingCenterMapper, times(1)).insert(any());
    }

    @Test
    void testCreateProcessingCenter_DuplicateCode() {
        CreateProcessingCenterDTO dto = new CreateProcessingCenterDTO();
        dto.setCenterCode("WH001");
        dto.setCenterName("武汉嘉一");

        when(processingCenterMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> {
            processingCenterService.createProcessingCenter(dto);
        });
    }

    @Test
    void testGetProcessingCenterById_Success() {
        ProcessingCenterEntity entity = new ProcessingCenterEntity();
        entity.setId(1L);
        entity.setCenterCode("WH001");
        entity.setCenterName("武汉嘉一");

        when(processingCenterMapper.selectById(1L)).thenReturn(entity);

        ProcessingCenterVO vo = processingCenterService.getProcessingCenterById(1L);

        assertNotNull(vo);
        assertEquals("WH001", vo.getCenterCode());
        assertEquals("武汉嘉一", vo.getCenterName());
    }

    @Test
    void testGetProcessingCenterById_NotFound() {
        when(processingCenterMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            processingCenterService.getProcessingCenterById(999L);
        });
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
cd yigongbao-parent
mvn test -Dtest=ProcessingCenterServiceImplTest
```

Expected: Tests run: 4, Failures: 0, Errors: 0

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-basic/src/test/
git commit -m "test: 添加加工中心Service单元测试"
```

---

## Task 7: 设备Service层

**目标:** 实现设备管理的业务逻辑,包括CRUD和批量更新状态

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/service/IDeviceService.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/service/impl/DeviceServiceImpl.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/service/IDeviceStateLogService.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/service/impl/DeviceStateLogServiceImpl.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/dto/*.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/vo/*.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/convert/*.java`

- [ ] **Step 1: 创建DTO和VO类**

DevicePageDTO.java, CreateDeviceDTO.java, UpdateDeviceStateDTO.java, DeviceVO.java, DeviceStateLogVO.java (参考ProcessingCenter的结构,添加设备特有字段如deviceType, state, connectionStatus等)

- [ ] **Step 2: 创建Convert转换器**

DeviceConvert.java, DeviceStateLogConvert.java

- [ ] **Step 3: 创建IDeviceService接口**

```java
package com.yigongbao.module.basic.device.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DevicePageDTO;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.vo.DeviceVO;
import java.util.List;

public interface IDeviceService extends IService<DeviceEntity> {
    IPage<DeviceVO> listDevices(DevicePageDTO dto);
    DeviceVO getDeviceById(Long id);
    Long createDevice(CreateDeviceDTO dto);
    void updateDeviceState(Long id, Integer state);
    List<DeviceVO> listIdleDevices(Long centerId, String deviceType);
    void batchUpdateDeviceStatus(DeviceStatusPushDTO dto);
    void markDevicesOffline(Long centerId);
    void detectOfflineDevices();
}
```

- [ ] **Step 4: 创建DeviceServiceImpl实现类**

```java
package com.yigongbao.module.basic.device.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.exception.ErrorCodeEnum;
import com.yigongbao.module.basic.device.convert.DeviceConvert;
import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DevicePageDTO;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.entity.DeviceStateLogEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.basic.device.service.IDeviceService;
import com.yigongbao.module.basic.device.service.IDeviceStateLogService;
import com.yigongbao.module.basic.device.vo.DeviceVO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, DeviceEntity> implements IDeviceService {

    private final ProcessingCenterMapper processingCenterMapper;
    private final IDeviceStateLogService deviceStateLogService;

    @Override
    public IPage<DeviceVO> listDevices(DevicePageDTO dto) {
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getCenterId() != null, DeviceEntity::getCenterId, dto.getCenterId())
               .eq(StrUtil.isNotBlank(dto.getDeviceType()), DeviceEntity::getDeviceType, dto.getDeviceType())
               .eq(dto.getState() != null, DeviceEntity::getState, dto.getState())
               .eq(dto.getConnectionStatus() != null, DeviceEntity::getConnectionStatus, dto.getConnectionStatus())
               .like(StrUtil.isNotBlank(dto.getDeviceId()), DeviceEntity::getDeviceId, dto.getDeviceId())
               .orderByDesc(DeviceEntity::getUpdateTime);
        
        IPage<DeviceEntity> page = page(new Page<>(dto.getCurrent(), dto.getSize()), wrapper);
        return page.convert(DeviceConvert::toVO);
    }

    @Override
    public DeviceVO getDeviceById(Long id) {
        DeviceEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        return DeviceConvert.toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDevice(CreateDeviceDTO dto) {
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceEntity::getDeviceId, dto.getDeviceId());
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCodeEnum.DATA_ALREADY_EXISTS, "设备编号已存在");
        }
        
        DeviceEntity entity = DeviceConvert.toEntity(dto);
        save(entity);
        
        log.info("创建设备: id={}, deviceId={}, deviceType={}", 
            entity.getId(), entity.getDeviceId(), entity.getDeviceType());
        
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeviceState(Long id, Integer state) {
        DeviceEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        
        Integer oldState = entity.getState();
        entity.setState(state);
        updateById(entity);
        
        // 记录状态变更日志
        if (!oldState.equals(state)) {
            DeviceStateLogEntity log = new DeviceStateLogEntity();
            log.setDeviceId(entity.getDeviceId());
            log.setOldState(oldState);
            log.setNewState(state);
            log.setChangeTime(LocalDateTime.now());
            log.setChangeType("manual");
            deviceStateLogService.save(log);
        }
        
        log.info("更新设备状态: deviceId={}, {} -> {}", entity.getDeviceId(), oldState, state);
    }

    @Override
    public List<DeviceVO> listIdleDevices(Long centerId, String deviceType) {
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(centerId != null, DeviceEntity::getCenterId, centerId)
               .eq(StrUtil.isNotBlank(deviceType), DeviceEntity::getDeviceType, deviceType)
               .eq(DeviceEntity::getState, 0)
               .eq(DeviceEntity::getConnectionStatus, 1)
               .orderByAsc(DeviceEntity::getDeviceId);
        
        return list(wrapper).stream()
                .map(DeviceConvert::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateDeviceStatus(DeviceStatusPushDTO dto) {
        // 根据center_name查询加工中心
        LambdaQueryWrapper<ProcessingCenterEntity> centerWrapper = new LambdaQueryWrapper<>();
        centerWrapper.eq(ProcessingCenterEntity::getCenterName, dto.getCenterName())
                     .eq(ProcessingCenterEntity::getStatus, StatusConstants.STATUS_ENABLED);
        ProcessingCenterEntity center = processingCenterMapper.selectOne(centerWrapper);
        
        if (center == null) {
            log.warn("加工中心不存在或已禁用: centerName={}", dto.getCenterName());
            return;
        }
        
        // 遍历设备列表
        for (DeviceStatusPushDTO.DeviceStatus deviceStatus : dto.getDevices()) {
            LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceEntity::getDeviceId, deviceStatus.getId());
            DeviceEntity device = getOne(wrapper);
            
            if (device == null) {
                // 自动创建设备
                device = new DeviceEntity();
                device.setDeviceId(deviceStatus.getId());
                device.setDeviceName(deviceStatus.getId());
                device.setDeviceType(DeviceTypeEnum.PRINTER_SLA.getCode());
                device.setCenterId(center.getId());
                device.setCenterName(center.getCenterName());
                device.setState(deviceStatus.getState());
                device.setConnectionStatus(1);
                device.setLastHeartbeat(LocalDateTime.now());
                save(device);
                
                log.info("自动创建设备: deviceId={}, centerId={}", device.getDeviceId(), center.getId());
            } else {
                // 更新设备状态
                Integer oldState = device.getState();
                device.setState(deviceStatus.getState());
                device.setConnectionStatus(1);
                device.setLastHeartbeat(LocalDateTime.now());
                updateById(device);
                
                // 记录状态变更
                if (!oldState.equals(deviceStatus.getState())) {
                    DeviceStateLogEntity stateLog = new DeviceStateLogEntity();
                    stateLog.setDeviceId(device.getDeviceId());
                    stateLog.setOldState(oldState);
                    stateLog.setNewState(deviceStatus.getState());
                    stateLog.setChangeTime(LocalDateTime.now());
                    stateLog.setChangeType("auto");
                    deviceStateLogService.save(stateLog);
                }
            }
        }
        
        log.info("批量更新设备状态: centerName={}, deviceCount={}", dto.getCenterName(), dto.getDevices().size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDevicesOffline(Long centerId) {
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceEntity::getCenterId, centerId)
               .eq(DeviceEntity::getConnectionStatus, 1);
        
        List<DeviceEntity> devices = list(wrapper);
        for (DeviceEntity device : devices) {
            device.setConnectionStatus(0);
            updateById(device);
        }
        
        log.info("标记加工中心设备离线: centerId={}, deviceCount={}", centerId, devices.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void detectOfflineDevices() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceEntity::getConnectionStatus, 1)
               .lt(DeviceEntity::getLastHeartbeat, threshold);
        
        List<DeviceEntity> devices = list(wrapper);
        for (DeviceEntity device : devices) {
            device.setConnectionStatus(0);
            updateById(device);
            log.warn("设备离线: deviceId={}, lastHeartbeat={}", device.getDeviceId(), device.getLastHeartbeat());
        }
        
        if (!devices.isEmpty()) {
            log.info("离线检测完成: 检测到{}个离线设备", devices.size());
        }
    }
}
```

- [ ] **Step 5: 创建DeviceStateLogService**

```java
package com.yigongbao.module.basic.device.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.basic.device.entity.DeviceStateLogEntity;
import com.yigongbao.module.basic.device.mapper.DeviceStateLogMapper;
import com.yigongbao.module.basic.device.service.IDeviceStateLogService;
import org.springframework.stereotype.Service;

@Service
public class DeviceStateLogServiceImpl extends ServiceImpl<DeviceStateLogMapper, DeviceStateLogEntity> 
        implements IDeviceStateLogService {
}
```

- [ ] **Step 6: Commit**

```bash
git add yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/
git commit -m "feat: 实现设备Service层"
```

---

## Task 8: 设备Controller层

**目标:** 实现设备管理的REST API接口

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/controller/DeviceController.java`

- [ ] **Step 1: 创建DeviceController**

```java
package com.yigongbao.module.basic.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DevicePageDTO;
import com.yigongbao.module.basic.device.service.IDeviceService;
import com.yigongbao.module.basic.device.vo.DeviceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/basic/device")
@RequiredArgsConstructor
public class DeviceController {

    private final IDeviceService deviceService;

    @PostMapping("/list")
    public Result<IPage<DeviceVO>> list(@RequestBody DevicePageDTO dto) {
        return Result.success(deviceService.listDevices(dto));
    }

    @GetMapping("/{id}")
    public Result<DeviceVO> getById(@PathVariable Long id) {
        return Result.success(deviceService.getDeviceById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateDeviceDTO dto) {
        return Result.success(deviceService.createDevice(dto));
    }

    @PutMapping("/{id}/state")
    public Result<Void> updateState(@PathVariable Long id, @RequestParam Integer state) {
        deviceService.updateDeviceState(id, state);
        return Result.success();
    }

    @GetMapping("/idle")
    public Result<List<DeviceVO>> listIdle(@RequestParam(required = false) Long centerId,
                                            @RequestParam(required = false) String deviceType) {
        return Result.success(deviceService.listIdleDevices(centerId, deviceType));
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd yigongbao-parent
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/controller/
git commit -m "feat: 实现设备Controller层"
```

---

## Task 9: 设备单元测试

**目标:** 为DeviceService编写单元测试

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/service/DeviceServiceImplTest.java`

- [ ] **Step 1: 创建测试类**

```java
package com.yigongbao.module.basic.device.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.basic.device.service.impl.DeviceServiceImpl;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceServiceImplTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private ProcessingCenterMapper processingCenterMapper;

    @Mock
    private IDeviceStateLogService deviceStateLogService;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(deviceService, deviceMapper);
    }

    @Test
    void testCreateDevice_Success() {
        CreateDeviceDTO dto = new CreateDeviceDTO();
        dto.setDeviceId("SLA-001");
        dto.setDeviceName("打印机001");

        when(deviceMapper.selectCount(any())).thenReturn(0L);
        when(deviceMapper.insert(any())).thenReturn(1);

        Long id = deviceService.createDevice(dto);

        assertNotNull(id);
        verify(deviceMapper, times(1)).insert(any());
    }

    @Test
    void testCreateDevice_DuplicateDeviceId() {
        CreateDeviceDTO dto = new CreateDeviceDTO();
        dto.setDeviceId("SLA-001");

        when(deviceMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> {
            deviceService.createDevice(dto);
        });
    }

    @Test
    void testBatchUpdateDeviceStatus_AutoCreate() {
        DeviceStatusPushDTO dto = new DeviceStatusPushDTO();
        dto.setCenterName("武汉嘉一");
        
        DeviceStatusPushDTO.DeviceStatus deviceStatus = new DeviceStatusPushDTO.DeviceStatus();
        deviceStatus.setId("SLA-001");
        deviceStatus.setState(1);
        dto.setDevices(Arrays.asList(deviceStatus));

        ProcessingCenterEntity center = new ProcessingCenterEntity();
        center.setId(1L);
        center.setCenterName("武汉嘉一");

        when(processingCenterMapper.selectOne(any())).thenReturn(center);
        when(deviceMapper.selectOne(any())).thenReturn(null);
        when(deviceMapper.insert(any())).thenReturn(1);

        deviceService.batchUpdateDeviceStatus(dto);

        verify(deviceMapper, times(1)).insert(any());
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
cd yigongbao-parent
mvn test -Dtest=DeviceServiceImplTest
```

Expected: Tests run: 3, Failures: 0, Errors: 0

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-basic/src/test/
git commit -m "test: 添加设备Service单元测试"
```

---

## Task 10: WebSocket配置和连接管理器

**目标:** 创建WebSocket配置类和连接管理器

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/websocket/DeviceWebSocketConfig.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/websocket/DeviceConnectionManager.java`

- [ ] **Step 1: 创建DeviceWebSocketConfig**

```java
package com.yigongbao.module.basic.device.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class DeviceWebSocketConfig implements WebSocketConfigurer {
    
    private final DeviceWebSocketHandler deviceWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(deviceWebSocketHandler, "/api/basic/ws/device")
                .setAllowedOrigins("*");
    }
}
```

- [ ] **Step 2: 创建DeviceConnectionManager**

```java
package com.yigongbao.module.basic.device.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceConnectionManager {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> heartbeats = new ConcurrentHashMap<>();
    
    public void addSession(String centerName, WebSocketSession session) {
        sessions.put(centerName, session);
        updateHeartbeat(centerName);
    }
    
    public void removeSession(String centerName) {
        sessions.remove(centerName);
        heartbeats.remove(centerName);
    }
    
    public void updateHeartbeat(String centerName) {
        heartbeats.put(centerName, LocalDateTime.now());
    }
    
    public WebSocketSession getSession(String centerName) {
        return sessions.get(centerName);
    }
    
    public LocalDateTime getLastHeartbeat(String centerName) {
        return heartbeats.get(centerName);
    }
    
    public Map<String, WebSocketSession> getAllSessions() {
        return new ConcurrentHashMap<>(sessions);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/websocket/
git commit -m "feat: 添加WebSocket配置和连接管理器"
```

---

## Task 11: WebSocket处理器和批量更新

**目标:** 实现WebSocket消息处理器,处理设备状态推送

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/websocket/DeviceWebSocketHandler.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/dto/DeviceStatusPushDTO.java`

- [ ] **Step 1: 创建DeviceStatusPushDTO**

```java
package com.yigongbao.module.basic.device.dto;

import lombok.Data;
import java.util.List;

@Data
public class DeviceStatusPushDTO {
    private String centerName;
    private List<DeviceStatus> devices;
    
    @Data
    public static class DeviceStatus {
        private String id;
        private Integer state;
    }
}
```

- [ ] **Step 2: 创建DeviceWebSocketHandler**

```java
package com.yigongbao.module.basic.device.websocket;

import cn.hutool.json.JSONUtil;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.service.IDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceWebSocketHandler extends TextWebSocketHandler {
    
    private final IDeviceService deviceService;
    private final DeviceConnectionManager connectionManager;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket连接建立: sessionId={}", session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            DeviceStatusPushDTO dto = JSONUtil.toBean(payload, DeviceStatusPushDTO.class);
            
            connectionManager.addSession(dto.getCenterName(), session);
            deviceService.batchUpdateDeviceStatus(dto);
            
            session.sendMessage(new TextMessage("{\"code\":200,\"message\":\"success\"}"));
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: sessionId={}", session.getId(), e);
            try {
                session.sendMessage(new TextMessage("{\"code\":500,\"message\":\"error\"}"));
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket连接关闭: sessionId={}, status={}", session.getId(), status);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/
git commit -m "feat: 实现WebSocket处理器和批量更新"
```

---

## Task 12: WebSocket集成测试

**目标:** 测试WebSocket连接和消息处理

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/websocket/DeviceWebSocketHandlerTest.java`

- [ ] **Step 1: 创建WebSocket测试类**

```java
package com.yigongbao.module.basic.device.websocket;

import com.yigongbao.module.basic.device.service.IDeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceWebSocketHandlerTest {

    @Mock
    private IDeviceService deviceService;

    @Mock
    private DeviceConnectionManager connectionManager;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private DeviceWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        when(session.getId()).thenReturn("test-session-id");
    }

    @Test
    void testHandleTextMessage_Success() throws Exception {
        String payload = "{\"center_name\":\"武汉嘉一\",\"devices\":[{\"id\":\"SLA-001\",\"state\":1}]}";
        TextMessage message = new TextMessage(payload);

        doNothing().when(deviceService).batchUpdateDeviceStatus(any());
        doNothing().when(connectionManager).addSession(anyString(), any());
        when(session.isOpen()).thenReturn(true);

        handler.handleTextMessage(session, message);

        verify(deviceService, times(1)).batchUpdateDeviceStatus(any());
        verify(connectionManager, times(1)).addSession(anyString(), any());
        verify(session, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testAfterConnectionEstablished() {
        handler.afterConnectionEstablished(session);
        verify(session, times(1)).getId();
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
cd yigongbao-parent
mvn test -Dtest=DeviceWebSocketHandlerTest
```

Expected: Tests run: 2, Failures: 0, Errors: 0

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-basic/src/test/
git commit -m "test: 添加WebSocket集成测试"
```

---

## Task 13: 定时任务和离线检测

**目标:** 实现设备离线检测定时任务

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/task/DeviceOfflineDetectionTask.java`

- [ ] **Step 1: 创建定时任务类**

```java
package com.yigongbao.module.basic.device.task;

import com.yigongbao.module.basic.device.service.IDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceOfflineDetectionTask {
    
    private final IDeviceService deviceService;
    
    @Scheduled(cron = "0 * * * * ?")
    public void detectOfflineDevices() {
        log.debug("开始执行设备离线检测任务");
        try {
            deviceService.detectOfflineDevices();
        } catch (Exception e) {
            log.error("设备离线检测任务执行失败", e);
        }
    }
}
```

- [ ] **Step 2: 启用定时任务**

修改 `yigongbao-boot` 的启动类，添加 `@EnableScheduling` 注解：

```java
@SpringBootApplication
@EnableScheduling
public class YigongbaoBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(YigongbaoBootApplication.class, args);
    }
}
```

- [ ] **Step 3: 验证编译**

```bash
cd yigongbao-parent
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/task/
git add yigongbao-boot/src/main/java/com/yigongbao/boot/YigongbaoBootApplication.java
git commit -m "feat: 添加设备离线检测定时任务"
```

---

## Task 14: 端到端集成测试

**目标:** 验证完整功能流程

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/integration/ProcessingCenterDeviceIntegrationTest.java`

- [ ] **Step 1: 创建集成测试类**

```java
package com.yigongbao.module.basic.integration;

import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.service.IDeviceService;
import com.yigongbao.module.basic.device.vo.DeviceVO;
import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.service.IProcessingCenterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProcessingCenterDeviceIntegrationTest {

    @Autowired
    private IProcessingCenterService processingCenterService;

    @Autowired
    private IDeviceService deviceService;

    @Test
    void testCompleteWorkflow() {
        // 1. 创建加工中心
        CreateProcessingCenterDTO centerDTO = new CreateProcessingCenterDTO();
        centerDTO.setCenterCode("WH001");
        centerDTO.setCenterName("武汉嘉一");
        Long centerId = processingCenterService.createProcessingCenter(centerDTO);
        assertNotNull(centerId);

        // 2. 手动创建设备
        CreateDeviceDTO deviceDTO = new CreateDeviceDTO();
        deviceDTO.setDeviceId("SLA-001");
        deviceDTO.setDeviceName("打印机001");
        deviceDTO.setDeviceType("PRINTER_SLA");
        deviceDTO.setCenterId(centerId);
        Long deviceId = deviceService.createDevice(deviceDTO);
        assertNotNull(deviceId);

        // 3. 模拟WebSocket推送更新设备状态
        DeviceStatusPushDTO pushDTO = new DeviceStatusPushDTO();
        pushDTO.setCenterName("武汉嘉一");
        
        DeviceStatusPushDTO.DeviceStatus status = new DeviceStatusPushDTO.DeviceStatus();
        status.setId("SLA-001");
        status.setState(1);
        pushDTO.setDevices(Arrays.asList(status));
        
        deviceService.batchUpdateDeviceStatus(pushDTO);

        // 4. 验证设备状态已更新
        DeviceVO device = deviceService.getDeviceById(deviceId);
        assertEquals(1, device.getState());
        assertEquals(1, device.getConnectionStatus());
        assertNotNull(device.getLastHeartbeat());

        // 5. 查询空闲设备（状态为占用，应该查不到）
        List<DeviceVO> idleDevices = deviceService.listIdleDevices(centerId, "PRINTER_SLA");
        assertTrue(idleDevices.isEmpty());
    }
}
```

- [ ] **Step 2: 运行集成测试**

```bash
cd yigongbao-parent
mvn test -Dtest=ProcessingCenterDeviceIntegrationTest
```

Expected: Tests run: 1, Failures: 0, Errors: 0

- [ ] **Step 3: 运行所有测试**

```bash
cd yigongbao-parent
mvn test
```

Expected: All tests pass

- [ ] **Step 4: 最终提交**

```bash
git add yigongbao-module-basic/src/test/
git commit -m "test: 添加端到端集成测试"
```

---

**实施计划完成**

所有14个任务已定义完成，涵盖：
- Phase 1: 数据库与基础框架 (Tasks 1-3)
- Phase 2: 加工中心管理 (Tasks 4-6)
- Phase 3: 设备管理基础 (Tasks 7-9)
- Phase 4: WebSocket功能 (Tasks 10-12)
- Phase 5: 设备离线检测 (Task 13)
- Phase 6: 集成测试 (Task 14)

下一步：按照计划逐步实施各个任务。

