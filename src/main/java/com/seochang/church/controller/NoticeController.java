package com.seochang.church.controller;

import com.seochang.church.entity.Notice;
import com.seochang.church.entity.User;
import com.seochang.church.service.NoticeService;
import com.seochang.church.service.FileStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notices")
public class NoticeController {

    private final NoticeService noticeService;
    private final FileStorageService fileStorageService;

    public NoticeController(NoticeService noticeService, FileStorageService fileStorageService) {
        this.noticeService = noticeService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String list(@RequestParam(name = "category", required = false, defaultValue = "all") String category, 
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "keyword", required = false) String keyword,
                       Model model) {
        model.addAttribute("currentMenu", "notices");
        model.addAttribute("currentCategory", category);
        
        org.springframework.data.domain.Page<Notice> noticePage = noticeService.getNotices(category, page, keyword);
        
        model.addAttribute("notices", noticePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", noticePage.getTotalPages());
        model.addAttribute("totalElements", noticePage.getTotalElements());
        model.addAttribute("keyword", keyword);
        
        return "notice_list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Notice notice = noticeService.getNoticeAndIncreaseViewCount(id);
        if (notice == null) {
            return "redirect:/notices";
        }
        model.addAttribute("notice", notice);
        return "notice_detail";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(name = "category", required = false) String category, Model model) {
        Notice notice = new Notice();
        if (category != null && !category.isEmpty()) {
            notice.setCategory(category);
        }
        model.addAttribute("notice", notice);
        return "notice_form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute Notice notice, 
                         @RequestParam(value = "imageFiles", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> imageFiles,
                         @RequestParam(value = "generalFiles", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> generalFiles,
                         RedirectAttributes redirectAttributes) {
        
        long validImages = imageFiles != null ? imageFiles.stream().filter(f -> !f.isEmpty()).count() : 0;
        long validFiles = generalFiles != null ? generalFiles.stream().filter(f -> !f.isEmpty()).count() : 0;
        
        if (validImages > 10) {
            redirectAttributes.addFlashAttribute("error", "이미지는 최대 10개까지 업로드할 수 있습니다.");
            return "redirect:/notices/new";
        }
        if (validFiles > 3) {
            redirectAttributes.addFlashAttribute("error", "파일은 최대 3개까지 업로드할 수 있습니다.");
            return "redirect:/notices/new";
        }
        
        processAttachments(notice, imageFiles, true);
        processAttachments(notice, generalFiles, false);
        
        noticeService.saveNotice(notice);
        return "redirect:/notices";
    }
    
    private void processAttachments(Notice notice, java.util.List<org.springframework.web.multipart.MultipartFile> files, boolean isImage) {
        if (files != null) {
            for (org.springframework.web.multipart.MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String storedName = fileStorageService.store(file, "notice");
                    com.seochang.church.entity.NoticeAttachment attachment = new com.seochang.church.entity.NoticeAttachment();
                    attachment.setOriginalFileName(file.getOriginalFilename());
                    attachment.setStoredFileName(storedName);
                    attachment.setFilePath("/uploads/" + storedName);
                    attachment.setFileSize(file.getSize());
                    attachment.setImage(isImage);
                    attachment.setNotice(notice);
                    notice.getAttachments().add(attachment);
                }
            }
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Notice notice = noticeService.getNotice(id);
        if (notice == null) {
            return "redirect:/notices";
        }
        model.addAttribute("notice", notice);
        return "notice_form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute Notice updatedNotice, 
                       @RequestParam(value = "imageFiles", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> imageFiles,
                       @RequestParam(value = "generalFiles", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> generalFiles,
                       @RequestParam(value = "deleteFileIds", required = false) java.util.List<Long> deleteFileIds,
                       RedirectAttributes redirectAttributes) {
        Notice notice = noticeService.getNotice(id);
        if (notice != null) {
            notice.setTitle(updatedNotice.getTitle());
            notice.setContent(updatedNotice.getContent());
            if (updatedNotice.getCategory() != null) {
                notice.setCategory(updatedNotice.getCategory());
            }
            notice.setUpdatedAt(java.time.LocalDateTime.now());
            
            // Delete requested files
            fileStorageService.deleteAttachments(deleteFileIds, notice.getAttachments());
            
            // Add new files
            processAttachments(notice, imageFiles, true);
            processAttachments(notice, generalFiles, false);
            
            long validImages = notice.getAttachments().stream().filter(a -> a.isImage()).count();
            long validFiles = notice.getAttachments().stream().filter(a -> !a.isImage()).count();
            
            if (validImages > 10) {
                redirectAttributes.addFlashAttribute("error", "이미지는 최대 10개까지 업로드할 수 있습니다.");
                return "redirect:/notices/" + id + "/edit";
            }
            if (validFiles > 3) {
                redirectAttributes.addFlashAttribute("error", "파일은 최대 3개까지 업로드할 수 있습니다.");
                return "redirect:/notices/" + id + "/edit";
            }

            noticeService.saveNotice(notice);
        }
        return "redirect:/notices";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Notice notice = noticeService.getNotice(id);
        if (notice != null) {
            for (com.seochang.church.entity.NoticeAttachment attachment : notice.getAttachments()) {
                fileStorageService.deleteFile(attachment.getStoredFileName());
            }
            noticeService.deleteNotice(id);
        }
        return "redirect:/notices";
    }
}
