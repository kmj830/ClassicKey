package com.example.classickey.controller;

import com.example.classickey.dto.CipherRequestDto;
import com.example.classickey.dto.CipherResultDto;
import com.example.classickey.dto.ErrorResponseDto;
import com.example.classickey.dto.TableEntryDto;
import com.example.classickey.service.CharacterTableManager;
import com.example.classickey.service.CipherCoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 고전 대칭키 암호 변환 처리를 위한 REST API 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/cipher")
@RequiredArgsConstructor
public class CipherApiController {

    private final CipherCoreService cipherCoreService;
    private final CharacterTableManager tableManager;

    /**
     * 암·복호화 요청을 처리하는 HTTP POST 엔드포인트입니다.
     *
     * @param req 암호화/복호화 요청 페이로드
     * @return 변환 결과 DTO (성공 시 200 OK, 실패 시 400 Bad Request)
     */
    @PostMapping
    public ResponseEntity<?> processCipher(@RequestBody @Valid CipherRequestDto req) {
        if (req == null) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDto("BAD_REQUEST", "요청 데이터가 존재하지 않습니다."));
        }

        String mode = req.getMode();
        if (mode == null || (!mode.equalsIgnoreCase("ENCRYPT") && !mode.equalsIgnoreCase("DECRYPT"))) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDto("INVALID_MODE", "모드는 'ENCRYPT' 또는 'DECRYPT'이어야 합니다."));
        }

        try {
            CipherResultDto result;
            if (mode.equalsIgnoreCase("ENCRYPT")) {
                result = cipherCoreService.encrypt(req.getText(), req.getKey());
            } else {
                result = cipherCoreService.decrypt(req.getText(), req.getKey());
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDto("VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 외부 파일 table.txt로부터 동적 산출된 매핑 테이블과 법(Modulus) 정보를 반환합니다.
     */
    @GetMapping("/table")
    public ResponseEntity<Map<String, Object>> getTableInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("modulus", tableManager.getModulus());
        response.put("entries", tableManager.getTableEntries());
        return ResponseEntity.ok(response);
    }
}
