package com.yigongbao.module.backup;

import com.yigongbao.module.backup.config.BackupModuleConfiguration;
import com.yigongbao.module.backup.config.BackupProperties;
import com.yigongbao.module.backup.security.filter.MachineHmacAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = BackupApplicationContextTest.TestApplication.class)
class BackupApplicationContextTest {

    @Autowired
    private BackupProperties backupProperties;

    @Autowired
    private MachineHmacAuthenticationFilter machineHmacAuthenticationFilter;

    @Autowired
    private FilterRegistrationBean<MachineHmacAuthenticationFilter> machineHmacFilterRegistration;

    @Test
    void contextLoadsWithSafeDefaultsAndExplicitTestBypass() {
        assertThat(machineHmacAuthenticationFilter).isNotNull();
        assertThat(machineHmacFilterRegistration.getUrlPatterns()).containsExactly("/internal/*");
        assertThat(backupProperties.isCosDeletionEnabled()).isFalse();
        assertThat(backupProperties.getSecurity().isAllowUnverifiedRequestsForTests()).isTrue();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import(BackupModuleConfiguration.class)
    static class TestApplication {
    }
}
