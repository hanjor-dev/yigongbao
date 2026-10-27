package com.yigongbao.module.backup.candidate;

import com.yigongbao.common.event.OrderEligibilityChangedEvent;
import com.yigongbao.module.backup.candidate.listener.OrderEligibilityChangedListener;
import com.yigongbao.module.backup.candidate.service.CandidateEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

/** 验证 BEFORE_COMMIT 候选落库失败时，订单事务本身会回滚。 */
@SpringJUnitConfig(OrderEligibilityChangedListenerTransactionTest.TestConfig.class)
class OrderEligibilityChangedListenerTransactionTest {

    @jakarta.annotation.Resource
    private JdbcTemplate jdbcTemplate;
    @jakarta.annotation.Resource
    private TransactionTemplate transactionTemplate;
    @jakarta.annotation.Resource
    private ApplicationEventPublisher eventPublisher;
    @jakarta.annotation.Resource
    private CandidateEventService candidateEventService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS tx_order");
        jdbcTemplate.execute("CREATE TABLE tx_order (id BIGINT PRIMARY KEY, status VARCHAR(32))");
    }

    @Test
    void beforeCommitListenerFailureRollsBackBusinessTransaction() {
        doThrow(new IllegalStateException("candidate write failed"))
                .when(candidateEventService)
                .recordEligibilityChange(Mockito.anyLong(), Mockito.anyString(), Mockito.any());

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("INSERT INTO tx_order(id, status) VALUES (?, ?)", 1L, "FORMAL");
            eventPublisher.publishEvent(new OrderEligibilityChangedEvent(this, 1L,
                    OrderEligibilityChangedEvent.FORMAL, LocalDateTime.now()));
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tx_order", Integer.class)).isZero();
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        DataSource dataSource() {
            return new org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder()
                    .setType(org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }

        @Bean
        CandidateEventService candidateEventService() {
            return Mockito.mock(CandidateEventService.class);
        }

        @Bean
        OrderEligibilityChangedListener orderEligibilityChangedListener(
                CandidateEventService candidateEventService) {
            return new OrderEligibilityChangedListener(candidateEventService);
        }
    }
}
