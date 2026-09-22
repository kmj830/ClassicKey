package com.example.classickey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableEntryDto {
    private int code;
    private String plainChar;
    private String cipherChar;
}
