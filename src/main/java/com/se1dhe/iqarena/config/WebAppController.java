package com.se1dhe.iqarena.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebAppController {
    @GetMapping({"/app", "/app/"})
    public String app() {
        return "forward:/app/index.html";
    }
}

