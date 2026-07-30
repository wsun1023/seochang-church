package com.seochang.church.controller;

import com.seochang.church.dto.DailyMissaDto;
import com.seochang.church.service.DailyMissaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DailyMissaController {

    private final DailyMissaService dailyMissaService;

    @Autowired
    public DailyMissaController(DailyMissaService dailyMissaService) {
        this.dailyMissaService = dailyMissaService;
    }

    @GetMapping("/daily-missa")
    public String getDailyMissa(@RequestParam(required = false) String date, Model model) {
        model.addAttribute("currentMenu", "daily-missa"); // Change from missa so the Missa Guide tab isn't highlighted
        DailyMissaDto dailyMissa = dailyMissaService.getDailyMissa(date);
        model.addAttribute("dailyMissa", dailyMissa);
        return "daily_missa";
    }
}
