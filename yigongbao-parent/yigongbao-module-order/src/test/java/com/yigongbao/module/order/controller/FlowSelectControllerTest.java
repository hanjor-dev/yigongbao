package com.yigongbao.module.order.controller;

import com.yigongbao.flow.service.FlowStatusColorResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlowSelectController.class)
class FlowSelectControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private FlowStatusColorResolver flowStatusColorResolver;

    @Test
    void phases_returnsNonEmptyEnumOptions() throws Exception {
        mockMvc.perform(get("/flow/select/phases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void statuses_canFilterByPhase() throws Exception {
        mockMvc.perform(get("/flow/select/statuses").param("phase", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void statuses_marksAutoTransitionStatusesAsHidden() throws Exception {
        mockMvc.perform(get("/flow/select/statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.value == '1010')].show").value(false))
                .andExpect(jsonPath("$.data[?(@.value == '1030')].show").value(false))
                .andExpect(jsonPath("$.data[?(@.value == '1040')].show").value(false))
                .andExpect(jsonPath("$.data[?(@.value == '2010')].show").value(true))
                .andExpect(jsonPath("$.data[?(@.value == '5020')].show").value(false))
                .andExpect(jsonPath("$.data[?(@.value == '5030')].show").value(false))
                .andExpect(jsonPath("$.data[?(@.value == '5040')].show").value(false))
                .andExpect(jsonPath("$.data[?(@.value == '5050')].show").value(true));
    }
}
