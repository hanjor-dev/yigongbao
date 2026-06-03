package com.yigongbao.module.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.order.dto.ClassicCaseQueryDTO;
import com.yigongbao.module.order.dto.MarkClassicCaseDTO;
import com.yigongbao.module.order.service.IOrderClassicCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderClassicCaseController.class)
class OrderClassicCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IOrderClassicCaseService classicCaseService;

    @Test
    void markAsClassicCase_success() throws Exception {
        MarkClassicCaseDTO dto = new MarkClassicCaseDTO();
        dto.setOrderId(1L);
        dto.setRemark("优秀案例");

        doNothing().when(classicCaseService).markAsClassicCase(any());

        mockMvc.perform(post("/order/classic-case/mark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void list_success() throws Exception {
        ClassicCaseQueryDTO dto = new ClassicCaseQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        mockMvc.perform(post("/order/classic-case/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void detail_success() throws Exception {
        mockMvc.perform(get("/order/classic-case/{orderId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
