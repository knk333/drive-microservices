package com.drive.Drive_Service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Drive");
        return "index";   // resolves to src/main/resources/templates/index.html
    }
}