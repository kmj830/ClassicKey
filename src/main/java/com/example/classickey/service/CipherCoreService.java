package com.example.classickey.service;

import com.example.classickey.dto.CipherResultDto;
import com.example.classickey.dto.StepDetail;
import com.example.classickey.util.HangulUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 고전 대칭키 덧셈 암호화 및 음수 보정 모듈러 복호화 핵심 도메인 서비스입니다.
 * 영문과 한글 간의 상호 교차 치환(Cross-Substitution)과 띄어쓰기 유지를 지원합니다.
 */
@Service
@RequiredArgsConstructor
public class CipherCoreService {

    private final CharacterTableManager tableManager;

    /**
     * 평문을 덧셈 암호화(Caesar Cipher)하여 암호문과 단계별 계산 과정을 반환합니다.
     * - 영문/숫자 ➔ 한글 자모로 치환 (C_i = (P_i + K) mod n)
     * - 한글(음절/자모) ➔ 자모 분해 후 영문/숫자로 치환 (P_i = (K_i + K) mod n)
     * - 띄어쓰기(' ') ➔ 공백 유지 (Pass-through)
     *
     * @param text 평문 문자열
     * @param key  비밀키 정수 (1 <= key <= n-1)
     * @return 암호화 결과 DTO (최종 암호문 및 세부 단계 배열)
     */
    public CipherResultDto encrypt(String text, int key) {
        validateInputs(text, key, true);

        int n = tableManager.getModulus();
        List<StepDetail> steps = new ArrayList<>();
        StringBuilder resultText = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == ' ') {
                // 공백 유지 (Pass-through)
                steps.add(StepDetail.builder()
                        .character("공백")
                        .code(0)
                        .formula("공백 유지 (Pass-through)")
                        .resultCode(0)
                        .resultChar(" ")
                        .build());
                resultText.append(' ');
            } else if (tableManager.containsPlainChar(ch)) {
                // 1. 영문/숫자 -> 한글 자모로 치환
                int pCode = tableManager.getPlainCode(ch);
                int cCode = (pCode + key) % n;
                char cChar = tableManager.getCipherChar(cCode);
                String formula = "(" + pCode + " + " + key + ") mod " + n;

                steps.add(StepDetail.builder()
                        .character(String.valueOf(ch))
                        .code(pCode)
                        .formula(formula)
                        .resultCode(cCode)
                        .resultChar(String.valueOf(cChar))
                        .build());

                resultText.append(cChar);
            } else if (HangulUtils.isHangulSyllable(ch)) {
                // 2. 한글 완성형 음절 -> 초성, 중성, (종성) 분해 후 영문으로 교차 치환
                List<Character> jamos = HangulUtils.decomposeSyllable(ch);
                for (char jamo : jamos) {
                    int jCode = tableManager.getCipherCode(jamo);
                    int pCode = (jCode + key) % n;
                    char pChar = tableManager.getPlainChar(pCode);
                    String formula = "(" + jCode + " + " + key + ") mod " + n;

                    steps.add(StepDetail.builder()
                            .character(ch + "(" + jamo + ")")
                            .code(jCode)
                            .formula(formula)
                            .resultCode(pCode)
                            .resultChar(String.valueOf(pChar))
                            .build());

                    resultText.append(pChar);
                }
            } else if (tableManager.containsCipherChar(ch)) {
                // 3. 한글 자모 직접 입력 -> 영문으로 치환
                int jCode = tableManager.getCipherCode(ch);
                int pCode = (jCode + key) % n;
                char pChar = tableManager.getPlainChar(pCode);
                String formula = "(" + jCode + " + " + key + ") mod " + n;

                steps.add(StepDetail.builder()
                        .character(String.valueOf(ch))
                        .code(jCode)
                        .formula(formula)
                        .resultCode(pCode)
                        .resultChar(String.valueOf(pChar))
                        .build());

                resultText.append(pChar);
            }
        }

        return CipherResultDto.builder()
                .resultText(resultText.toString())
                .steps(steps)
                .build();
    }

    /**
     * 암호문을 복호화하여 원본 평문과 단계별 계산 과정을 반환합니다.
     * - 한글 자모 ➔ 영문/숫자로 복원
     * - 영문/숫자 ➔ 한글 자모로 복원 후 완성형 한글 음절로 자동 재조립
     * - 띄어쓰기(' ') ➔ 공백 유지
     *
     * @param text 암호문 문자열
     * @param key  비밀키 정수 (1 <= key <= n-1)
     * @return 복호화 결과 DTO (최종 복원된 평문 및 세부 단계 배열)
     */
    public CipherResultDto decrypt(String text, int key) {
        validateInputs(text, key, false);

        int n = tableManager.getModulus();
        List<StepDetail> steps = new ArrayList<>();
        List<Character> rawDecryptedChars = new ArrayList<>();

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == ' ') {
                // 공백 유지
                rawDecryptedChars.add(' ');
                steps.add(StepDetail.builder()
                        .character("공백")
                        .code(0)
                        .formula("공백 유지 (Pass-through)")
                        .resultCode(0)
                        .resultChar(" ")
                        .build());
            } else if (tableManager.containsCipherChar(ch)) {
                // 1. 한글 자모 -> 영문/숫자로 복원
                int cCode = tableManager.getCipherCode(ch);
                int pCode = ((cCode - key) % n + n) % n;
                char pChar = tableManager.getPlainChar(pCode);
                String formula = "((" + (cCode - key) + " % " + n + ") + " + n + ") % " + n;

                steps.add(StepDetail.builder()
                        .character(String.valueOf(ch))
                        .code(cCode)
                        .formula(formula)
                        .resultCode(pCode)
                        .resultChar(String.valueOf(pChar))
                        .build());

                rawDecryptedChars.add(pChar);
            } else if (tableManager.containsPlainChar(ch)) {
                // 2. 영문/숫자 -> 한글 자모로 복원
                int pCode = tableManager.getPlainCode(ch);
                int cCode = ((pCode - key) % n + n) % n;
                char jamoChar = tableManager.getCipherChar(cCode);
                String formula = "((" + (pCode - key) + " % " + n + ") + " + n + ") % " + n;

                steps.add(StepDetail.builder()
                        .character(String.valueOf(ch))
                        .code(pCode)
                        .formula(formula)
                        .resultCode(cCode)
                        .resultChar(String.valueOf(jamoChar))
                        .build());

                rawDecryptedChars.add(jamoChar);
            }
        }

        // 복호화된 자모 및 영문 문자들을 자연스러운 한글 음절로 재조립
        String assembledResult = HangulUtils.assembleJamos(rawDecryptedChars);

        return CipherResultDto.builder()
                .resultText(assembledResult)
                .steps(steps)
                .build();
    }

    /**
     * 텍스트 및 키 입력값의 유효성을 검증합니다.
     */
    private void validateInputs(String text, int key, boolean isEncrypt) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("변환할 텍스트를 입력해주세요.");
        }

        int n = tableManager.getModulus();
        if (key < 1 || key > n - 1) {
            throw new IllegalArgumentException("키 K의 유효 범위는 1 <= K <= " + (n - 1) + " 입니다. (입력값: " + key + ")");
        }

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isEncrypt) {
                boolean valid = (ch == ' ')
                        || tableManager.containsPlainChar(ch)
                        || HangulUtils.isHangulSyllable(ch)
                        || tableManager.containsCipherChar(ch);
                if (!valid) {
                    throw new IllegalArgumentException("지원되지 않는 문자입니다: '" + ch + "' (위치: " + (i + 1) + ")");
                }
            } else {
                boolean valid = (ch == ' ')
                        || tableManager.containsCipherChar(ch)
                        || tableManager.containsPlainChar(ch);
                if (!valid) {
                    throw new IllegalArgumentException("지원되지 않는 문자입니다: '" + ch + "' (위치: " + (i + 1) + ")");
                }
            }
        }
    }
}
