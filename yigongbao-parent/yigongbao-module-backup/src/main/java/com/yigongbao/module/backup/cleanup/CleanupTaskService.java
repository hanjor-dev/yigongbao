package com.yigongbao.module.backup.cleanup;

import com.yigongbao.module.backup.cleanup.entity.CleanupItemEntity;
import com.yigongbao.module.backup.cleanup.entity.CleanupPermitEntity;
import com.yigongbao.module.backup.cleanup.entity.CleanupPolicyEntity;
import com.yigongbao.module.backup.cleanup.entity.CleanupSnapshotEntity;
import com.yigongbao.module.backup.cleanup.entity.CleanupTaskEntity;
import com.yigongbao.module.backup.cleanup.mapper.CleanupItemMapper;
import com.yigongbao.module.backup.cleanup.mapper.CleanupPermitMapper;
import com.yigongbao.module.backup.cleanup.mapper.CleanupPolicyMapper;
import com.yigongbao.module.backup.cleanup.mapper.CleanupSnapshotMapper;
import com.yigongbao.module.backup.cleanup.mapper.CleanupTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * COS 清理任务持久化服务。
 *
 * <p>任务、对象明细和删除授权全部写入 MyBatis-Plus 对应表，服务本身不持有
 * 运行期 Map，因此进程重启不会丢失任务状态或 permit 生命周期。</p>
 */
@Service
public class CleanupTaskService {
    private final CleanupTaskMapper taskMapper;
    private final CleanupItemMapper itemMapper;
    private final CleanupPolicyMapper policyMapper;
    private final CleanupSnapshotMapper snapshotMapper;
    private final CleanupPermitMapper permitMapper;

    /** 由 Spring 注入全部持久化依赖。 */
    public CleanupTaskService(CleanupTaskMapper taskMapper, CleanupItemMapper itemMapper,
                              CleanupPolicyMapper policyMapper, CleanupSnapshotMapper snapshotMapper,
                              CleanupPermitMapper permitMapper) {
        this.taskMapper = Objects.requireNonNull(taskMapper);
        this.itemMapper = Objects.requireNonNull(itemMapper);
        this.policyMapper = Objects.requireNonNull(policyMapper);
        this.snapshotMapper = Objects.requireNonNull(snapshotMapper);
        this.permitMapper = Objects.requireNonNull(permitMapper);
    }

    /** 执行候选评估；正式任务创建前不产生删除授权。 */
    public Evaluation evaluate(int olderThanDays) {
        if (olderThanDays < 0) {
            throw new IllegalArgumentException("清理天数不能为负");
        }
        return new Evaluation(UUID.randomUUID().toString(), "DRY_RUN", List.of(), 0L);
    }

    /** 使用输入内容生成稳定幂等键，重复请求返回已有任务。 */
    @Transactional
    public Task create(int olderThanDays, List<String> keys) {
        if (olderThanDays < 0 || keys == null) {
            throw new IllegalArgumentException("参数非法");
        }
        List<String> normalizedKeys = keys.stream().filter(Objects::nonNull).map(String::trim)
                .filter(key -> !key.isEmpty()).distinct().sorted().toList();
        String idempotencyKey = "cleanup:" + sha256(olderThanDays + "\n" + String.join("\n", normalizedKeys));
        return create(olderThanDays, normalizedKeys, idempotencyKey);
    }

    /** 由内部调度器传入显式幂等键，支持同一业务批次跨进程重试。 */
    @Transactional
    public Task create(int olderThanDays, List<String> keys, String idempotencyKey) {
        if (olderThanDays < 0 || keys == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("参数非法");
        }
        List<String> normalizedKeys = keys.stream().filter(Objects::nonNull).map(String::trim)
                .filter(key -> !key.isEmpty()).distinct().sorted().toList();
        CleanupTaskEntity existing = taskMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return toTask(existing);
        }

        String objectJson = normalizedKeys.stream()
                .map(key -> "\"" + jsonEscape(key) + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        String objectSetHash = sha256(String.join("\n", normalizedKeys));
        // 阈值是策略版本的一部分，避免不同天数的任务复用错误的 policy_json。
        int policyVersion = olderThanDays;
        CleanupPolicyEntity policy = policyMapper.selectByCodeVersion("COS_AUTO_CLEANUP", policyVersion);
        if (policy == null) {
            policy = new CleanupPolicyEntity();
            policy.setPolicyCode("COS_AUTO_CLEANUP");
            policy.setPolicyVersion(policyVersion);
            policy.setPolicyJson("{\"olderThanDays\":" + olderThanDays + "}");
            policy.setEnabled(1);
            policy.setVersion(0);
            policyMapper.insert(policy);
        }

        CleanupSnapshotEntity snapshot = new CleanupSnapshotEntity();
        snapshot.setOrderId(0L);
        snapshot.setReceiptId(0L);
        snapshot.setObjectSetHash(objectSetHash);
        snapshot.setSnapshotJson(objectJson);
        snapshot.setSnapshotHash(sha256(olderThanDays + "\n" + objectJson));
        snapshot.setOrderStatus(0);
        snapshot.setClassicCase(0);
        CleanupSnapshotEntity existingSnapshot = snapshotMapper.selectByHash(snapshot.getSnapshotHash());
        if (existingSnapshot != null) {
            snapshot = existingSnapshot;
        } else {
            snapshotMapper.insert(snapshot);
        }

        CleanupTaskEntity task = new CleanupTaskEntity();
        task.setTaskNo(UUID.randomUUID().toString());
        task.setIdempotencyKey(idempotencyKey);
        task.setPolicyId(policy.getId());
        task.setSnapshotId(snapshot.getId());
        task.setMode("ACTUAL");
        task.setTaskStatus("RUNNING");
        task.setEstimatedVersionBytes(0L);
        task.setActualPurgedBytes(0L);
        task.setVersion(0);
        taskMapper.insert(task);

        for (String key : normalizedKeys) {
            CleanupItemEntity item = new CleanupItemEntity();
            item.setCleanupTaskId(task.getId());
            item.setPlatform("cos");
            item.setBucket("");
            item.setObjectKey(key);
            item.setVersionIdentity("*");
            item.setSharedReferenceCount(0);
            item.setDeleteStatus("PENDING");
            item.setVersionBytes(0L);
            item.setRetryCount(0);
            item.setVersion(0);
            itemMapper.insert(item);
        }
        return new Task(task.getTaskNo(), olderThanDays, task.getTaskStatus(), normalizedKeys);
    }

    /** 读取任务及其对象明细。 */
    @Transactional(readOnly = true)
    public Task get(String id) {
        CleanupTaskEntity task = taskMapper.selectByTaskNo(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        return toTask(task);
    }

    /** 按持久化创建时间倒序返回任务。 */
    @Transactional(readOnly = true)
    public List<Task> list() {
        return taskMapper.selectAllActive().stream().map(this::toTask)
                .sorted(Comparator.comparing(Task::id).reversed()).toList();
    }

    /** 返回任务对象明细，供管理页展示每个对象的删除结果。 */
    @Transactional(readOnly = true)
    public List<Item> items(String id) {
        CleanupTaskEntity task = taskMapper.selectByTaskNo(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        return itemMapper.selectByTaskId(task.getId()).stream().map(Item::from).toList();
    }

    /** 暂停运行中的任务，使用数据库版本号避免并发状态覆盖。 */
    @Transactional
    public Task pause(String id) {
        return transition(id, "RUNNING", "PAUSED");
    }

    /** 恢复已暂停任务。 */
    @Transactional
    public Task resume(String id) {
        return transition(id, "PAUSED", "RUNNING");
    }

    /** 为运行中的非空任务签发一个持久化、可撤销的删除授权。 */
    @Transactional
    public Permit issuePermit(String id) {
        CleanupTaskEntity task = taskMapper.selectByTaskNo(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        List<CleanupItemEntity> items = itemMapper.selectByTaskId(task.getId());
        if (!"RUNNING".equals(task.getTaskStatus()) || items.isEmpty()) {
            throw new IllegalStateException("任务尚未满足授权条件");
        }
        CleanupPermitEntity active = permitMapper.selectActiveByTaskId(task.getId());
        if (active != null) {
            if (active.getExpiresAt() != null && !active.getExpiresAt().isAfter(LocalDateTime.now())) {
                permitMapper.expireIfNecessary(active.getPermitId());
            } else {
                return toPermit(active, items);
            }
        }
        CleanupSnapshotEntity snapshot = snapshotMapper.selectById(task.getSnapshotId());
        int attempt = permitMapper.selectMaxAttempt(task.getId()) + 1;
        CleanupPermitEntity permit = new CleanupPermitEntity();
        permit.setPermitId(UUID.randomUUID().toString());
        permit.setCleanupTaskId(task.getId());
        permit.setAttemptNo(attempt);
        permit.setPermitStatus("ISSUED");
        permit.setCleanupSnapshotHash(snapshot == null ? "" : snapshot.getSnapshotHash());
        permit.setObjectSetHash(snapshot == null ? "" : snapshot.getObjectSetHash());
        permit.setFencingToken(System.currentTimeMillis());
        permit.setReceiptId(0L);
        permit.setReceiptVersion(0);
        permit.setPackageHash("");
        permit.setNonce(UUID.randomUUID().toString());
        permit.setIssuedAt(LocalDateTime.now());
        permit.setExpiresAt(LocalDateTime.now().plusHours(1));
        permit.setVersion(0);
        permitMapper.insert(permit);
        return toPermit(permit, items);
    }

    /** 撤销仍处于有效生命周期的授权。 */
    @Transactional
    public Permit revoke(String permitId) {
        CleanupPermitEntity permit = permitMapper.selectByPermitId(permitId);
        if (permit == null) {
            throw new IllegalArgumentException("授权不存在");
        }
        if ("REQUESTED".equals(permit.getPermitStatus()) || "ISSUED".equals(permit.getPermitStatus())
                || "CLAIMED".equals(permit.getPermitStatus())) {
            if (permitMapper.revoke(permitId) == 1) {
                permit.setPermitStatus("REVOKED");
            }
        }
        List<CleanupItemEntity> items = itemMapper.selectByTaskId(permit.getCleanupTaskId());
        return toPermit(permit, items);
    }

    private Task transition(String id, String expected, String target) {
        CleanupTaskEntity task = taskMapper.selectByTaskNo(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!expected.equals(task.getTaskStatus())) {
            throw new IllegalStateException("任务不可" + ("PAUSED".equals(target) ? "暂停" : "恢复"));
        }
        if (taskMapper.updateStatus(id, expected, target, task.getVersion()) != 1) {
            throw new IllegalStateException("任务状态已被其他操作更新，请刷新后重试");
        }
        task.setTaskStatus(target);
        task.setVersion(task.getVersion() + 1);
        return toTask(task);
    }

    private Task toTask(CleanupTaskEntity entity) {
        CleanupPolicyEntity policy = entity.getPolicyId() == null ? null : policyMapper.selectById(entity.getPolicyId());
        int days = parseDays(policy == null ? null : policy.getPolicyJson());
        List<String> keys = itemMapper.selectByTaskId(entity.getId()).stream()
                .map(CleanupItemEntity::getObjectKey).toList();
        return new Task(entity.getTaskNo(), days, entity.getTaskStatus(), keys);
    }

    private Permit toPermit(CleanupPermitEntity entity, List<CleanupItemEntity> items) {
        return new Permit(entity.getPermitId(), String.valueOf(entity.getCleanupTaskId()),
                items.stream().map(CleanupItemEntity::getObjectKey).toList(), entity.getPermitStatus(),
                entity.getExpiresAt());
    }

    private static int parseDays(String json) {
        if (json == null) return 0;
        int colon = json.indexOf(':');
        int end = json.indexOf('}', colon);
        if (colon < 0 || end < 0) return 0;
        try { return Integer.parseInt(json.substring(colon + 1, end)); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM 不支持 SHA-256", exception);
        }
    }

    /** 评估结果，不会授权删除。 */
    public record Evaluation(String taskId, String status, List<String> keys, long estimatedBytes) {}

    /** 管理端任务摘要。 */
    public record Task(String id, int olderThanDays, String status, List<String> keys) {
        public String id() { return id; }
        public String status() { return status; }
        public List<String> keys() { return keys; }
    }

    /** 管理端对象明细。 */
    public record Item(Long id, String objectKey, String deleteStatus, int retryCount, String lastError) {
        static Item from(CleanupItemEntity item) {
            return new Item(item.getId(), item.getObjectKey(), item.getDeleteStatus(),
                    item.getRetryCount() == null ? 0 : item.getRetryCount(), item.getLastError());
        }
    }

    /** 删除授权摘要。 */
    public record Permit(String id, String taskId, List<String> keys, String status,
                         LocalDateTime expiresAt) {
        public String id() { return id; }
    }
}
