package com.seochang.church.controller;

import com.seochang.church.service.NoticeService;
import com.seochang.church.service.GalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private GalleryService galleryService;

    @Autowired
    private com.seochang.church.service.BannerService bannerService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("currentMenu", "home");
        model.addAttribute("notices", noticeService.getRecentNotices(3));
        
        // 최신 갤러리 3건 조회
        model.addAttribute("recentGalleries", galleryService.getRecentGalleries(3));
            
        // 배너 및 팝업 조회
        model.addAttribute("activeBanners", bannerService.getActiveBanners());
        model.addAttribute("activePopups", bannerService.getActivePopups());
        
        return "main";
    }

    @GetMapping("/missa")
    public String missa(Model model) {
        model.addAttribute("currentMenu", "missa");
        return "missa";
    }

    @GetMapping("/bible")
    public String bible(Model model) {
        model.addAttribute("currentMenu", "bible");
        return "bible";
    }
}