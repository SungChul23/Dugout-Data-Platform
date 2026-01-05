package com.dev.dugout.domain.player.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class sd {
    @GetMapping("/")
    @ResponseBody
    String hello(){
        return "이제부터 더그아웃 시작";
    }
}
