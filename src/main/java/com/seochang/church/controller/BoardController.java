package com.seochang.church.controller;

import com.seochang.church.entity.Board;
import com.seochang.church.entity.User;
import com.seochang.church.service.BoardService;
import com.seochang.church.service.UserService;
import com.seochang.church.repository.BoardLikeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;
    private final com.seochang.church.service.FileStorageService fileStorageService;
    private final BoardLikeRepository boardLikeRepository;

    public BoardController(BoardService boardService, com.seochang.church.service.FileStorageService fileStorageService, BoardLikeRepository boardLikeRepository) {
        this.boardService = boardService;
        this.fileStorageService = fileStorageService;
        this.boardLikeRepository = boardLikeRepository;
    }

    @GetMapping
    public String list(@RequestParam(name = "category", required = false, defaultValue = "free") String category, 
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "keyword", required = false) String keyword,
                       Model model) {
        model.addAttribute("currentMenu", "boards");
        model.addAttribute("currentCategory", category);
        
        org.springframework.data.domain.Page<Board> boardPage = boardService.getActiveBoardsByCategory(category, page, keyword);
        
        model.addAttribute("boards", boardPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", boardPage.getTotalPages());
        model.addAttribute("totalElements", boardPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        
        return "board_list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");

        Board board = boardService.getBoard(id);
        if (board == null || "Y".equals(board.getDelYn())) {
            return "redirect:/boards";
        }
        boardService.increaseViewCount(id);
        
        boolean isOwnerOrAdmin = false;
        boolean isLiked = false;
        
        if (loginUser != null) {
            isOwnerOrAdmin = board.getWriterId().equals(loginUser.getId()) || "ADMIN".equals(loginUser.getRole());
            isLiked = boardLikeRepository.existsByBoardAndUser(board, loginUser);
        }
        
        model.addAttribute("board", board);
        model.addAttribute("isOwnerOrAdmin", isOwnerOrAdmin);
        model.addAttribute("isLiked", isLiked);
        
        return "board_detail";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(name = "category", required = false) String category, Model model) {
        Board board = new Board();
        if (category != null && !category.isEmpty()) {
            board.setCategory(category);
        }
        model.addAttribute("board", board);
        return "board_form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute Board board, 
                         @RequestParam(value = "imageFiles", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> imageFiles,
                         @RequestParam(value = "generalFiles", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> generalFiles,
                         HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        
        long validImages = imageFiles != null ? imageFiles.stream().filter(f -> !f.isEmpty()).count() : 0;
        long validFiles = generalFiles != null ? generalFiles.stream().filter(f -> !f.isEmpty()).count() : 0;
        
        if (validImages > 10) {
            model.addAttribute("message", "이미지는 최대 10개까지 업로드할 수 있습니다.");
            model.addAttribute("redirectUri", "/boards/new");
            return "alert";
        }
        if (validFiles > 3) {
            model.addAttribute("message", "파일은 최대 3개까지 업로드할 수 있습니다.");
            model.addAttribute("redirectUri", "/boards/new");
            return "alert";
        }
        
        board.setWriter(loginUser.getDisplayName());
        board.setWriterId(loginUser.getId());
        
        processAttachments(board, imageFiles, true);
        processAttachments(board, generalFiles, false);
        
        boardService.saveBoard(board);
        return "redirect:/boards";
    }
    
    private void processAttachments(Board board, java.util.List<org.springframework.web.multipart.MultipartFile> files, boolean isImage) {
        if (files != null) {
            for (org.springframework.web.multipart.MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String storedName = fileStorageService.store(file, "board");
                    com.seochang.church.entity.BoardAttachment attachment = new com.seochang.church.entity.BoardAttachment();
                    attachment.setOriginalFileName(file.getOriginalFilename());
                    attachment.setStoredFileName(storedName);
                    attachment.setFilePath("/uploads/" + storedName);
                    attachment.setFileSize(file.getSize());
                    attachment.setImage(isImage);
                    attachment.setBoard(board);
                    board.getAttachments().add(attachment);
                }
            }
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");

        Board board = boardService.getBoard(id);
        if (board == null || "Y".equals(board.getDelYn())) {
            return "redirect:/boards";
        }

        boolean isAdmin = "ADMIN".equals(loginUser.getRole());

        if (!board.getWriterId().equals(loginUser.getId()) && !isAdmin) {
            model.addAttribute("message", "수정 권한이 없습니다.");
            model.addAttribute("redirectUri", "/boards/" + id);
            return "alert";
        }

        model.addAttribute("board", board);
        return "board_form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute Board updatedBoard, 
                       @RequestParam(value = "imageFiles", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> imageFiles,
                       @RequestParam(value = "generalFiles", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> generalFiles,
                       @RequestParam(value = "deleteFileIds", required = false) java.util.List<Long> deleteFileIds,
                       HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");

        Board board = boardService.getBoard(id);
        if (board == null || "Y".equals(board.getDelYn())) {
            return "redirect:/boards";
        }

        boolean isAdmin = "ADMIN".equals(loginUser.getRole());

        if (!board.getWriterId().equals(loginUser.getId()) && !isAdmin) {
            model.addAttribute("message", "수정 권한이 없습니다.");
            model.addAttribute("redirectUri", "/boards/" + id);
            return "alert";
        }

        board.setTitle(updatedBoard.getTitle());
        board.setContent(updatedBoard.getContent());
        if (updatedBoard.getCategory() != null) {
            board.setCategory(updatedBoard.getCategory());
        }
        board.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Delete requested files
        fileStorageService.deleteAttachments(deleteFileIds, board.getAttachments());
        
        // Add new files
        processAttachments(board, imageFiles, true);
        processAttachments(board, generalFiles, false);
        
        long validImages = board.getAttachments().stream().filter(a -> a.isImage()).count();
        long validFiles = board.getAttachments().stream().filter(a -> !a.isImage()).count();
        
        if (validImages > 10) {
            model.addAttribute("message", "이미지는 최대 10개까지 업로드할 수 있습니다.");
            model.addAttribute("redirectUri", "/boards/" + id + "/edit");
            return "alert";
        }
        if (validFiles > 3) {
            model.addAttribute("message", "파일은 최대 3개까지 업로드할 수 있습니다.");
            model.addAttribute("redirectUri", "/boards/" + id + "/edit");
            return "alert";
        }

        boardService.saveBoard(board);
        return "redirect:/boards/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");

        Board board = boardService.getBoard(id);
        if (board == null || "Y".equals(board.getDelYn())) {
            return "redirect:/boards";
        }

        boolean isAdmin = "ADMIN".equals(loginUser.getRole());

        if (!board.getWriterId().equals(loginUser.getId()) && !isAdmin) {
            model.addAttribute("message", "삭제 권한이 없습니다.");
            model.addAttribute("redirectUri", "/boards/" + id);
            return "alert";
        }

        // 물리적 파일 삭제
        for (com.seochang.church.entity.BoardAttachment attachment : board.getAttachments()) {
            fileStorageService.deleteFile(attachment.getStoredFileName());
        }
        // DB에서도 매핑 삭제 (옵션, 하드삭제 안하더라도 파일은 날리므로)
        board.getAttachments().clear();

        boardService.deleteBoard(id);
        return "redirect:/boards";
    }
}
