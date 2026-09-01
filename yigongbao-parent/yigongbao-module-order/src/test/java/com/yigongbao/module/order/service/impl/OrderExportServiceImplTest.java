package com.yigongbao.module.order.service.impl;

import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.module.order.helper.OrderQueryHelper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.dto.order.OrderExportQueryDTO;
import com.yigongbao.module.order.dto.order.OrderCustomExportDTO;
import com.yigongbao.module.order.vo.order.OrderListVO;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.dto.workload.DesignerWorkloadExportDTO;
import com.yigongbao.module.system.user.service.UserHospitalService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.LocalDateTime;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExportServiceImplTest {

    @Mock private OrderMainMapper orderMainMapper;
    @Mock private UserHospitalService userHospitalService;
    @Mock private OrderQueryHelper orderQueryHelper;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private OrderExportServiceImpl service;

    @Test
    void availableExportFields_containsCorePatientAndProjectFields() {
        var fields = service.getAvailableExportFields();

        assertThat(fields).extracting("field").contains(
                "orderCode", "patientName", "projectName",
                "designStartTime", "designSubmitTime",
                "productionStartTime", "productionEndTime");
    }

    @Test
    void customExportOrders_writesOrderLifecycleTimes() throws Exception {
        OrderMainEntity entity = new OrderMainEntity();
        OrderListVO order = new OrderListVO();
        order.setDesignStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        order.setDesignSubmitTime(LocalDateTime.of(2026, 8, 2, 10, 0));
        order.setProductionStartTime(LocalDateTime.of(2026, 8, 3, 11, 0));
        order.setProductionEndTime(LocalDateTime.of(2026, 8, 4, 12, 0));
        when(orderMainMapper.selectList(any())).thenReturn(List.of(entity));
        when(orderQueryHelper.toOrderListVO(entity)).thenReturn(order);

        OrderCustomExportDTO dto = new OrderCustomExportDTO();
        dto.setExportFields(List.of("designStartTime", "designSubmitTime",
                "productionStartTime", "productionEndTime"));
        MockHttpServletResponse output = new MockHttpServletResponse();

        service.customExportOrders(dto, output);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.getContentAsByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("设计开始时间");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("2026-08-01 09:00:00");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("2026-08-02 10:00:00");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("2026-08-03 11:00:00");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("2026-08-04 12:00:00");
        }
    }

    @Test
    void exportOrders_rejectsMissingColumnConfiguration() {
        when(orderQueryHelper.getColumnConfig()).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.exportOrders(new OrderExportQueryDTO(), response));

        assertThat(exception.getMessage()).contains("列配置");
        verifyNoInteractions(orderMainMapper);
    }

    @Test
    void exportDesignerWorkload_rejectsEmptyStatistics() {
        when(orderMainMapper.statisticsDesignerWorkload(any(), any())).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.exportDesignerWorkload(new DesignerWorkloadExportDTO(), response));

        assertThat(exception).isNotNull();
    }

    @Test
    void customExportOrders_buildsWorkbookForEmptyOrderResult() {
        when(orderMainMapper.selectList(any())).thenReturn(List.of());
        OrderCustomExportDTO dto = new OrderCustomExportDTO();
        dto.setExportFields(List.of("orderCode"));
        MockHttpServletResponse output = new MockHttpServletResponse();

        service.customExportOrders(dto, output);

        assertThat(output.getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(output.getContentAsByteArray()).isNotEmpty();
        verify(orderQueryHelper).fillRebuildProjectList(any());
    }

    @Test
    void customExportOrders_appliesCurrentUserDataScope() {
        when(orderQueryHelper.getCurrentUserId()).thenReturn(11L);
        when(userHospitalService.getDataScopeType(11L)).thenReturn(DataScopeTypeEnum.ORG);
        when(orderMainMapper.selectList(any())).thenReturn(List.of());

        OrderCustomExportDTO dto = new OrderCustomExportDTO();
        dto.setExportFields(List.of("orderCode"));

        service.customExportOrders(dto, new MockHttpServletResponse());

        verify(orderQueryHelper).buildDataScopeCondition(any(), eq(11L), eq(DataScopeTypeEnum.ORG));
    }
}
