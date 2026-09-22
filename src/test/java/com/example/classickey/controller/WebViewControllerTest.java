package com.example.classickey.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GUI 메인 페이지 라우팅 테스트")
    void testIndexRouting() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}
