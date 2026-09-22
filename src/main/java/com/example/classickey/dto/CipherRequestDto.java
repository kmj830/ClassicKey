package com.example.classickey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CipherRequestDto {

    @NotBlank(message = "변환할 텍스트를 입력해주세요.")
    private String text;

    @NotNull(message = "키 값을 입력해주세요.")
    private Integer key;

    @NotBlank(message = "모드(ENCRYPT 또는 DECRYPT)를 입력해주세요.")
    private String mode;
}
