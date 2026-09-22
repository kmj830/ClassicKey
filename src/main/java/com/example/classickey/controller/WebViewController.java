package com.example.classickey.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 단일 페이지 GUI 웹 리소스(HTML/CSS/JS) 라우팅을 담당하는 웹 뷰 컨트롤러입니다.
 */
@Controller
public class WebViewController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
