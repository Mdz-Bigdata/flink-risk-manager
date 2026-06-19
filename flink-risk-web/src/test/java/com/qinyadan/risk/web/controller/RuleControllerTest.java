package com.qinyadan.risk.web.controller;

import com.qinyadan.risk.RiskWebApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(classes = RiskWebApplication.class)
@AutoConfigureMockMvc
public class RuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetRules() throws Exception {
        mockMvc.perform(get("/api/rules?page=1&pageSize=10"))
                .andDo(print());
    }
}
