package com.yigongbao.module.basic.rebuildProject.mapper;

import com.yigongbao.module.basic.BasicTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = BasicTestApplication.class)
@ActiveProfiles("test")
class RebuildProjectMapperTest {

    @Autowired
    private RebuildProjectMapper mapper;

    @Test
    void selectCategoryNameSkipsInvalidRowsBeforeLimit() {
        assertEquals("导板", mapper.selectCategoryName("13.2"));
    }
}
