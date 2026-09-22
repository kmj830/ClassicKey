package com.example.classickey.service;

import com.example.classickey.dto.CipherResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CrossSubstitutionTest {

    private CharacterTableManager tableManager;
    private CipherCoreService cipherService;

    @BeforeEach
    void setUp() throws IOException {
        tableManager = new CharacterTableManager();
        tableManager.init();
        cipherService = new CipherCoreService(tableManager);
    }

    @Test
    @DisplayName("영문 + 한글 + 띄어쓰기 혼용 교차 치환 및 복원 테스트 (Round-trip)")
    void testMixedCrossSubstitutionRoundTrip() {
        String original = "abc 가나다";
        int key = 15;

        // 1. 암호화: abc는 한글 자모로, 가나다는 영문으로 치환되고 공백은 유지됨
        CipherResultDto encResult = cipherService.encrypt(original, key);
        String cipherText = encResult.getResultText();

        assertNotNull(cipherText);
        assertEquals(' ', cipherText.charAt(3), "공백의 위치는 그대로 유지되어야 합니다.");

        // 앞부분은 한글 자모, 뒷부분은 영문이어야 함
        char firstChar = cipherText.charAt(0);
        assertTrue(tableManager.containsCipherChar(firstChar), "영문 'a'는 한글 자모로 치환되어야 합니다.");

        char fourthChar = cipherText.charAt(4);
        assertTrue(tableManager.containsPlainChar(fourthChar), "한글 '가'는 영문/숫자로 치환되어야 합니다.");

        // 2. 복호화: 다시 완벽하게 "abc 가나다"로 복원
        CipherResultDto decResult = cipherService.decrypt(cipherText, key);
        assertEquals(original, decResult.getResultText(), "복호화 결과가 원본과 일치해야 합니다.");
    }

    @Test
    @DisplayName("복합 한글 음절(종성, 받침 포함)과 영문 혼용 문장 전수 키 검증")
    void testComplexKoreanSentencesAllKeys() {
        String testText = "ClassicKey 2026 고전 암호 시스템 구현 성공";

        // 모든 유효 키 1 ~ 61에 대해 완벽한 왕복 가역성 검증
        for (int k = 1; k <= 61; k++) {
            CipherResultDto enc = cipherService.encrypt(testText, k);
            CipherResultDto dec = cipherService.decrypt(enc.getResultText(), k);
            assertEquals(testText, dec.getResultText(), "키 K=" + k + "에서 원본과 일치해야 합니다.");
        }
    }

    @Test
    @DisplayName("다양한 종성 받침(단자음, 복자음) 한글 단어 가역성 검증")
    void testKoreanSyllableStructures() {
        String[] samples = {
                "가나다라마바사아자차카타파하",
                "강냉이 달빛 별빛 꽃잎 맑음",
                "닭 흙 넓 밟 값 넋 앉 잃",
                "Hello World 안녕하세요 2026"
        };

        for (String sample : samples) {
            CipherResultDto enc = cipherService.encrypt(sample, 7);
            CipherResultDto dec = cipherService.decrypt(enc.getResultText(), 7);
            assertEquals(sample, dec.getResultText());
        }
    }

    @Test
    @DisplayName("복호화 모드에서 한글 완성형 음절('양' 등) 직접 입력 시 자모 분해 및 복호화 검증")
    void testDecryptHangulSyllablesDirectly() {
        // '양' -> 초성 'ㅇ'(11), 중성 'ㅑ'(21), 종성 'ㅇ'(11)
        // Key 3 복호화:
        // (11 - 3) = 8 -> 'i'
        // (21 - 3) = 18 -> 's'
        // (11 - 3) = 8 -> 'i'
        CipherResultDto result = cipherService.decrypt("양", 3);
        assertNotNull(result);
        assertEquals("isi", result.getResultText(), "'양'을 키 3으로 복호화하면 'isi'가 되어야 합니다.");
        assertEquals(3, result.getSteps().size(), "3개의 자모 분해 단계가 생성되어야 합니다.");
        assertEquals("양(ㅇ)", result.getSteps().get(0).getCharacter());
        assertEquals("i", result.getSteps().get(0).getResultChar());
        assertEquals("양(ㅑ)", result.getSteps().get(1).getCharacter());
        assertEquals("s", result.getSteps().get(1).getResultChar());
        assertEquals("양(ㅇ)", result.getSteps().get(2).getCharacter());
        assertEquals("i", result.getSteps().get(2).getResultChar());

        // 대칭성 검증: 'isi'를 키 3으로 암호화하면 'ㅇ', 'ㅑ', 'ㅇ'이 산출됨
        CipherResultDto encResult = cipherService.encrypt("isi", 3);
        assertEquals("ㅇㅑㅇ", encResult.getResultText());
    }
}
