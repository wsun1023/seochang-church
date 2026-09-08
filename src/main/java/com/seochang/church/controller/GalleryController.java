package com.seochang.church.controller;

import com.seochang.church.entity.Gallery;
import com.seochang.church.entity.GalleryAttachment;
import com.seochang.church.entity.User;
import com.seochang.church.service.GalleryService;
import com.seochang.church.service.FileStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/gallery")
public class GalleryController {

    private final GalleryService galleryService;
    private final FileStorageService fileStorageService;

    public GalleryController(GalleryService galleryService, FileStorageService fileStorageService) {
        this.galleryService = galleryService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "keyword", required = false) String keyword,
                       Model model) {
        model.addAttribute("currentMenu", "gallery");
        
        Page<Gallery> galleryPage = galleryService.getActiveGalleries(page, keyword);
        
        model.addAttribute("galleries", galleryPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", galleryPage.getTotalPages());
        model.addAttribute("totalElements", galleryPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        
        return "gallery_list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        Gallery gallery = galleryService.getGallery(id);
        if (gallery == null || "Y".equals(gallery.getDelYn())) {
            return "redirect:/gallery";
        }
        galleryService.increaseViewCount(id);
        
        User loginUser = (User) session.getAttribute("loginUser");
        boolean isAdmin = loginUser != null && "ADMIN".equals(loginUser.getRole());
        
        model.addAttribute("gallery", gallery);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentMenu", "gallery");
        
        return "gallery_detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Gallery gallery = new Gallery();
        model.addAttribute("gallery", gallery);
        model.addAttribute("currentMenu", "gallery");
        return "gallery_form";
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/new")
    public String create(@ModelAttribute com.seochang.church.dto.PostForm form,
                         @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                         HttpSession session, Model model) {
        Gallery gallery = form.toGallery();
        fileStorageService.validatePlan(java.util.List.of(), null, imageFiles, null, 50, 3);
        User loginUser = (User) session.getAttribute("loginUser");
        
        long validImages = imageFiles != null ? imageFiles.stream().filter(f -> !f.isEmpty()).count() : 0;
        if (validImages == 0) {
            model.addAttribute("message", "최소 1개의 사진을 업로드해야 합니다.");
            model.addAttribute("redirectUri", "/gallery/new");
            return "alert";
        }

        
        gallery.setWriter(loginUser.getDisplayName());
        gallery.setWriterId(loginUser.getId());
        
        processAttachments(gallery, imageFiles, true);
        
        galleryService.saveGallery(gallery);
        return "redirect:/gallery";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Gallery gallery = galleryService.getGallery(id);
        if (gallery == null || "Y".equals(gallery.getDelYn())) {
            return "redirect:/gallery";
        }

        model.addAttribute("gallery", gallery);
        model.addAttribute("currentMenu", "gallery");
        return "gallery_form";
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute com.seochang.church.dto.PostForm updatedGallery,
                       @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                       Model model) {
        Gallery gallery = galleryService.getGallery(id);
        if (gallery == null || "Y".equals(gallery.getDelYn())) {
            return "redirect:/gallery";
        }

        updatedGallery.validate();
        fileStorageService.validatePlan(gallery.getAttachments(), null, imageFiles, null, 50, 3);
        gallery.setTitle(updatedGallery.getTitle());
        gallery.setContent(updatedGallery.getContent());
        gallery.setUpdatedAt(LocalDateTime.now());
        
        processAttachments(gallery, imageFiles, true);
        
        galleryService.saveGallery(gallery);
        return "redirect:/gallery/" + id;
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Gallery gallery = galleryService.getGallery(id);
        if (gallery != null) {
            galleryService.deleteGallery(id);
        }
        return "redirect:/gallery";
    }

    private void processAttachments(Gallery gallery, List<MultipartFile> files, boolean isImage) {
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String storedName = fileStorageService.store(file, "gallery");
                    GalleryAttachment attachment = new GalleryAttachment();
                    attachment.setOriginalFileName(file.getOriginalFilename());
                    attachment.setStoredFileName(storedName);
                    attachment.setFilePath("/uploads/" + storedName);
                    attachment.setFileSize(file.getSize());
                    attachment.setImage(isImage);
                    attachment.setGallery(gallery);
                    gallery.getAttachments().add(attachment);
                }
            }
        }
    }
}
