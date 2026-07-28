package com.seochang.church.controller;

import com.seochang.church.entity.Notice;
import com.seochang.church.entity.User;
import com.seochang.church.repository.NoticeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notices")
public class NoticeController {

    private final NoticeRepository noticeRepository;
    private final com.seochang.church.service.FileStorageService fileStorageService;

    public NoticeController(NoticeRepository noticeRepository, com.seochang.church.service.FileStorageService fileStorageService) {
        this.noticeRepository = noticeRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String list(@RequestParam(name = "category", required = false, defaultValue = "all") String category, 
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "keyword", required = false) String keyword,
                       Model model) {
        model.addAttribute("currentMenu", "notices");
        model.addAttribute("currentCategory", category);
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<Notice> noticePage;
        
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        
        if ("all".equals(category) || category == null || category.isEmpty()) {
            if (hasKeyword) {
                noticePage = noticeRepository.findByTitleKeyword(keyword.trim(), pageable);
            } else {
                noticePage = noticeRepository.findAll(pageable);
            }
        } else {
            if (hasKeyword) {
                noticePage = noticeRepository.findByCategoryAndTitleKeyword(category, keyword.trim(), pageable);
            } else {
                noticePage = noticeRepository.findByCategory(category, pageable);
            }
        }
        
        model.addAttribute("notices", noticePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", noticePage.getTotalPages());
        model.addAttribute("totalElements", noticePage.getTotalElements());
        model.addAttribute("keyword", keyword);
        
        return "notice_list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Notice notice = noticeRepository.findById(id).orElse(null);
        if (notice == null) {
            return "redirect:/notices";
        }
        
        // Increase view count
        notice.setViewCount(notice.getViewCount() + 1);
        noticeRepository.save(notice);
        
        model.addAttribute("notice", notice);
        return "notice_detail";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(name = "category", required = false) String category, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("error", "관리자만 접근할 수 있습니다.");
            return "redirect:/notices";
        }
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
                         HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("error", "관리자만 접근할 수 있습니다.");
            return "redirect:/notices";
        }
        
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
        
        noticeRepository.save(notice);
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
    public String editForm(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("error", "관리자만 접근할 수 있습니다.");
            return "redirect:/notices";
        }
        Notice notice = noticeRepository.findById(id).orElse(null);
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
                       HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("error", "관리자만 접근할 수 있습니다.");
            return "redirect:/notices";
        }
        Notice notice = noticeRepository.findById(id).orElse(null);
        if (notice != null) {
            notice.setTitle(updatedNotice.getTitle());
            notice.setContent(updatedNotice.getContent());
            if (updatedNotice.getCategory() != null) {
                notice.setCategory(updatedNotice.getCategory());
            }
            notice.setUpdatedAt(java.time.LocalDateTime.now());
            
            // Delete requested files
            if (deleteFileIds != null && !deleteFileIds.isEmpty()) {
                java.util.Iterator<com.seochang.church.entity.NoticeAttachment> iterator = notice.getAttachments().iterator();
                while (iterator.hasNext()) {
                    com.seochang.church.entity.NoticeAttachment attachment = iterator.next();
                    if (deleteFileIds.contains(attachment.getId())) {
                        fileStorageService.deleteFile(attachment.getStoredFileName());
                        iterator.remove();
                    }
                }
            }
            
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

            noticeRepository.save(notice);
        }
        return "redirect:/notices";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("error", "관리자만 접근할 수 있습니다.");
            return "redirect:/notices";
        }
        Notice notice = noticeRepository.findById(id).orElse(null);
        if (notice != null) {
            for (com.seochang.church.entity.NoticeAttachment attachment : notice.getAttachments()) {
                fileStorageService.deleteFile(attachment.getStoredFileName());
            }
            noticeRepository.deleteById(id);
        }
        return "redirect:/notices";
    }

    private boolean isAdmin(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        return loginUser != null && "ADMIN".equals(loginUser.getRole());
    }
}
