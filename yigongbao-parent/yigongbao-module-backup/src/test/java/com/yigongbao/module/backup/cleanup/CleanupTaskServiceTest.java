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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CleanupTaskServiceTest {

    @Test
    void createPersistsTaskAndEveryObjectBeforeReturningIt() {
        CleanupTaskMapper taskMapper = mock(CleanupTaskMapper.class);
        CleanupItemMapper itemMapper = mock(CleanupItemMapper.class);
        CleanupPolicyMapper policyMapper = mock(CleanupPolicyMapper.class);
        CleanupSnapshotMapper snapshotMapper = mock(CleanupSnapshotMapper.class);
        CleanupPermitMapper permitMapper = mock(CleanupPermitMapper.class);
        doAnswer(invocation -> {
            ((CleanupPolicyEntity) invocation.getArgument(0)).setId(11L);
            return 1;
        }).when(policyMapper).insert(any(CleanupPolicyEntity.class));
        doAnswer(invocation -> {
            ((CleanupSnapshotEntity) invocation.getArgument(0)).setId(12L);
            return 1;
        }).when(snapshotMapper).insert(any(CleanupSnapshotEntity.class));
        doAnswer(invocation -> {
            ((CleanupTaskEntity) invocation.getArgument(0)).setId(13L);
            return 1;
        }).when(taskMapper).insert(any(CleanupTaskEntity.class));

        CleanupTaskService service = new CleanupTaskService(
                taskMapper, itemMapper, policyMapper, snapshotMapper, permitMapper);

        CleanupTaskService.Task task = service.create(180, List.of("cos/object-a", "cos/object-b"));

        assertThat(task.status()).isEqualTo("RUNNING");
        assertThat(task.keys()).containsExactly("cos/object-a", "cos/object-b");
        verify(policyMapper).insert(any(CleanupPolicyEntity.class));
        verify(snapshotMapper).insert(any(CleanupSnapshotEntity.class));
        verify(taskMapper).insert(any(CleanupTaskEntity.class));
        verify(itemMapper, times(2)).insert(any(CleanupItemEntity.class));
    }

    @Test
    void duplicateIdempotencyKeyReturnsThePersistedTaskWithoutCreatingObjectsAgain() {
        CleanupTaskMapper taskMapper = mock(CleanupTaskMapper.class);
        CleanupItemMapper itemMapper = mock(CleanupItemMapper.class);
        CleanupPolicyMapper policyMapper = mock(CleanupPolicyMapper.class);
        CleanupSnapshotMapper snapshotMapper = mock(CleanupSnapshotMapper.class);
        CleanupPermitMapper permitMapper = mock(CleanupPermitMapper.class);
        CleanupTaskEntity existing = new CleanupTaskEntity();
        existing.setId(21L);
        existing.setTaskNo("task-existing");
        existing.setTaskStatus("RUNNING");
        when(taskMapper.selectByIdempotencyKey(any())).thenReturn(existing);
        CleanupItemEntity item = new CleanupItemEntity();
        item.setObjectKey("cos/object-a");
        when(itemMapper.selectByTaskId(21L)).thenReturn(List.of(item));

        CleanupTaskService service = new CleanupTaskService(
                taskMapper, itemMapper, policyMapper, snapshotMapper, permitMapper);

        CleanupTaskService.Task task = service.create(180, List.of("cos/object-a"));

        assertThat(task.id()).isEqualTo("task-existing");
        verify(taskMapper, never()).insert(any(CleanupTaskEntity.class));
        verify(itemMapper, never()).insert(any(CleanupItemEntity.class));
    }

    @Test
    void issuePermitPersistsAuthorizationForTheTaskObjectSet() {
        CleanupTaskMapper taskMapper = mock(CleanupTaskMapper.class);
        CleanupItemMapper itemMapper = mock(CleanupItemMapper.class);
        CleanupPolicyMapper policyMapper = mock(CleanupPolicyMapper.class);
        CleanupSnapshotMapper snapshotMapper = mock(CleanupSnapshotMapper.class);
        CleanupPermitMapper permitMapper = mock(CleanupPermitMapper.class);
        CleanupTaskEntity task = new CleanupTaskEntity();
        task.setId(31L);
        task.setTaskNo("task-31");
        task.setSnapshotId(41L);
        task.setTaskStatus("RUNNING");
        CleanupItemEntity item = new CleanupItemEntity();
        item.setObjectKey("cos/object-a");
        item.setDeleteStatus("PENDING");
        when(taskMapper.selectByTaskNo("task-31")).thenReturn(task);
        when(itemMapper.selectByTaskId(31L)).thenReturn(List.of(item));
        when(permitMapper.selectMaxAttempt(31L)).thenReturn(0);
        CleanupSnapshotEntity snapshot = new CleanupSnapshotEntity();
        snapshot.setSnapshotHash("snapshot-hash");
        snapshot.setObjectSetHash("object-set-hash");
        when(snapshotMapper.selectById(41L)).thenReturn(snapshot);

        CleanupTaskService service = new CleanupTaskService(
                taskMapper, itemMapper, policyMapper, snapshotMapper, permitMapper);

        CleanupTaskService.Permit permit = service.issuePermit("task-31");

        assertThat(permit.status()).isEqualTo("ISSUED");
        assertThat(permit.keys()).containsExactly("cos/object-a");
        assertThat(permit.expiresAt()).isAfter(LocalDateTime.now());
        verify(permitMapper).insert(any(CleanupPermitEntity.class));
    }
}
