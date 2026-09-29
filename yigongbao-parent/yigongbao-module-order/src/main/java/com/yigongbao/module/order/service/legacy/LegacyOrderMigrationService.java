package com.yigongbao.module.order.service.legacy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.module.order.dto.legacy.LegacyMigrationTaskPageDTO;
import com.yigongbao.module.order.entity.legacy.LegacyMigrationTaskEntity;
import com.yigongbao.module.order.entity.legacy.LegacyMigrationErrorEntity;
import com.yigongbao.module.order.entity.legacy.LegacyOrderListEntity;
import com.yigongbao.module.order.mapper.legacy.LegacyMigrationErrorMapper;
import com.yigongbao.module.order.mapper.legacy.LegacyMigrationTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegacyOrderMigrationService {
    private static final String SOURCE = "OLD_YIGONGBAO";
    private static final String MAPPING_VERSION = "v5";
    private static final int BATCH_SIZE = 500;
    private static final int STALE_TASK_MINUTES = 30;
    private final LegacyMigrationTaskMapper taskMapper;
    private final LegacyMigrationErrorMapper errorMapper;
    private final LegacyOrderArchiveService archiveService;
    private final ObjectProvider<JdbcTemplate> legacyJdbcTemplate;
    private final LegacyOrderMigrationRunner runner;
    private final LegacyMigrationDistributedLock distributedLock;
    private final LegacySourceSchemaValidator schemaValidator;

    public synchronized Long createIncrementalTask(Long userId) {
        Long taskId;
        try (LegacyMigrationDistributedLock.LockHandle ignored = acquireLock(2)) {
            recoverStaleTasks();
            Long running = taskMapper.selectCount(new LambdaQueryWrapper<LegacyMigrationTaskEntity>()
                    .in(LegacyMigrationTaskEntity::getStatus, "PENDING", "RUNNING"));
            if (running != null && running > 0) {
                LegacyMigrationTaskEntity current = taskMapper.selectOne(new LambdaQueryWrapper<LegacyMigrationTaskEntity>()
                        .in(LegacyMigrationTaskEntity::getStatus, "PENDING", "RUNNING")
                        .orderByDesc(LegacyMigrationTaskEntity::getId).last("LIMIT 1"));
                if (current != null) {
                    log.info("历史订单迁移任务已在执行，复用任务，taskId={}, status={}", current.getId(), current.getStatus());
                    return current.getId();
                }
                return null;
            }
            JdbcTemplate jdbc = legacyJdbcTemplate.getIfAvailable();
            if (jdbc == null) throw new IllegalStateException("旧医工宝数据库未配置或未启用");
            schemaValidator.validate();
            LegacyMigrationTaskEntity task = new LegacyMigrationTaskEntity();
            task.setTaskType("INCREMENTAL");
            task.setStatus("PENDING");
            task.setRequestedBy(userId);
            task.setRequestedAt(LocalDateTime.now());
            task.setMappingVersion(MAPPING_VERSION);
            task.setTotalRead(0);
            task.setInsertedCount(0);
            task.setUpdatedCount(0);
            task.setSkippedCount(0);
            task.setErrorCount(0);
            taskMapper.insert(task);
            taskId = task.getId();
        }
        // 释放创建锁后再提交异步任务，避免异步线程抢不到同一命名锁而被误判为重复任务。
        runner.runAsync(taskId);
        log.info("历史订单增量迁移任务已创建，taskId={}, requestedBy={}, mappingVersion={}", taskId, userId, MAPPING_VERSION);
        return taskId;
    }

    private LegacyMigrationDistributedLock.LockHandle acquireLock(int timeoutSeconds) {
        LegacyMigrationDistributedLock.LockHandle lock = distributedLock.tryAcquire(timeoutSeconds);
        if (lock == null) throw new IllegalStateException("已有迁移任务正在执行，请稍后重试。");
        return lock;
    }

    public LegacyMigrationTaskEntity getTask(Long id) {
        return taskMapper.selectById(id);
    }

    public LegacyMigrationTaskEntity getLatestTask() {
        return taskMapper.selectOne(new LambdaQueryWrapper<LegacyMigrationTaskEntity>()
                .orderByDesc(LegacyMigrationTaskEntity::getId)
                .last("LIMIT 1"));
    }

    public IPage<LegacyMigrationTaskEntity> pageTasks(LegacyMigrationTaskPageDTO dto) {
        Page<LegacyMigrationTaskEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<LegacyMigrationTaskEntity> query = new LambdaQueryWrapper<LegacyMigrationTaskEntity>()
                .eq(LegacyMigrationTaskEntity::getIsDeleted, 0)
                .eq(StringUtils.hasText(dto.getStatus()), LegacyMigrationTaskEntity::getStatus, dto.getStatus())
                .orderByDesc(LegacyMigrationTaskEntity::getId);
        return taskMapper.selectPage(page, query);
    }

    void run(Long taskId) {
        LegacyMigrationTaskEntity task = taskMapper.selectById(taskId);
        JdbcTemplate jdbc = legacyJdbcTemplate.getIfAvailable();
        if (task == null || jdbc == null) return;
        LegacyMigrationDistributedLock.LockHandle lock;
        try {
            lock = acquireLock(0);
        } catch (IllegalStateException ex) {
            task.setStatus("FAILED");
            task.setErrorMessage("已有迁移任务正在执行，请稍后重试。");
            task.setFinishedAt(LocalDateTime.now());
            taskMapper.updateById(task);
            return;
        }
        try (lock) {
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        task.setHeartbeatAt(LocalDateTime.now());
        task.setSourceSnapshotAt(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("历史订单迁移任务开始执行，taskId={}, batchSize={}", taskId, BATCH_SIZE);
        try {
            long lastId = 0L;
            int totalRead = 0;
            int inserted = 0, updated = 0, skipped = 0, errors = 0;
            List<String> warnings = new ArrayList<>();
            while (true) {
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "SELECT * FROM jy_order WHERE od_id > ? ORDER BY od_id LIMIT ?", lastId, BATCH_SIZE);
                if (rows.isEmpty()) break;
                Map<String, PostalInfo> postalMap = loadPostalMap(jdbc, orderCodes(rows), warnings);
                Map<Long, String> moduleMap = loadModuleMap(jdbc, moduleIds(rows), warnings);
                List<LegacyOrderListEntity> items = new ArrayList<>();
                for (Map<String, Object> row : rows) {
                    try {
                        items.add(convert(row, postalMap, moduleMap));
                    } catch (Exception ex) {
                        errors++;
                        saveError(taskId, row, ex);
                    }
                    Object rowId = row.get("od_id");
                    if (rowId != null) lastId = Long.parseLong(String.valueOf(rowId));
                }
                if (!items.isEmpty()) {
                    Set<Long> sourceIds = items.stream().map(LegacyOrderListEntity::getSourceOrderId).collect(Collectors.toSet());
                    Map<Long, LegacyOrderListEntity> existing = archiveService.list(new LambdaQueryWrapper<LegacyOrderListEntity>()
                                    .eq(LegacyOrderListEntity::getSourceSystem, SOURCE)
                                    .in(LegacyOrderListEntity::getSourceOrderId, sourceIds))
                            .stream().collect(Collectors.toMap(LegacyOrderListEntity::getSourceOrderId, v -> v));
                    List<LegacyOrderListEntity> inserts = new ArrayList<>();
                    List<LegacyOrderListEntity> updates = new ArrayList<>();
                    for (LegacyOrderListEntity item : items) {
                        LegacyOrderListEntity old = existing.get(item.getSourceOrderId());
                        if (old == null) { inserts.add(item); }
                        else if (!String.valueOf(old.getSourceRowHash()).equals(item.getSourceRowHash())
                                || !MAPPING_VERSION.equals(old.getMappingVersion())) {
                            item.setId(old.getId()); updates.add(item);
                        } else skipped++;
                    }
                    if (!inserts.isEmpty()) { archiveService.saveBatch(inserts, BATCH_SIZE); inserted += inserts.size(); }
                    if (!updates.isEmpty()) { archiveService.updateBatchById(updates, BATCH_SIZE); updated += updates.size(); }
                }
                totalRead += rows.size();
                task.setHeartbeatAt(LocalDateTime.now());
                task.setTotalRead(totalRead); task.setInsertedCount(inserted); task.setUpdatedCount(updated);
                task.setSkippedCount(skipped); task.setErrorCount(errors); taskMapper.updateById(task);
                log.info("历史订单迁移批次完成，taskId={}, lastSourceId={}, totalRead={}, inserted={}, updated={}, skipped={}, errors={}",
                        taskId, lastId, totalRead, inserted, updated, skipped, errors);
            }
            task.setStatus(errors == 0 && warnings.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS");
            if (!warnings.isEmpty()) task.setErrorMessage(String.join("；", warnings));
            log.info("历史订单迁移任务完成，taskId={}, status={}, totalRead={}, inserted={}, updated={}, skipped={}, errors={}, warnings={}",
                    taskId, task.getStatus(), totalRead, inserted, updated, skipped, errors, warnings.size());
        } catch (Exception ex) {
            task.setStatus("FAILED");
            task.setErrorMessage(userFriendlyError(ex));
            log.error("历史订单迁移任务失败，taskId={}", taskId, ex);
        } finally {
            task.setFinishedAt(LocalDateTime.now());
            try {
                taskMapper.updateById(task);
            } catch (Exception updateEx) {
                // 主库短暂不可用时不能让异步线程再次抛异常；下次创建任务时会按心跳恢复该任务。
                log.error("保存迁移任务最终状态失败，taskId={}", taskId, updateEx);
            }
        }
        }
    }

    private void recoverStaleTasks() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(STALE_TASK_MINUTES);
        List<LegacyMigrationTaskEntity> active = taskMapper.selectList(new LambdaQueryWrapper<LegacyMigrationTaskEntity>()
                .in(LegacyMigrationTaskEntity::getStatus, "PENDING", "RUNNING"));
        for (LegacyMigrationTaskEntity task : active) {
            LocalDateTime checkpoint = "RUNNING".equals(task.getStatus()) ?
                    (task.getHeartbeatAt() == null ? task.getStartedAt() : task.getHeartbeatAt()) : task.getRequestedAt();
            if (checkpoint != null && checkpoint.isBefore(deadline)) {
                task.setStatus("FAILED"); task.setErrorMessage("迁移任务超时，已自动标记失败"); task.setFinishedAt(LocalDateTime.now());
                taskMapper.updateById(task);
            }
        }
    }

    private Map<String, PostalInfo> loadPostalMap(JdbcTemplate jdbc, Set<String> orderCodes, List<String> warnings) {
        Map<String, PostalInfo> result = new HashMap<>();
        if (orderCodes.isEmpty()) return result;
        List<Map<String, Object>> rows;
        try {
            String placeholders = String.join(",", java.util.Collections.nCopies(orderCodes.size(), "?"));
            rows = jdbc.queryForList("SELECT od_num, inst_mail, inst_mailaddress FROM jy_instruct WHERE od_num IN (" + placeholders + ")",
                    orderCodes.toArray());
        } catch (DataAccessException ex) {
            log.warn("旧库邮寄指令表不可用，将跳过邮寄信息映射并继续迁移", ex);
            addWarning(warnings, "邮寄信息表不可用，邮寄信息未完成映射");
            return result;
        }
        for (Map<String, Object> row : rows) {
            String order = text(row.get("od_num"));
            if (!StringUtils.hasText(order)) continue;
            String mail = text(row.get("inst_mail"));
            String address = text(row.get("inst_mailaddress"));
            PostalInfo old = result.get(order);
            if ("是".equals(mail)) {
                String mergedAddress = old == null ? address : mergeAddress(old.address(), address);
                result.put(order, new PostalInfo(true, mergedAddress));
            } else if (!result.containsKey(order)) {
                result.put(order, new PostalInfo(false, StringUtils.hasText(address) ? address : null));
            }
        }
        return result;
    }

    private Map<Long, String> loadModuleMap(JdbcTemplate jdbc, Set<Long> moduleIds, List<String> warnings) {
        Map<Long, String> result = new HashMap<>();
        if (moduleIds.isEmpty()) return result;
        List<Map<String, Object>> rows;
        try {
            String placeholders = String.join(",", java.util.Collections.nCopies(moduleIds.size(), "?"));
            rows = jdbc.queryForList("SELECT id_part, rebuildSub FROM jy_module WHERE id_part IN (" + placeholders + ")",
                    moduleIds.toArray());
        } catch (DataAccessException ex) {
            log.warn("旧库模块表不可用，将保留重建项目原始值并继续迁移", ex);
            addWarning(warnings, "重建项目字典表不可用，已保留原始项目值");
            return result;
        }
        for (Map<String, Object> row : rows) {
            Object id = row.get("id_part");
            if (id != null) result.put(Long.valueOf(String.valueOf(id)), text(row.get("rebuildSub")));
        }
        return result;
    }

    private Set<String> orderCodes(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> text(row.get("od_num")))
                .filter(StringUtils::hasText).collect(Collectors.toSet());
    }

    private Set<Long> moduleIds(List<Map<String, Object>> rows) {
        Set<Long> ids = new java.util.HashSet<>();
        for (Map<String, Object> row : rows) {
            for (String key : List.of("od_requirePart", "od_rebuildSub", "od_subjectExpl")) {
                String raw = text(row.get(key));
                if (!StringUtils.hasText(raw)) continue;
                for (String value : raw.split("[,，;；\\s]+")) {
                    try { ids.add(Long.valueOf(value)); } catch (NumberFormatException ignored) { }
                }
            }
        }
        return ids;
    }

    private void addWarning(List<String> warnings, String warning) {
        if (!warnings.contains(warning)) warnings.add(warning);
    }

    private LegacyOrderListEntity convert(Map<String, Object> r, Map<String, PostalInfo> postalMap, Map<Long, String> moduleMap) throws Exception {
        LegacyOrderListEntity e = new LegacyOrderListEntity();
        e.setSourceSystem(SOURCE);
        e.setSourceOrderId(number(r.get("od_id"), Long.class));
        e.setSourceOrderCode(text(r.get("od_num")));
        e.setStatusRaw(text(r.get("od_status")));
        e.setBusinessTypeRaw(text(r.get("od_orderType")));
        e.setPrintRaw(text(r.get("od_print")));
        e.setLegacyDeptName(text(r.get("od_dept")));
        e.setOperatorName(text(r.get("od_clerkname")));
        e.setOperatorPhone(text(r.get("od_clerkTel")));
        e.setHospitalName(text(r.get("od_hospital")));
        e.setAreaName(text(r.get("od_dist")));
        e.setHospitalDeptName(text(r.get("od_office")));
        e.setDoctorName(text(first(r, "od_docname", "od_doctor")));
        e.setDoctorPhone(text(first(r, "od_doctel", "od_docTel")));
        e.setPatientName(text(r.get("od_patient")));
        e.setPatientAgeRaw(text(r.get("od_patientAge")));
        e.setPatientGenderRaw(text(r.get("od_patientSex")));
        PostalInfo postal = postalMap.get(e.getSourceOrderCode());
        e.setPostalFlag(postal == null ? null : postal.mail() ? 1 : 0);
        e.setPostalAddressDisplay(postal == null ? null : postal.address());
        e.setDesignerName(text(r.get("od_designername")));
        e.setExpectedDeliveryRaw(text(r.get("od_preDelivery")));
        e.setExpectedDeliveryTime(timestamp(r.get("od_preDelivery")));
        // 收费字段严格映射旧系统 od_fee，保留原始文本；不与 od_subjectCost 混用。
        e.setEstimatedCostRaw(text(r.get("od_fee")));
        e.setDeliveryRaw(text(r.get("od_delivery")));
        e.setDeliveryTime(timestamp(r.get("od_delivery")));
        e.setEstimatedCostNumber(parseEstimatedCost(e.getEstimatedCostRaw()));
        e.setDataEvaluationOpinion(text(r.get("od_assess")));
        String parts = text(r.get("od_requirePart"));
        String projects = text(r.get("od_rebuildSub"));
        String explain = text(r.get("od_subjectExpl"));
        e.setRebuildProjectSummary(List.of(resolveProject(parts, moduleMap), resolveProject(projects, moduleMap), resolveProject(explain, moduleMap))
                .stream().filter(StringUtils::hasText).collect(Collectors.joining(" / ")));
        e.setDesignStartRaw(text(r.get("ds_time"))); e.setDesignStartTime(timestamp(r.get("ds_time")));
        e.setDesignSubmitRaw(text(r.get("dc_time"))); e.setDesignSubmitTime(timestamp(r.get("dc_time")));
        e.setProductionStartRaw(text(r.get("ps_time"))); e.setProductionStartTime(timestamp(r.get("ps_time")));
        e.setProductionEndRaw(text(r.get("pc_time"))); e.setProductionEndTime(timestamp(r.get("pc_time")));
        e.setCreateTimeRaw(text(r.get("od_createtime"))); e.setSourceCreateTime(timestamp(r.get("od_createtime")));
        e.setSourceSnapshotAt(LocalDateTime.now());
        e.setMappingVersion(MAPPING_VERSION);
        e.setSourceRowHash(hash(r));
        return e;
    }

    /**
     * 归档表数值列为 DECIMAL(18,2)，旧系统费用字段可能包含超大值、单位或其他文本。
     * 原始值已经单独保存在 estimated_cost_raw 中，超出目标列范围时只跳过数值列，
     * 不能让一条异常历史数据回滚整个迁移批次。
     */
    private BigDecimal parseEstimatedCost(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            BigDecimal value = new BigDecimal(raw.trim()).setScale(2, RoundingMode.HALF_UP);
            // DECIMAL(18,2) 最多 16 位整数、2 位小数。
            return value.precision() - value.scale() <= 16 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveProject(String raw, Map<Long, String> moduleMap) {
        if (!StringUtils.hasText(raw)) return null;
        return java.util.Arrays.stream(raw.split("[,，;；\\s]+"))
                .filter(StringUtils::hasText)
                .map(value -> {
                    try { Long id = Long.valueOf(value); String name = moduleMap.get(id); return name == null ? value : value + "(" + name + ")"; }
                    catch (NumberFormatException ignored) { return value; }
                }).collect(Collectors.joining(", "));
    }
    private void saveError(Long taskId, Map<String, Object> row, Exception ex) {
        try {
            LegacyMigrationErrorEntity error = new LegacyMigrationErrorEntity();
            error.setTaskId(taskId); error.setSourceOrderId(row.get("od_id") == null ? null : Long.valueOf(String.valueOf(row.get("od_id"))));
            error.setSourceOrderCode(text(row.get("od_num"))); error.setErrorType(ex.getClass().getSimpleName());
            error.setErrorMessage(limit(ex.getMessage() == null ? "未知迁移错误" : ex.getMessage(), 1900));
            // TEXT 按字节限制，保守截断，避免中文内容超过 64KB 导致错误明细再次写入失败。
            error.setRawSnapshot(limit(row.toString(), 12000));
            errorMapper.insert(error);
        } catch (Exception saveEx) {
            log.error("保存迁移错误明细失败，taskId={}, sourceOrderId={}", taskId, row.get("od_id"), saveEx);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "...";
    }
    private String userFriendlyError(Exception ex) {
        if (ex instanceof DataAccessException) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (message.contains("bad sql grammar") || message.contains("unknown column") || message.contains("doesn't exist")) {
                return "旧医工宝数据库结构与迁移映射不一致，请联系管理员检查旧库表结构。";
            }
            if (message.contains("connection") || message.contains("communications link") || message.contains("connect")) {
                return "旧医工宝数据库连接失败，请检查数据库地址、账号密码及网络。";
            }
            return "读取旧医工宝数据失败，请联系管理员查看迁移日志。";
        }
        return "历史订单迁移失败，请联系管理员查看迁移日志。";
    }
    private String mergeAddress(String left, String right) {
        if (!StringUtils.hasText(left)) return right;
        if (!StringUtils.hasText(right) || left.contains(right)) return left;
        return left + "；" + right;
    }
    private String text(Object value) { return value == null ? null : String.valueOf(value).trim(); }
    private Object first(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) return row.get(key);
            String matched = row.keySet().stream().filter(k -> k.equalsIgnoreCase(key)).findFirst().orElse(null);
            if (matched != null && row.get(matched) != null) return row.get(matched);
        }
        return null;
    }
    @SuppressWarnings("unchecked") private <T> T number(Object v, Class<T> type) { if (v == null) return null; return (T) Long.valueOf(String.valueOf(v)); }
    private LocalDateTime timestamp(Object v) {
        if (v instanceof Timestamp t) return t.toLocalDateTime();
        if (v instanceof java.sql.Date d) return d.toLocalDate().atStartOfDay();
        if (v instanceof LocalDateTime t) return t;
        if (v instanceof java.util.Date d) return LocalDateTime.ofInstant(d.toInstant(), java.time.ZoneId.systemDefault());
        String value = text(v);
        if (!StringUtils.hasText(value)) return null;
        for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"))) {
            try { return LocalDateTime.parse(value, formatter); }
            catch (DateTimeParseException ignored) { }
            try { return java.time.LocalDate.parse(value, formatter).atStartOfDay(); }
            catch (DateTimeException ignored) { }
        }
        return null;
    }
    private String hash(Map<String, Object> row) throws Exception {
        String canonical = row.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .map(entry -> entry.getKey() + "=" + String.valueOf(entry.getValue()))
                .collect(Collectors.joining("|"));
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
    }
    private record PostalInfo(boolean mail, String address) { }
}
