package com.seochang.church.controller;

import com.seochang.church.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @Autowired
    private NoticeRepository noticeRepository;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("currentMenu", "home");
        model.addAttribute("notices", noticeRepository.findAll());
        return "main";
    }

    @GetMapping("/mass")
    public String mass(Model model) {
        model.addAttribute("currentMenu", "mass");
        return "mass";
    }
}