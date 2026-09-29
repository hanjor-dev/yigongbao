package com.yigongbao.module.order.service.legacy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** 使用主库 MySQL 命名锁，保证多实例环境最多一个迁移任务运行。 */
@Component
public class LegacyMigrationDistributedLock {
    private static final String LOCK_NAME = "yigongbao:legacy-order-migration";

    private final DataSource dataSource;

    public LegacyMigrationDistributedLock(@Qualifier("dataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public LockHandle tryAcquire(int timeoutSeconds) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)");
            statement.setString(1, LOCK_NAME);
            statement.setInt(2, timeoutSeconds);
            try (statement; ResultSet result = statement.executeQuery()) {
                if (result.next() && result.getInt(1) == 1) return new LockHandle(connection);
            }
            connection.close();
        } catch (Exception ex) {
            if (connection != null) {
                try { connection.close(); } catch (Exception ignored) { }
            }
            throw new IllegalStateException("迁移任务锁服务不可用，请稍后重试。", ex);
        }
        return null;
    }

    public final class LockHandle implements AutoCloseable {
        private final Connection connection;
        private boolean closed;

        private LockHandle(Connection connection) { this.connection = connection; }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                statement.setString(1, LOCK_NAME);
                statement.executeQuery().close();
            } catch (Exception ignored) {
                // 连接关闭后 MySQL 会自动释放命名锁。
            } finally {
                try { connection.close(); } catch (Exception ignored) { }
            }
        }
    }
}
