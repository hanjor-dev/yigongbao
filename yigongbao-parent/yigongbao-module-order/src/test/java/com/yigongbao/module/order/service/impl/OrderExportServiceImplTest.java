package com.yigongbao.module.order.service.impl;

import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.order.helper.OrderQueryHelper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.dto.order.OrderExportQueryDTO;
import com.yigongbao.module.order.dto.order.OrderCustomExportDTO;
import com.yigongbao.module.order.dto.workload.DesignerWorkloadExportDTO;
import com.yigongbao.module.system.user.service.UserHospitalService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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

        assertThat(fields).extracting("field").contains("orderCode", "patientName", "projectName");
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
}
