package com.ecommerce.monitor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReliabilityPageController {

    @GetMapping("/admin/reliability")
    public String index() {
        return "forward:/reliability/index.html";
    }
}
