package com.seochang.church.controller;

import com.seochang.church.dto.DailyMassDto;
import com.seochang.church.service.DailyMassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DailyMassController {

    private final DailyMassService dailyMassService;

    @Autowired
    public DailyMassController(DailyMassService dailyMassService) {
        this.dailyMassService = dailyMassService;
    }

    @GetMapping("/daily-mass")
    public String getDailyMass(@RequestParam(required = false) String date, Model model) {
        model.addAttribute("currentMenu", "daily-mass"); // Change from mass so the Mass Guide tab isn't highlighted
        DailyMassDto dailyMass = dailyMassService.getDailyMass(date);
        model.addAttribute("dailyMass", dailyMass);
        return "daily_mass";
    }
}
