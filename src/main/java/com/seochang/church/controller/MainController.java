package com.seochang.church.controller;

import com.seochang.church.repository.NoticeRepository;
import com.seochang.church.repository.BoardRepository;
import com.seochang.church.repository.GalleryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private GalleryRepository galleryRepository;

    @Autowired
    private com.seochang.church.service.BannerService bannerService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("currentMenu", "home");
        model.addAttribute("notices", noticeRepository.findAll());
        
        // 최신 갤러리 3건 조회
        model.addAttribute("recentGalleries", galleryRepository.findTop3ByOrderByCreatedAtDesc(PageRequest.of(0, 3)));
            
        // 배너 및 팝업 조회
        model.addAttribute("activeBanners", bannerService.getActiveBanners());
        model.addAttribute("activePopups", bannerService.getActivePopups());
        
        return "main";
    }

    @GetMapping("/mass")
    public String mass(Model model) {
        model.addAttribute("currentMenu", "mass");
        return "mass";
    }
}