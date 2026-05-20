package com.yigongbao.module.order.service.impl;

import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.module.order.dto.modify.ExecuteModifyDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderModifyDirectServiceTest {

    @InjectMocks
    private OrderModifyApplyServiceImpl service;

    @Test
    void testDetermineAllowedTypesByPhase_OrderPhase_ReturnsAllTypes() {
        Set<String> result = service.determineAllowedTypesByPhase(FlowPhaseEnum.ORDER.getValue());

        assertEquals(3, result.size());
        assertTrue(result.contains("14.1"));
        assertTrue(result.contains("14.2"));
        assertTrue(result.contains("14.3"));
    }

    @Test
    void testDetermineAllowedTypesByPhase_DesignPhase_ReturnsItemOnly() {
        Set<String> result = service.determineAllowedTypesByPhase(FlowPhaseEnum.DESIGN.getValue());

        assertEquals(1, result.size());
        assertTrue(result.contains("14.3"));
    }

    @Test
    void testDetermineAllowedTypesByPhase_InvalidPhase_ThrowsException() {
        assertThrows(BusinessException.class, () -> {
            service.determineAllowedTypesByPhase(30);
        });
    }

    @Test
    void testBuildModificationsMap_WithInfoFields_ReturnsCorrectMap() {
        ExecuteModifyDTO dto = new ExecuteModifyDTO();
        ExecuteModifyDTO.ModifyField field = new ExecuteModifyDTO.ModifyField();
        field.setField("patientName");
        field.setValue("张三");
        dto.setInfoFields(List.of(field));

        Map<String, Object> result = service.buildModificationsMap(dto);

        assertNotNull(result);
        assertTrue(result.containsKey("patientName"));
        assertEquals("张三", result.get("patientName"));
    }
}
