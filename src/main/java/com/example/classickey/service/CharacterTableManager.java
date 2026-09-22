package com.example.classickey.service;

import com.example.classickey.datastructure.CustomHashTable;
import com.example.classickey.dto.TableEntryDto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * table.txt 외부 설정 파일을 파싱하여 4개의 CustomHashTable에 적재하고
 * 평문/암호문 문자와 정수 코드 간의 양방향 O(1) 매핑을 제공하는 관리자 클래스입니다.
 */
@Slf4j
@Component
public class CharacterTableManager {

    private final CustomHashTable<Character, Integer> plainToCode = new CustomHashTable<>();
    private final CustomHashTable<Integer, Character> codeToPlain = new CustomHashTable<>();
    private final CustomHashTable<Character, Integer> cipherToCode = new CustomHashTable<>();
    private final CustomHashTable<Integer, Character> codeToCipher = new CustomHashTable<>();

    private int modulus = 0;
    private final List<TableEntryDto> tableEntries = new ArrayList<>();
    private final List<Character> allPlainCharacters = new ArrayList<>();
    private final List<Character> allCipherCharacters = new ArrayList<>();

    @PostConstruct
    public void init() throws IOException {
        loadTable("table.txt");
    }

    /**
     * table.txt 파일을 파싱하여 내부 4개의 CustomHashTable에 적재하고
     * 유한환 Modulus n 값을 동적으로 산출합니다.
     *
     * @param filePath 로드할 파일 경로 (외부 파일 경로 또는 클래스패스 파일명)
     * @throws IOException 파일 입출력 오류 발생 시
     */
    public synchronized void loadTable(String filePath) throws IOException {
        InputStream is = null;

        // 1. 외부 파일 시스템 경로 확인
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                is = new FileInputStream(file);
            }
        }

        // 2. 클래스패스 리소스에서 로드 시도
        if (is == null) {
            String resourcePath = (filePath != null && !filePath.trim().isEmpty()) ? filePath : "table.txt";
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            ClassPathResource cpr = new ClassPathResource(resourcePath);
            if (cpr.exists()) {
                is = cpr.getInputStream();
            }
        }

        if (is == null) {
            throw new FileNotFoundException("테이블 파일을 찾을 수 없습니다: " + filePath);
        }

        tableEntries.clear();
        allPlainCharacters.clear();
        allCipherCharacters.clear();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    int code = Integer.parseInt(parts[0]);
                    char plainChar = parts[1].charAt(0);
                    char cipherChar = parts[2].charAt(0);

                    plainToCode.put(plainChar, code);
                    codeToPlain.put(code, plainChar);
                    cipherToCode.put(cipherChar, code);
                    codeToCipher.put(code, cipherChar);

                    tableEntries.add(new TableEntryDto(code, String.valueOf(plainChar), String.valueOf(cipherChar)));
                    allPlainCharacters.add(plainChar);
                    allCipherCharacters.add(cipherChar);
                }
            }
        }

        this.modulus = tableEntries.size();
        log.info("Character table successfully loaded. Modulus n = {}", this.modulus);
    }

    /**
     * 평문 문자에 매핑된 정수 코드를 O(1)으로 반환합니다.
     */
    public int getPlainCode(char c) {
        Integer code = plainToCode.get(c);
        if (code == null) {
            throw new IllegalArgumentException("정의되지 않은 평문 문자입니다: '" + c + "'");
        }
        return code;
    }

    /**
     * 정수 코드에 매핑된 암호문 문자를 O(1)으로 반환합니다.
     */
    public char getCipherChar(int code) {
        Character c = codeToCipher.get(code);
        if (c == null) {
            throw new IllegalArgumentException("유효하지 않은 코드 번호입니다: " + code);
        }
        return c;
    }

    /**
     * 암호문 문자에 매핑된 정수 코드를 O(1)으로 반환합니다.
     */
    public int getCipherCode(char c) {
        Integer code = cipherToCode.get(c);
        if (code == null) {
            throw new IllegalArgumentException("정의되지 않은 암호문 문자입니다: '" + c + "'");
        }
        return code;
    }

    /**
     * 정수 코드에 매핑된 평문 문자를 O(1)으로 반환합니다.
     */
    public char getPlainChar(int code) {
        Character c = codeToPlain.get(code);
        if (c == null) {
            throw new IllegalArgumentException("유효하지 않은 코드 번호입니다: " + code);
        }
        return c;
    }

    public boolean containsPlainChar(char c) {
        return plainToCode.containsKey(c);
    }

    public boolean containsCipherChar(char c) {
        return cipherToCode.containsKey(c);
    }

    public int getModulus() {
        return modulus;
    }

    public List<TableEntryDto> getTableEntries() {
        return Collections.unmodifiableList(tableEntries);
    }

    public List<Character> getAllPlainCharacters() {
        return Collections.unmodifiableList(allPlainCharacters);
    }

    public List<Character> getAllCipherCharacters() {
        return Collections.unmodifiableList(allCipherCharacters);
    }
}
