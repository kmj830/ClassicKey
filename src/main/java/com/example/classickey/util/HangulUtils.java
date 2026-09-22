package com.example.classickey.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 한글 음절(가~힣)의 초성, 중성, 종성 분해 및 자모 재조립 유틸리티 클래스입니다.
 */
public class HangulUtils {

    public static final char SYLLABLE_START = 0xAC00; // '가'
    public static final char SYLLABLE_END = 0xD7A3;   // '힣'

    public static final char[] CHOSEONG = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    public static final char[] JUNGSEONG = {
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
            'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
    };

    public static final char[] JONGSEONG = {
            '\0', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
            'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    /**
     * 완성형 한글 음절('가'~'힣') 여부 확인
     */
    public static boolean isHangulSyllable(char c) {
        return c >= SYLLABLE_START && c <= SYLLABLE_END;
    }

    /**
     * 한글 자모 여부 확인
     */
    public static boolean isHangulJamo(char c) {
        return (c >= 0x3131 && c <= 0x318E) || (c >= 0x1100 && c <= 0x11FF);
    }

    public static int getChoseongIndex(char c) {
        for (int i = 0; i < CHOSEONG.length; i++) {
            if (CHOSEONG[i] == c) return i;
        }
        return -1;
    }

    public static int getJungseongIndex(char c) {
        for (int i = 0; i < JUNGSEONG.length; i++) {
            if (JUNGSEONG[i] == c) return i;
        }
        return -1;
    }

    public static int getJongseongIndex(char c) {
        for (int i = 1; i < JONGSEONG.length; i++) {
            if (JONGSEONG[i] == c) return i;
        }
        return -1;
    }

    /**
     * 완성형 한글 음절을 초성, 중성, (종성) 자모 목록으로 분해합니다.
     */
    public static List<Character> decomposeSyllable(char c) {
        List<Character> jamos = new ArrayList<>();
        if (!isHangulSyllable(c)) {
            jamos.add(c);
            return jamos;
        }

        int syllableIndex = c - SYLLABLE_START;
        int choIndex = syllableIndex / (21 * 28);
        int jungIndex = (syllableIndex % (21 * 28)) / 28;
        int jongIndex = syllableIndex % 28;

        jamos.add(CHOSEONG[choIndex]);
        jamos.add(JUNGSEONG[jungIndex]);
        if (jongIndex > 0) {
            jamos.add(JONGSEONG[jongIndex]);
        }

        return jamos;
    }

    /**
     * 복호화된 문자 스트림(영어, 공백, 한글 자모 등)에서 연속된 한글 자모를
     * 온전한 한글 완성형 음절로 자동 재조립합니다.
     */
    public static String assembleJamos(List<Character> chars) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        int n = chars.size();

        while (i < n) {
            char current = chars.get(i);
            int choIndex = getChoseongIndex(current);

            // 1. 현재 문자가 초성이고, 뒤에 중성(모음)이 따라오는 경우 음절 합성 시작
            if (choIndex != -1 && (i + 1 < n) && getJungseongIndex(chars.get(i + 1)) != -1) {
                int jungIndex = getJungseongIndex(chars.get(i + 1));
                int jongIndex = 0;
                int consumed = 2;

                // 종성 후보 확인
                if (i + 2 < n) {
                    char nextNext = chars.get(i + 2);
                    int potentialJong = getJongseongIndex(nextNext);

                    if (potentialJong != -1) {
                        // 만약 그 뒤(i+3)에 또 모음이 온다면, nextNext는 다음 글자의 초성이어야 함!
                        boolean nextIsVowel = (i + 3 < n) && (getJungseongIndex(chars.get(i + 3)) != -1);
                        if (!nextIsVowel) {
                            jongIndex = potentialJong;
                            consumed = 3;
                        }
                    }
                }

                char assembled = (char) (SYLLABLE_START + (choIndex * 21 + jungIndex) * 28 + jongIndex);
                result.append(assembled);
                i += consumed;
            } else {
                result.append(current);
                i++;
            }
        }

        return result.toString();
    }
}
