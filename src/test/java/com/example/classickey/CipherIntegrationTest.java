package com.example.classickey;

import com.example.classickey.dto.CipherResultDto;
import com.example.classickey.dto.StepDetail;
import com.example.classickey.service.CharacterTableManager;
import com.example.classickey.service.CipherCoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CipherIntegrationTest {

    private CharacterTableManager tableManager;
    private CipherCoreService cipherService;

    @BeforeEach
    void setUp() throws IOException {
        tableManager = new CharacterTableManager();
        tableManager.init();
        cipherService = new CipherCoreService(tableManager);
    }

    @Test
    @DisplayName("통합 테스트 1: 왕복 가역성 무결성 검증 (Round-Trip Test)")
    void testRoundTripInvertibility() {
        // 1. 영대소문자 및 숫자가 무작위로 혼합된 길이 100의 평문 테스트 스트링 생성
        List<Character> allPlainChars = tableManager.getAllPlainCharacters();
        Random random = new Random(42); // 재현 가능한 난수 시드
        StringBuilder plainBuilder = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            int randomIndex = random.nextInt(allPlainChars.size());
            plainBuilder.append(allPlainChars.get(randomIndex));
        }
        String p = plainBuilder.toString();
        assertEquals(100, p.length());

        // 2. 유효 키 범위 [1, 61] 내의 모든 키 K에 대해 encrypt() -> decrypt() == p 단언
        int modulus = tableManager.getModulus();
        assertEquals(62, modulus);

        for (int k = 1; k <= modulus - 1; k++) {
            CipherResultDto encResult = cipherService.encrypt(p, k);
            String c = encResult.getResultText();
            assertNotNull(c);
            assertEquals(100, c.length());

            CipherResultDto decResult = cipherService.decrypt(c, k);
            String pPrime = decResult.getResultText();

            assertEquals(p, pPrime, "키 K=" + k + "에 대해 복호화 결과가 원본 평문과 일치해야 합니다.");
        }
    }

    @Test
    @DisplayName("통합 테스트 2: 전체 문자 공간 순환 전수 검증 (Exhaustive Domain Space Test, 3,782건)")
    void testExhaustiveDomainSpace() {
        List<Character> all62PlainCharacters = tableManager.getAllPlainCharacters();
        assertEquals(62, all62PlainCharacters.size(), "정의된 평문 문자는 총 62자여야 합니다.");

        int totalCases = 0;

        for (char p : all62PlainCharacters) {
            for (int k = 1; k <= 61; k++) {
                CipherResultDto encResult = cipherService.encrypt(String.valueOf(p), k);
                String cipherChar = encResult.getResultText();
                assertNotNull(cipherChar);
                assertEquals(1, cipherChar.length());

                // StepDetail 규격 검증
                assertEquals(1, encResult.getSteps().size());
                StepDetail encStep = encResult.getSteps().get(0);
                assertNotNull(encStep.getFormula());
                assertTrue(encStep.getFormula().contains("mod 62"));

                CipherResultDto decResult = cipherService.decrypt(cipherChar, k);
                String restoredPlain = decResult.getResultText();

                assertEquals(String.valueOf(p), restoredPlain,
                        String.format("문자 '%c', 키 %d 치환/복원 실패", p, k));

                totalCases++;
            }
        }

        // 총 62 * 61 = 3,782개의 경우의 수 전수 성공 단언
        assertEquals(3782, totalCases, "총 3,782건의 순환 반복 테스트 케이스가 전수 검증되어야 합니다.");
    }
}
