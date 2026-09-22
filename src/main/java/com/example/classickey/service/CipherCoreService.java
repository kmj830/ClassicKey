package com.example.classickey.service;

import com.example.classickey.dto.CipherResultDto;
import com.example.classickey.dto.StepDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 고전 대칭키 덧셈 암호화 및 음수 보정 모듈러 복호화 핵심 도메인 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class CipherCoreService {

    private final CharacterTableManager tableManager;

    /**
     * 평문을 덧셈 암호화(Caesar Cipher)하여 암호문과 단계별 계산 과정을 반환합니다.
     * C_i = (P_i + K) mod n
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
            char pChar = text.charAt(i);
            int pCode = tableManager.getPlainCode(pChar);
            int cCode = (pCode + key) % n;
            char cChar = tableManager.getCipherChar(cCode);

            String formula = "(" + pCode + " + " + key + ") mod " + n;

            steps.add(StepDetail.builder()
                    .character(String.valueOf(pChar))
                    .code(pCode)
                    .formula(formula)
                    .resultCode(cCode)
                    .resultChar(String.valueOf(cChar))
                    .build());

            resultText.append(cChar);
        }

        return CipherResultDto.builder()
                .resultText(resultText.toString())
                .steps(steps)
                .build();
    }

    /**
     * 암호문을 복호화하여 원본 평문과 단계별 계산 과정을 반환합니다.
     * 음수 나머지 문제를 방지하기 위해 일반화 수식 ((C_i - K) % n + n) % n 을 적용합니다.
     *
     * @param text 암호문 문자열
     * @param key  비밀키 정수 (1 <= key <= n-1)
     * @return 복호화 결과 DTO (최종 복원된 평문 및 세부 단계 배열)
     */
    public CipherResultDto decrypt(String text, int key) {
        validateInputs(text, key, false);

        int n = tableManager.getModulus();
        List<StepDetail> steps = new ArrayList<>();
        StringBuilder resultText = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char cChar = text.charAt(i);
            int cCode = tableManager.getCipherCode(cChar);
            int pCode = ((cCode - key) % n + n) % n;
            char pChar = tableManager.getPlainChar(pCode);

            String formula = "((" + (cCode - key) + " % " + n + ") + " + n + ") % " + n;

            steps.add(StepDetail.builder()
                    .character(String.valueOf(cChar))
                    .code(cCode)
                    .formula(formula)
                    .resultCode(pCode)
                    .resultChar(String.valueOf(pChar))
                    .build());

            resultText.append(pChar);
        }

        return CipherResultDto.builder()
                .resultText(resultText.toString())
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
                if (!tableManager.containsPlainChar(ch)) {
                    throw new IllegalArgumentException("지원되지 않는 평문 문자입니다: '" + ch + "' (위치: " + (i + 1) + ")");
                }
            } else {
                if (!tableManager.containsCipherChar(ch)) {
                    throw new IllegalArgumentException("지원되지 않는 암호문 문자입니다: '" + ch + "' (위치: " + (i + 1) + ")");
                }
            }
        }
    }
}
