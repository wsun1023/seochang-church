package com.seochang.church.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AboutController {

    @GetMapping("/about")
    public String about(@RequestParam(name = "tab", defaultValue = "overview") String tab, Model model) {
        model.addAttribute("currentMenu", "about");
        model.addAttribute("activeTab", tab);
        return "about";
    }
}
