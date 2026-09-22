package com.example.classickey.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepDetail {

    @JsonProperty("char")
    private String character;

    private int code;

    private String formula;

    private int resultCode;

    private String resultChar;
}
