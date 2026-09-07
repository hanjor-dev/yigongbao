package com.yigongbao.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultConfigPropertiesTest {

    @Test
    void orderColumnConfig_containsVisibleDesignerRemark() {
        DefaultConfigProperties properties = new DefaultConfigProperties();

        assertThat(properties.getConfigOrderColumnConfig())
                .contains("\"field\":\"designerRemark\"")
                .contains("\"label\":\"设计师备注\"")
                .contains("\"visible\":true");
    }

    @Test
    void designColumnConfig_containsVisibleDesignerRemark() {
        DefaultConfigProperties properties = new DefaultConfigProperties();

        assertThat(properties.getConfigDesignColumnConfig())
                .contains("\"field\":\"designerRemark\"")
                .contains("\"label\":\"设计师备注\"")
                .contains("\"visible\":true");
    }
}
