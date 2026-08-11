package com.seochang.church.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CalendarController {

    @GetMapping("/calendar")
    public String calendar(Model model) {
        model.addAttribute("requestURI", "/calendar");
        return "calendar";
    }
}
