package com.yigongbao.module.production.warehouse.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.warehouse.dto.SaveWarehouseColumnConfigDTO;
import com.yigongbao.module.production.warehouse.vo.WarehouseColumnConfigVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseColumnConfigServiceTest {

    @Mock private ProductionProductMapper productMapper;
    @Mock private ProductionRecordMapper recordMapper;
    @Mock private IProductionRecordService recordService;
    @Mock private UserService userService;
    @Mock private ConfigService configService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    @Test
    void getColumnConfig_noUserConfig_returnsSystemConfig() throws Exception {
        UserEntity user = new UserEntity();
        WarehouseColumnConfigVO expected = config("recordNo");
        when(userService.getById(1L)).thenReturn(user);
        when(configService.getConfigValue(SystemConfigKeyEnum.WAREHOUSE_COLUMN_CONFIG.getKey()))
                .thenReturn("system-json");
        when(objectMapper.readValue("system-json", WarehouseColumnConfigVO.class)).thenReturn(expected);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertSame(expected, warehouseService.getColumnConfig());
        }
    }

    @Test
    void getColumnConfig_invalidUserConfig_fallsBackToSystem() throws Exception {
        UserEntity user = new UserEntity();
        user.setWarehouseColumnSettings("invalid");
        WarehouseColumnConfigVO expected = config("status");
        when(userService.getById(1L)).thenReturn(user);
        when(objectMapper.readValue("invalid", WarehouseColumnConfigVO.class))
                .thenThrow(new JsonProcessingException("invalid") {});
        when(configService.getConfigValue(SystemConfigKeyEnum.WAREHOUSE_COLUMN_CONFIG.getKey()))
                .thenReturn("system-json");
        when(objectMapper.readValue("system-json", WarehouseColumnConfigVO.class)).thenReturn(expected);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertSame(expected, warehouseService.getColumnConfig());
        }
    }

    @Test
    void saveColumnConfig_serializesToWarehouseUserField() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getById(1L)).thenReturn(user);
        when(objectMapper.writeValueAsString(any(WarehouseColumnConfigVO.class))).thenReturn("saved-json");
        SaveWarehouseColumnConfigDTO dto = new SaveWarehouseColumnConfigDTO();
        SaveWarehouseColumnConfigDTO.ColumnItemDTO item = new SaveWarehouseColumnConfigDTO.ColumnItemDTO();
        item.setField("recordNo");
        item.setLabel("流转卡编号");
        item.setVisible(true);
        item.setSort(1);
        dto.setColumns(List.of(item));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            warehouseService.saveColumnConfig(dto);
        }
        verify(userService).updateById(argThat(updated ->
                "saved-json".equals(((UserEntity) updated).getWarehouseColumnSettings())));
    }

    @Test
    void saveColumnConfig_rejectsDuplicateFields() {
        SaveWarehouseColumnConfigDTO dto = new SaveWarehouseColumnConfigDTO();
        SaveWarehouseColumnConfigDTO.ColumnItemDTO first = item("recordNo", 1);
        SaveWarehouseColumnConfigDTO.ColumnItemDTO second = item("recordNo", 2);
        dto.setColumns(List.of(first, second));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertThrows(BusinessException.class, () -> warehouseService.saveColumnConfig(dto));
        }
        verify(userService, never()).updateById(any());
    }

    private SaveWarehouseColumnConfigDTO.ColumnItemDTO item(String field, int sort) {
        SaveWarehouseColumnConfigDTO.ColumnItemDTO item = new SaveWarehouseColumnConfigDTO.ColumnItemDTO();
        item.setField(field);
        item.setLabel(field);
        item.setVisible(true);
        item.setSort(sort);
        return item;
    }

    private WarehouseColumnConfigVO config(String field) {
        WarehouseColumnConfigVO config = new WarehouseColumnConfigVO();
        WarehouseColumnConfigVO.ColumnItemVO item = new WarehouseColumnConfigVO.ColumnItemVO();
        item.setField(field);
        config.setColumns(List.of(item));
        return config;
    }
}
