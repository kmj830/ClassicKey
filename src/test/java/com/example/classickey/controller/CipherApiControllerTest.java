package com.example.classickey.controller;

import com.example.classickey.dto.CipherRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CipherApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("API 암호화 성공 테스트")
    void testApiEncryptSuccess() throws Exception {
        String jsonPayload = """
                {
                    "text": "ClassicKey",
                    "key": 15,
                    "mode": "ENCRYPT"
                }
                """;

        mockMvc.perform(post("/api/cipher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultText", notNullValue()))
                .andExpect(jsonPath("$.steps", hasSize(10)))
                .andExpect(jsonPath("$.steps[0].char", is("C")))
                .andExpect(jsonPath("$.steps[0].code", notNullValue()))
                .andExpect(jsonPath("$.steps[0].formula", containsString("mod 62")))
                .andExpect(jsonPath("$.steps[0].resultCode", notNullValue()))
                .andExpect(jsonPath("$.steps[0].resultChar", notNullValue()));
    }

    @Test
    @DisplayName("API 복호화 성공 테스트")
    void testApiDecryptSuccess() throws Exception {
        // 'a' encrypted with key 3
        String encPayload = """
                {
                    "text": "a",
                    "key": 3,
                    "mode": "ENCRYPT"
                }
                """;

        String encResponse = mockMvc.perform(post("/api/cipher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(encPayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract cipher text from json (simple substring or regex)
        // {"resultText":"...","steps":[...]}
        int start = encResponse.indexOf("\"resultText\":\"") + 14;
        int end = encResponse.indexOf("\"", start);
        String cipherChar = encResponse.substring(start, end);

        String decPayload = String.format("""
                {
                    "text": "%s",
                    "key": 3,
                    "mode": "DECRYPT"
                }
                """, cipherChar);

        mockMvc.perform(post("/api/cipher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultText", is("a")))
                .andExpect(jsonPath("$.steps", hasSize(1)))
                .andExpect(jsonPath("$.steps[0].resultChar", is("a")));
    }

    @Test
    @DisplayName("API 키 유효성 검증 실패 (K=0) 400 에러")
    void testApiKeyUnderflowError() throws Exception {
        String jsonPayload = """
                {
                    "text": "a",
                    "key": 0,
                    "mode": "ENCRYPT"
                }
                """;

        mockMvc.perform(post("/api/cipher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("API 키 유효성 검증 실패 (K=62) 400 에러")
    void testApiKeyOverflowError() throws Exception {
        String jsonPayload = """
                {
                    "text": "a",
                    "key": 62,
                    "mode": "ENCRYPT"
                }
                """;

        mockMvc.perform(post("/api/cipher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("API 미정의 특수문자 포함 시 400 에러")
    void testApiUndefinedCharError() throws Exception {
        String jsonPayload = """
                {
                    "text": "Hello! World",
                    "key": 10,
                    "mode": "ENCRYPT"
                }
                """;

        mockMvc.perform(post("/api/cipher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("API 한글 및 띄어쓰기 교차 치환 암복호화 테스트")
    void testApiCrossSubstitutionKoreanSuccess() throws Exception {
        String jsonPayload = """
                {
                    "text": "abc 가나다",
                    "key": 15,
                    "mode": "ENCRYPT"
                }
                """;

        String encResponse = mockMvc.perform(post("/api/cipher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultText", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        int start = encResponse.indexOf("\"resultText\":\"") + 14;
        int end = encResponse.indexOf("\"", start);
        String cipherText = encResponse.substring(start, end);

        String decPayload = String.format("""
                {
                    "text": "%s",
                    "key": 15,
                    "mode": "DECRYPT"
                }
                """, cipherText);

        mockMvc.perform(post("/api/cipher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultText", is("abc 가나다")));
    }

    @Test
    @DisplayName("API 복호화 모드에서 한글 음절 '양' 자모 분해 복호화 성공 테스트")
    void testApiDecryptHangulSyllableSuccess() throws Exception {
        String jsonPayload = """
                {
                    "text": "양",
                    "key": 3,
                    "mode": "DECRYPT"
                }
                """;

        mockMvc.perform(post("/api/cipher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultText", is("isi")))
                .andExpect(jsonPath("$.steps", hasSize(3)))
                .andExpect(jsonPath("$.steps[0].char", is("양(ㅇ)")))
                .andExpect(jsonPath("$.steps[0].resultChar", is("i")))
                .andExpect(jsonPath("$.steps[1].char", is("양(ㅑ)")))
                .andExpect(jsonPath("$.steps[1].resultChar", is("s")))
                .andExpect(jsonPath("$.steps[2].char", is("양(ㅇ)")))
                .andExpect(jsonPath("$.steps[2].resultChar", is("i")));
    }

    @Test
    @DisplayName("API 테이블 정보 조회 성공")
    void testGetTableInfo() throws Exception {
        mockMvc.perform(get("/api/cipher/table"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modulus", is(62)))
                .andExpect(jsonPath("$.entries", hasSize(62)));
    }
}
