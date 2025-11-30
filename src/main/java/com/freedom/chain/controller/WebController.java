package com.freedom.chain.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @description: Web页面控制器
 * @author: freedom
 * @create: 2025-11-28
 **/
@Controller
public class WebController {

    /**
     * 首页
     * @return
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
