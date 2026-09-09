package com.yigongbao.module.production.qc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.service.IProductionProductService;
import com.yigongbao.module.production.qc.dto.SaveQcColumnConfigDTO;
import com.yigongbao.module.production.qc.vo.QcColumnConfigVO;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionQcColumnConfigServiceTest {

    @Mock private ProductionProductMapper productMapper;
    @Mock private ProductionRecordMapper recordMapper;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private IProductionRecordService recordService;
    @Mock private OrderMainMapper orderMainMapper;
    @Mock private UserMapper userMapper;
    @Mock private IProductionProductService productService;
    @Mock private UserService userService;
    @Mock private ConfigService configService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ProductionQcServiceImpl qcService;

    @Test
    void getColumnConfig_userConfigWins() throws Exception {
        UserEntity user = new UserEntity();
        user.setQualityColumnSettings("user-json");
        QcColumnConfigVO expected = config("recordNo");
        expected.setVersion(com.yigongbao.common.constant.ColumnConfigConstants.CURRENT_VERSION);
        when(userService.getById(1L)).thenReturn(user);
        when(objectMapper.readValue("user-json", QcColumnConfigVO.class)).thenReturn(expected);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertSame(expected, qcService.getColumnConfig());
        }
        verifyNoInteractions(configService);
    }

    @Test
    void getColumnConfig_invalidUserConfig_fallsBackToSystem() throws Exception {
        UserEntity user = new UserEntity();
        user.setQualityColumnSettings("invalid");
        QcColumnConfigVO expected = config("designPackageCode");
        when(userService.getById(1L)).thenReturn(user);
        when(objectMapper.readValue("invalid", QcColumnConfigVO.class))
                .thenThrow(new JsonProcessingException("invalid") {});
        when(configService.getConfigValue(SystemConfigKeyEnum.QUALITY_COLUMN_CONFIG.getKey()))
                .thenReturn("system-json");
        when(objectMapper.readValue("system-json", QcColumnConfigVO.class)).thenReturn(expected);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertSame(expected, qcService.getColumnConfig());
        }
    }

    @Test
    void saveColumnConfig_serializesToQualityUserField() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getById(1L)).thenReturn(user);
        when(objectMapper.writeValueAsString(any(QcColumnConfigVO.class))).thenReturn("saved-json");
        SaveQcColumnConfigDTO dto = new SaveQcColumnConfigDTO();
        SaveQcColumnConfigDTO.ColumnItemDTO item = new SaveQcColumnConfigDTO.ColumnItemDTO();
        item.setField("recordNo");
        item.setLabel("流转卡编号");
        item.setVisible(true);
        item.setSort(1);
        dto.setColumns(List.of(item));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            qcService.saveColumnConfig(dto);
        }
        verify(userService).updateById(argThat(updated ->
                "saved-json".equals(((UserEntity) updated).getQualityColumnSettings())));
    }

    @Test
    void saveColumnConfig_rejectsUnknownField() {
        SaveQcColumnConfigDTO dto = new SaveQcColumnConfigDTO();
        SaveQcColumnConfigDTO.ColumnItemDTO item = new SaveQcColumnConfigDTO.ColumnItemDTO();
        item.setField("not-a-quality-field");
        item.setLabel("非法字段");
        item.setVisible(true);
        item.setSort(1);
        dto.setColumns(List.of(item));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertThrows(BusinessException.class, () -> qcService.saveColumnConfig(dto));
        }
        verify(userService, never()).updateById(any());
    }

    private QcColumnConfigVO config(String field) {
        QcColumnConfigVO config = new QcColumnConfigVO();
        QcColumnConfigVO.ColumnItemVO item = new QcColumnConfigVO.ColumnItemVO();
        item.setField(field);
        config.setColumns(List.of(item));
        return config;
    }
}
