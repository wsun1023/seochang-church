package com.seochang.church.controller;

import com.seochang.church.entity.Board;
import com.seochang.church.entity.User;
import com.seochang.church.service.BoardService;
import com.seochang.church.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.seochang.church.entity.Banner;
import com.seochang.church.entity.Gallery;
import com.seochang.church.entity.Notice;
import com.seochang.church.service.NoticeService;
import com.seochang.church.service.GalleryService;
import com.seochang.church.service.BannerService;
import com.seochang.church.service.FileStorageService;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final BoardService boardService;
    private final GalleryService galleryService;
    private final BannerService bannerService;
    private final NoticeService noticeService;
    private final FileStorageService fileStorageService;

    public AdminController(UserService userService, BoardService boardService, 
                          GalleryService galleryService, BannerService bannerService,
                          NoticeService noticeService, FileStorageService fileStorageService) {
        this.userService = userService;
        this.boardService = boardService;
        this.galleryService = galleryService;
        this.bannerService = bannerService;
        this.noticeService = noticeService;
        this.fileStorageService = fileStorageService;
    }

    @ModelAttribute
    public void addAttributes(HttpServletRequest request, Model model) {
        model.addAttribute("requestURI", request.getRequestURI());
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.getTotalUserCount());
        model.addAttribute("pendingUsers", userService.getPendingApprovalCount());
        model.addAttribute("totalBoards", boardService.getTotalBoardCount());
        model.addAttribute("totalNotices", noticeService.getTotalNoticeCount());
        model.addAttribute("totalGalleries", galleryService.getTotalGalleryCount());
        
        PageRequest pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        model.addAttribute("recentUsers", userService.getAllUsers(pageable).getContent());
        
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "keyword", required = false) String keyword,
                        Model model) {
        Page<User> userPage;
        PageRequest pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            userPage = userService.searchUsers(keyword, pageable);
        } else {
            userPage = userService.getAllUsers(pageable);
        }
        
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        
        return "admin/users";
    }

    @PostMapping("/users/{id}/approve")
    public String approveUser(@PathVariable("id") Long id) {
        userService.approveUser(id);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeUserRole(@PathVariable("id") Long id, @RequestParam("role") String role) {
        userService.changeUserRole(id, role);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }

    @GetMapping("/boards")
    public String boards(@RequestParam(value = "page", defaultValue = "0") int page,
                         @RequestParam(value = "keyword", required = false) String keyword,
                         Model model) {
        Page<Board> boardPage = boardService.getAdminBoards(page, keyword);
        
        model.addAttribute("boards", boardPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", boardPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        
        return "admin/boards";
    }

    @PostMapping("/boards/{id}/delete")
    public String deleteBoard(@PathVariable("id") Long id) {
        boardService.deleteBoard(id);
        return "redirect:/admin/boards";
    }

    @GetMapping("/notices")
    public String notices(@RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "keyword", required = false) String keyword,
                         Model model) {
        Page<Notice> noticePage = noticeService.getNoticesForAdmin(keyword, page);
        
        model.addAttribute("notices", noticePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", noticePage.getTotalPages());
        model.addAttribute("keyword", keyword);
        
        return "admin/notices";
    }

    @PostMapping("/notices/{id}/delete")
    public String deleteNotice(@PathVariable("id") Long id) {
        noticeService.deleteNotice(id);
        return "redirect:/admin/notices";
    }

    @GetMapping("/galleries")
    public String galleries(@RequestParam(value = "page", defaultValue = "0") int page,
                         Model model) {
        PageRequest pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        Page<Gallery> galleryPage = galleryService.getAllGalleries(pageable);
        
        model.addAttribute("galleries", galleryPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", galleryPage.getTotalPages());
        
        return "admin/galleries";
    }

    @PostMapping("/galleries/{id}/delete")
    public String deleteGallery(@PathVariable("id") Long id) {
        galleryService.deleteGallery(id);
        return "redirect:/admin/galleries";
    }

    @GetMapping("/banners")
    public String banners(Model model) {
        model.addAttribute("banners", bannerService.getAllBanners());
        return "admin/banners";
    }

    @PostMapping("/banners/add")
    public String addBanner(@ModelAttribute Banner banner, @RequestParam("imageFile") MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            String savedPath = fileStorageService.store(imageFile, "banner");
            // prepend /uploads/ to the savedPath so it can be accessed from frontend
            banner.setImageUrl("/uploads/" + savedPath.replace("\\", "/"));
        }
        bannerService.saveBanner(banner);
        return "redirect:/admin/banners";
    }

    @PostMapping("/banners/{id}/toggle")
    public String toggleBanner(@PathVariable("id") Long id) {
        bannerService.toggleActive(id);
        return "redirect:/admin/banners";
    }

    @PostMapping("/banners/{id}/delete")
    public String deleteBanner(@PathVariable("id") Long id) {
        bannerService.deleteBanner(id);
        return "redirect:/admin/banners";
    }

    @PostMapping("/banners/{id}/edit")
    public String editBanner(@PathVariable("id") Long id,
                             @ModelAttribute Banner banner,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) throws IOException {
        bannerService.updateBanner(id, banner, imageFile);
        return "redirect:/admin/banners";
    }
}
