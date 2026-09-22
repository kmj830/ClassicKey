package com.example.classickey.service;

import com.example.classickey.dto.CipherResultDto;
import com.example.classickey.dto.StepDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CipherCoreServiceTest {

    private CharacterTableManager tableManager;
    private CipherCoreService cipherService;

    @BeforeEach
    void setUp() throws IOException {
        tableManager = new CharacterTableManager();
        tableManager.init();
        cipherService = new CipherCoreService(tableManager);
    }

    @Test
    @DisplayName("UT-01: 기본 암호화 연산 검증")
    void testBasicEncryption() {
        // Text: "a", Key: 3
        // Code 0 -> (0 + 3) mod 62 = 3 에 매핑되는 암호문 문자 반환 일치 검증
        int expectedCode = (tableManager.getPlainCode('a') + 3) % tableManager.getModulus();
        assertEquals(3, expectedCode);
        char expectedCipherChar = tableManager.getCipherChar(expectedCode);

        CipherResultDto result = cipherService.encrypt("a", 3);

        assertEquals(String.valueOf(expectedCipherChar), result.getResultText());
        assertEquals(1, result.getSteps().size());
        StepDetail step = result.getSteps().get(0);
        assertEquals("a", step.getCharacter());
        assertEquals(0, step.getCode());
        assertEquals("(0 + 3) mod 62", step.getFormula());
        assertEquals(3, step.getResultCode());
        assertEquals(String.valueOf(expectedCipherChar), step.getResultChar());
    }

    @Test
    @DisplayName("UT-02: 기본 복호화 연산 검증")
    void testBasicDecryption() {
        // Text: Code 3의 암호문 문자, Key: 3
        // Code 3 -> (3 - 3) mod 62 = 0 에 매핑되는 원본 평문 문자 "a" 복원 검증
        char code3CipherChar = tableManager.getCipherChar(3);

        CipherResultDto result = cipherService.decrypt(String.valueOf(code3CipherChar), 3);

        assertEquals("a", result.getResultText());
        assertEquals(1, result.getSteps().size());
        StepDetail step = result.getSteps().get(0);
        assertEquals(String.valueOf(code3CipherChar), step.getCharacter());
        assertEquals(3, step.getCode());
        assertEquals(0, step.getResultCode());
        assertEquals("a", step.getResultChar());
    }

    @Test
    @DisplayName("UT-03: 음수 나머지 보정 검증")
    void testNegativeModuloCorrection() {
        // Text: Code 2의 암호문 문자, Key: 15
        // (2 - 15) = -13 -> ((-13 % 62) + 62) % 62 = 49 에 해당하는 평문 문자 정확 반환
        char code2CipherChar = tableManager.getCipherChar(2);
        char expectedPlainChar49 = tableManager.getPlainChar(49);

        CipherResultDto result = cipherService.decrypt(String.valueOf(code2CipherChar), 15);

        assertEquals(String.valueOf(expectedPlainChar49), result.getResultText());
        assertEquals(1, result.getSteps().size());
        StepDetail step = result.getSteps().get(0);
        assertEquals(49, step.getResultCode());
        assertEquals(String.valueOf(expectedPlainChar49), step.getResultChar());
    }

    @Test
    @DisplayName("UT-04: 암호화 오버플로우 랩어라운드")
    void testEncryptionOverflowWrapAround() {
        // Text: Code 61의 문자, Key: 5
        // (61 + 5) mod 62 = 4 에 해당하는 암호문 문자 반환 검증
        char code61PlainChar = tableManager.getPlainChar(61);
        char expectedCipherChar4 = tableManager.getCipherChar(4);

        CipherResultDto result = cipherService.encrypt(String.valueOf(code61PlainChar), 5);

        assertEquals(String.valueOf(expectedCipherChar4), result.getResultText());
        assertEquals(1, result.getSteps().size());
        StepDetail step = result.getSteps().get(0);
        assertEquals(61, step.getCode());
        assertEquals(4, step.getResultCode());
        assertEquals(String.valueOf(expectedCipherChar4), step.getResultChar());
    }

    @Test
    @DisplayName("UT-05: 키 최소 경계값 검증")
    void testKeyMinimumBoundary() {
        // Text: "a", Key: 1 -> 유효 범위 최솟값으로 정상 암호화 수행 완료 확인
        int expectedCode = (0 + 1) % 62;
        char expectedCipher = tableManager.getCipherChar(expectedCode);

        CipherResultDto result = cipherService.encrypt("a", 1);

        assertEquals(String.valueOf(expectedCipher), result.getResultText());
    }

    @Test
    @DisplayName("UT-06: 키 최대 경계값 검증")
    void testKeyMaximumBoundary() {
        // Text: "a", Key: 61 -> 유효 범위 최댓값(n-1)으로 정상 암호화 수행 완료 확인
        int expectedCode = (0 + 61) % 62;
        char expectedCipher = tableManager.getCipherChar(expectedCode);

        CipherResultDto result = cipherService.encrypt("a", 61);

        assertEquals(String.valueOf(expectedCipher), result.getResultText());
    }

    @Test
    @DisplayName("UT-07: 키 유효 범위 미달 예외")
    void testKeyUnderflowException() {
        // Text: "a", Key: 0 -> IllegalArgumentException 발생 확인 (키=0 불허)
        assertThrows(IllegalArgumentException.class, () -> {
            cipherService.encrypt("a", 0);
        });
    }

    @Test
    @DisplayName("UT-08: 키 유효 범위 초과 예외")
    void testKeyOverflowException() {
        // Text: "a", Key: 62 -> IllegalArgumentException 발생 확인 (키 >= n 불허)
        assertThrows(IllegalArgumentException.class, () -> {
            cipherService.encrypt("a", 62);
        });
    }

    @Test
    @DisplayName("UT-09: 미정의 특수문자 입력 예외")
    void testUndefinedCharacterException() {
        // Text: "Hello!", Key: 10 -> 미지원 문자 '!' 검출 시 IllegalArgumentException 정상 스로우 확인
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            cipherService.encrypt("Hello!", 10);
        });
        assertTrue(exception.getMessage().contains("!"));
    }
}
