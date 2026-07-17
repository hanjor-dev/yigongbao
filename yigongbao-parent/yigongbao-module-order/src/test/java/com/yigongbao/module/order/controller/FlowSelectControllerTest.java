package com.yigongbao.module.order.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlowSelectController.class)
class FlowSelectControllerTest {
    @Autowired private MockMvc mockMvc;

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
}
