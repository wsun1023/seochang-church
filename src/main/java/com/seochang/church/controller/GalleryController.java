package com.seochang.church.controller;

import com.seochang.church.entity.Gallery;
import com.seochang.church.entity.User;
import com.seochang.church.service.GalleryService;
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
    private final com.seochang.church.service.GalleryPhotoService galleryPhotos;

    public GalleryController(GalleryService galleryService, com.seochang.church.service.GalleryPhotoService galleryPhotos) {
        this.galleryService = galleryService;
        this.galleryPhotos = galleryPhotos;
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
                         @RequestParam(value = "photoOrder", required = false) List<String> photoOrder,
                         @RequestParam(value = "coverPhoto", required = false) String coverPhoto,
                         HttpSession session, Model model) {
        Gallery gallery = form.toGallery();
        User loginUser = (User) session.getAttribute("loginUser");
        
        gallery.setWriter(loginUser.getDisplayName());
        gallery.setWriterId(loginUser.getId());
        
        galleryPhotos.update(gallery, imageFiles, null, photoOrder, coverPhoto);
        
        galleryService.saveGallery(gallery);
        return "redirect:/gallery/" + gallery.getId();
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
                       @RequestParam(value = "deleteFileIds", required = false) List<Long> deleteFileIds,
                       @RequestParam(value = "photoOrder", required = false) List<String> photoOrder,
                       @RequestParam(value = "coverPhoto", required = false) String coverPhoto,
                       Model model) {
        Gallery gallery = galleryService.getGallery(id);
        if (gallery == null || "Y".equals(gallery.getDelYn())) {
            return "redirect:/gallery";
        }

        updatedGallery.validate();
        galleryPhotos.update(gallery, imageFiles, deleteFileIds, photoOrder, coverPhoto);
        gallery.setTitle(updatedGallery.getTitle());
        gallery.setContent(updatedGallery.getContent());
        gallery.setUpdatedAt(LocalDateTime.now());
        
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

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> photoError(
            org.springframework.web.server.ResponseStatusException exception) {
        return org.springframework.http.ResponseEntity.status(exception.getStatusCode())
                .body(java.util.Map.of("message", exception.getReason() == null ? "사진첩을 저장할 수 없습니다." : exception.getReason()));
    }
}
