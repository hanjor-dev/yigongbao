# 基础模块安全审计报告

**审计日期**: 2026-05-23  
**审计范围**: yigongbao-module-basic 模块全部功能代码  
**审计重点**: 水平越权、垂直越权、数据权限泄露、文件访问控制  
**审计人**: Kiro AI Agent

---

## 执行摘要

本次安全审计对基础模块（yigongbao-module-basic）进行了全面的安全评估，重点审查了文件访问控制、操作日志权限、主数据管理权限等安全问题。审计发现：

- **严重漏洞**: 2个
- **高风险问题**: 1个
- **中等风险问题**: 1个
- **低风险问题**: 1个

**关键发现**：
1. 文件下载接口缺少所有权校验，存在越权下载风险
2. 文件删除接口缺少所有权校验，存在越权删除风险
3. 操作日志查询缺少用户范围过滤，可能泄露其他用户操作记录
4. 主数据管理接口部分缺少权限控制注解
5. 编码生成接口缺少速率限制

**风险评级**: 🔴 **高风险** - 建议立即修复严重漏洞

---

## 一、严重漏洞（Critical）

### 1.1 文件下载接口缺少所有权校验 🔴

**漏洞位置**: `FileServiceImpl.download()`  
**文件**: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/service/impl/FileServiceImpl.java`

**漏洞描述**:
文件下载接口只检查文件是否存在，但没有校验当前用户是否有权下载该文件。这是一个严重的**水平越权漏洞**。

**问题代码**:
```java
@Override
public void download(Long fileId, HttpServletResponse response) {
    // 1. 查询文件记录
    FileEntity file = getById(fileId);
    if (file == null) {
        throw new BusinessException(ErrorCodeEnum.FILE_NOT_FOUND);
    }
    // ❌ 缺少所有权校验！
    
    // 2. 下载文件
    try {
        FileInfo fileInfo = fileStorageService.getFileInfoByUrl(file.getUrl());
        // ... 下载逻辑
    } catch (Exception e) {
        log.error("文件下载失败: fileId={}", fileId, e);
        throw new BusinessException(ErrorCodeEnum.FILE_DOWNLOAD_ERROR);
    }
}
```

**攻击场景**:
1. 用户A上传了私密文件（如医院资质证明），fileId=100
2. 用户B通过接口 `GET /basic/file/100/download` 可以下载用户A的文件
3. 系统未校验用户B是否有权访问文件100，导致越权下载
4. 可能泄露敏感业务数据、医院资质、产品图片等

**影响范围**:
- 任何知道文件ID的用户都可以下载不属于自己的文件
- 可能导致敏感数据泄露、商业机密泄露
- 违反数据隐私保护要求

**修复建议**:
```java
@Override
public void download(Long fileId, HttpServletResponse response) {
    // 1. 查询文件记录
    FileEntity file = getById(fileId);
    if (file == null) {
        throw new BusinessException(ErrorCodeEnum.FILE_NOT_FOUND);
    }
    
    // ✅ 添加所有权校验
    Long currentUserId = StpUtil.getLoginIdAsLong();
    if (!currentUserId.equals(file.getCreateBy())) {
        // 检查是否是公共文件或有特殊权限
        if (!isPublicFile(file) && !hasFileAccessPermission(currentUserId, fileId)) {
            log.warn("文件访问权限不足: fileId={}, userId={}", fileId, currentUserId);
            throw new BusinessException(ErrorCodeEnum.FILE_ACCESS_DENIED);
        }
    }
    
    // 2. 下载文件
    try {
        FileInfo fileInfo = fileStorageService.getFileInfoByUrl(file.getUrl());
        // ... 下载逻辑
    } catch (Exception e) {
        log.error("文件下载失败: fileId={}", fileId, e);
        throw new BusinessException(ErrorCodeEnum.FILE_DOWNLOAD_ERROR);
    }
}
```

**优先级**: 🔴 **P0 - 立即修复**

---

### 1.2 文件删除接口缺少所有权校验 🔴

**漏洞位置**: `FileServiceImpl.delete()`  
**文件**: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/service/impl/FileServiceImpl.java`

**漏洞描述**:
文件删除接口只检查文件是否存在，但没有校验当前用户是否是文件的上传者。这是一个严重的**水平越权漏洞**。

**问题代码**:
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void delete(Long fileId) {
    // 1. 查询文件记录
    FileEntity file = getById(fileId);
    if (file == null) {
        throw new BusinessException(ErrorCodeEnum.FILE_NOT_FOUND);
    }
    // ❌ 缺少所有权校验！
    
    // 2. 删除物理文件
    try {
        fileStorageService.delete(file.getUrl());
    } catch (Exception e) {
        log.error("删除物理文件失败: fileId={}, url={}", fileId, file.getUrl(), e);
    }
    
    // 3. 删除数据库记录
    removeById(fileId);
    log.info("删除文件: fileId={}, fileName={}", fileId, file.getFileName());
}
```

**攻击场景**:
1. 用户A上传了重要文件，fileId=100
2. 用户B通过接口 `DELETE /basic/file/100` 可以删除用户A的文件
3. 系统未校验用户B是否是文件所有者，导致越权删除
4. 可能导致重要业务数据丢失、业务流程中断

**影响范围**:
- 任何知道文件ID的用户都可以删除不属于自己的文件
- 可能导致数据丢失、业务中断
- 严重违反数据安全原则

**修复建议**:
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void delete(Long fileId) {
    // 1. 查询文件记录
    FileEntity file = getById(fileId);
    if (file == null) {
        throw new BusinessException(ErrorCodeEnum.FILE_NOT_FOUND);
    }
    
    // ✅ 添加所有权校验
    Long currentUserId = StpUtil.getLoginIdAsLong();
    if (!currentUserId.equals(file.getCreateBy())) {
        // 检查是否有文件管理权限
        if (!StpUtil.hasPermission("file:DeleteAny")) {
            log.warn("文件删除权限不足: fileId={}, userId={}, owner={}", 
                fileId, currentUserId, file.getCreateBy());
            throw new BusinessException(ErrorCodeEnum.FILE_DELETE_DENIED);
        }
    }
    
    // 2. 删除物理文件
    try {
        fileStorageService.delete(file.getUrl());
    } catch (Exception e) {
        log.error("删除物理文件失败: fileId={}, url={}", fileId, file.getUrl(), e);
    }
    
    // 3. 删除数据库记录
    removeById(fileId);
    log.info("删除文件: fileId={}, fileName={}, deleteBy={}", fileId, file.getFileName(), currentUserId);
}
```

**优先级**: 🔴 **P0 - 立即修复**

---

## 二、高风险问题（High）

### 2.1 操作日志查询缺少用户范围过滤 🟠

**问题位置**: `OperationLogServiceImpl.listLogs()`  
**文件**: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/service/impl/OperationLogServiceImpl.java`

**问题描述**:
操作日志查询接口没有根据当前用户的数据权限范围过滤结果，导致用户可以查询到不属于自己权限范围内的操作日志。

**问题代码**:
```java
@Override
public IPage<OperationLogVO> listLogs(OperationLogPageDTO dto) {
    LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();
    wrapper.like(StrUtil.isNotBlank(dto.getModule()), OperationLogEntity::getModule, dto.getModule())
           .eq(Objects.nonNull(dto.getOperatorId()), OperationLogEntity::getOperatorId, dto.getOperatorId())
           // ❌ 未根据当前用户的数据权限范围过滤
           .orderByDesc(OperationLogEntity::getCreateTime);
    
    IPage<OperationLogEntity> page = page(new Page<>(dto.getCurrent(), dto.getSize()), wrapper);
    return page.convert(OperationLogConvert.INSTANCE::toVO);
}
```

**攻击场景**:
1. 用户A（数据权限范围=DEPT）登录系统
2. 用户A调用 `POST /basic/operation-log/list`
3. 系统返回所有操作日志，包括其他部门、其他机构的用户操作记录
4. 用户A可以看到其他用户的敏感操作（如修改密码、删除数据等）

**影响范围**:
- 用户可以查询到不属于自己权限范围的操作日志
- 可能泄露其他用户的操作行为、业务数据
- 违反数据隐私保护要求

**修复建议**:
```java
@Override
public IPage<OperationLogVO> listLogs(OperationLogPageDTO dto) {
    Long currentUserId = StpUtil.getLoginIdAsLong();
    DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);
    
    LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();
    wrapper.like(StrUtil.isNotBlank(dto.getModule()), OperationLogEntity::getModule, dto.getModule())
           .eq(Objects.nonNull(dto.getOperatorId()), OperationLogEntity::getOperatorId, dto.getOperatorId())
           .orderByDesc(OperationLogEntity::getCreateTime);
    
    // ✅ 添加数据权限过滤
    applyDataScopeFilter(wrapper, currentUserId, scopeType);
    
    IPage<OperationLogEntity> page = page(new Page<>(dto.getCurrent(), dto.getSize()), wrapper);
    return page.convert(OperationLogConvert.INSTANCE::toVO);
}

private void applyDataScopeFilter(LambdaQueryWrapper<OperationLogEntity> wrapper, 
                                   Long currentUserId, 
                                   DataScopeTypeEnum scopeType) {
    switch (scopeType) {
        case SELF:
            wrapper.eq(OperationLogEntity::getOperatorId, currentUserId);
            break;
        case DEPT:
            // 查询同部门用户的操作日志
            List<Long> deptUserIds = userService.getUserIdsByDept(currentUserId);
            wrapper.in(OperationLogEntity::getOperatorId, deptUserIds);
            break;
        case ORG:
            // 查询同机构用户的操作日志
            List<Long> orgUserIds = userService.getUserIdsByOrg(currentUserId);
            wrapper.in(OperationLogEntity::getOperatorId, orgUserIds);
            break;
        case ALL:
            // 查看所有日志，不过滤
            break;
        default:
            wrapper.eq(OperationLogEntity::getOperatorId, currentUserId);
    }
}
```

**优先级**: 🟠 **P1 - 高优先级修复**

---

## 三、中等风险问题（Medium）

### 3.1 主数据管理接口缺少权限控制 🟡

**问题位置**: 
- `BodyPartController` - 身体部位管理接口
- `CodeRuleController` - 编码规则管理接口

**问题描述**:
部分主数据管理接口缺少 `@RequirePermission` 注解，可能导致权限控制不严格。

**问题代码**:
```java
// BodyPartController
@PostMapping("/list")
// ❌ 缺少 @RequirePermission 注解
public Result<IPage<BodyPartVO>> list(@RequestBody BodyPartPageDTO dto) {
    return Result.success(bodyPartService.listBodyParts(dto));
}

@PostMapping
// ❌ 缺少 @RequirePermission 注解
public Result<Void> create(@Validated @RequestBody CreateBodyPartDTO dto) {
    bodyPartService.createBodyPart(dto);
    return Result.success();
}

// CodeRuleController
@PostMapping
// ❌ 缺少 @RequirePermission 注解
public Result<Void> create(@Validated @RequestBody CreateCodeRuleDTO dto) {
    codeRuleService.createCodeRule(dto);
    return Result.success();
}
```

**影响范围**:
- 任何登录用户都可以访问主数据管理接口
- 可能导致数据被非授权用户修改
- 违反最小权限原则

**修复建议**:
```java
// BodyPartController
@PostMapping("/list")
@RequirePermission("bodypart:List")  // ✅ 添加权限控制
public Result<IPage<BodyPartVO>> list(@RequestBody BodyPartPageDTO dto) {
    return Result.success(bodyPartService.listBodyParts(dto));
}

@PostMapping
@RequirePermission("bodypart:Add")  // ✅ 添加权限控制
public Result<Void> create(@Validated @RequestBody CreateBodyPartDTO dto) {
    bodyPartService.createBodyPart(dto);
    return Result.success();
}

// CodeRuleController
@PostMapping
@RequirePermission("coderule:Add")  // ✅ 添加权限控制
public Result<Void> create(@Validated @RequestBody CreateCodeRuleDTO dto) {
    codeRuleService.createCodeRule(dto);
    return Result.success();
}
```

**优先级**: 🟡 **P2 - 中优先级修复**

---

## 四、低风险问题（Low）

### 4.1 编码生成接口缺少速率限制 ✅

**问题位置**: `CodeGeneratorServiceImpl.generate()`

**问题描述**:
编码生成接口没有速率限制，可能被恶意用户滥用，导致编码号段快速消耗。

**建议**:
为编码生成接口添加速率限制（如每用户每分钟最多生成100个编码），防止恶意消耗编码资源。

**修复建议**:
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class CodeGeneratorServiceImpl implements ICodeGeneratorService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public String generate(String ruleCode) {
        // ✅ 添加速率限制
        Long currentUserId = StpUtil.getLoginIdAsLong();
        String rateLimitKey = "code:gen:limit:" + currentUserId;
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        
        if (count == 1) {
            redisTemplate.expire(rateLimitKey, 1, TimeUnit.MINUTES);
        }
        
        if (count > 100) {
            log.warn("编码生成速率超限: userId={}, count={}", currentUserId, count);
            throw new BusinessException(ErrorCodeEnum.RATE_LIMIT_EXCEEDED);
        }
        
        // 生成编码逻辑...
        return generatedCode;
    }
}
```

**优先级**: ⚪ **P3 - 低优先级**

---

## 五、安全建议

### 5.1 立即修复建议（P0）

**必须立即修复的2个严重漏洞**：

1. **FileServiceImpl.download()** - 添加文件所有权校验
2. **FileServiceImpl.delete()** - 添加文件所有权校验

**统一修复模板**：
```java
// 文件访问权限校验方法
private void validateFileAccess(Long fileId, String operation) {
    FileEntity file = getById(fileId);
    if (file == null) {
        throw new BusinessException(ErrorCodeEnum.FILE_NOT_FOUND);
    }
    
    Long currentUserId = StpUtil.getLoginIdAsLong();
    
    // 检查是否是文件所有者
    if (currentUserId.equals(file.getCreateBy())) {
        return;
    }
    
    // 检查是否是公共文件
    if (isPublicFile(file)) {
        return;
    }
    
    // 检查是否有特殊权限
    String permission = "file:" + operation + "Any";
    if (StpUtil.hasPermission(permission)) {
        return;
    }
    
    log.warn("文件{}权限不足: fileId={}, userId={}, owner={}", 
        operation, fileId, currentUserId, file.getCreateBy());
    throw new BusinessException(ErrorCodeEnum.FILE_ACCESS_DENIED);
}
```

### 5.2 文件访问控制优化建议

**问题**: 当前文件访问控制逻辑分散，容易遗漏。

**建议**: 
1. 建立统一的文件访问控制机制
2. 区分公共文件和私有文件
3. 支持文件共享功能（如订单相关文件可被订单参与者访问）

**实现方案**：
```java
@Component
public class FileAccessControl {
    
    /**
     * 检查文件访问权限
     * @param fileId 文件ID
     * @param operation 操作类型（download/delete/update）
     * @return 是否有权限
     */
    public boolean hasFileAccess(Long fileId, String operation) {
        FileEntity file = fileService.getById(fileId);
        if (file == null) {
            return false;
        }
        
        Long currentUserId = StpUtil.getLoginIdAsLong();
        
        // 1. 文件所有者
        if (currentUserId.equals(file.getCreateBy())) {
            return true;
        }
        
        // 2. 公共文件（仅下载）
        if ("download".equals(operation) && isPublicFile(file)) {
            return true;
        }
        
        // 3. 业务关联文件（如订单文件）
        if (hasBusinessAccess(file, currentUserId)) {
            return true;
        }
        
        // 4. 特殊权限
        String permission = "file:" + operation + "Any";
        return StpUtil.hasPermission(permission);
    }
    
    private boolean hasBusinessAccess(FileEntity file, Long userId) {
        // 检查用户是否有权访问文件关联的业务对象
        // 例如：订单文件可被订单创建者、审核者、设计师访问
        if (StrUtil.isNotBlank(file.getBusinessType())) {
            switch (file.getBusinessType()) {
                case "ORDER":
                    return orderService.hasOrderAccess(file.getBusinessId(), userId);
                case "HOSPITAL":
                    return hospitalService.hasHospitalAccess(file.getBusinessId(), userId);
                default:
                    return false;
            }
        }
        return false;
    }
}
```

### 5.3 操作日志权限过滤标准化建议

**建议**: 在 `OperationLogServiceImpl` 中创建统一的数据权限过滤方法，复用到所有查询接口。

```java
@Component
public class OperationLogDataScopeHelper {
    
    public void applyDataScopeFilter(LambdaQueryWrapper<OperationLogEntity> wrapper) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);
        
        switch (scopeType) {
            case SELF:
                wrapper.eq(OperationLogEntity::getOperatorId, currentUserId);
                break;
            case DEPT:
                List<Long> deptUserIds = userService.getUserIdsByDept(currentUserId);
                if (CollUtil.isEmpty(deptUserIds)) {
                    wrapper.apply("1 = 0");
                } else {
                    wrapper.in(OperationLogEntity::getOperatorId, deptUserIds);
                }
                break;
            case ORG:
                List<Long> orgUserIds = userService.getUserIdsByOrg(currentUserId);
                if (CollUtil.isEmpty(orgUserIds)) {
                    wrapper.apply("1 = 0");
                } else {
                    wrapper.in(OperationLogEntity::getOperatorId, orgUserIds);
                }
                break;
            case ALL:
                // 不过滤
                break;
            default:
                wrapper.eq(OperationLogEntity::getOperatorId, currentUserId);
        }
    }
}
```

### 5.4 安全测试建议

**建议进行以下安全测试**：

1. **文件访问控制测试**
   - 用户A上传文件，用户B尝试下载/删除
   - 验证是否正确拒绝越权操作

2. **操作日志权限测试**
   - 创建不同数据权限范围的用户
   - 验证每个用户只能看到权限范围内的日志

3. **主数据管理权限测试**
   - 测试无权限用户是否可以访问主数据管理接口
   - 验证权限控制是否生效

4. **编码生成速率限制测试**
   - 短时间内大量调用编码生成接口
   - 验证速率限制是否生效

---

## 六、总结

### 6.1 漏洞统计

| 风险级别 | 数量 | 占比 |
|---------|------|------|
| 🔴 严重漏洞 | 2 | 40% |
| 🟠 高风险 | 1 | 20% |
| 🟡 中风险 | 1 | 20% |
| ⚪ 低风险 | 1 | 20% |
| **总计** | **5** | **100%** |

### 6.2 核心问题

**文件访问控制缺失是最严重的问题**：
- 文件下载接口缺少所有权校验
- 文件删除接口缺少所有权校验
- 可能导致敏感数据泄露、数据丢失

**操作日志权限过滤缺失**：
- 用户可以查询到不属于自己权限范围的操作日志
- 可能泄露其他用户的操作行为

### 6.3 修复优先级

**第一阶段（P0 - 立即修复）**：
1. FileServiceImpl.download() - 添加文件所有权校验
2. FileServiceImpl.delete() - 添加文件所有权校验

**第二阶段（P1 - 高优先级）**：
1. OperationLogServiceImpl.listLogs() - 添加数据权限过滤

**第三阶段（P2 - 中优先级）**：
1. 为主数据管理接口添加权限控制注解

**第四阶段（P3 - 低优先级）**：
1. 为编码生成接口添加速率限制

### 6.4 长期改进建议

1. **建立统一的文件访问控制机制**，支持公共文件、私有文件、业务关联文件
2. **建立操作日志数据权限过滤标准**，确保所有查询接口都正确过滤
3. **定期进行安全审计**，确保新增接口都正确实施权限校验
4. **完善权限校验文档**，明确每个接口的权限要求

---

**报告结束**

**审计人**: Kiro AI Agent  
**审计日期**: 2026-05-23  
**报告版本**: 1.0
