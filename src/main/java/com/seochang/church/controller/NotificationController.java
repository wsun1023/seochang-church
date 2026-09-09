package com.seochang.church.controller;

import com.seochang.church.entity.User;
import com.seochang.church.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;

@Controller
public class NotificationController {
    private final NotificationService notifications;
    public NotificationController(NotificationService notifications) { this.notifications = notifications; }

    private User user(HttpSession session) {
        Object value = session.getAttribute("loginUser");
        if (!(value instanceof User user) || !user.isApproved() || !"N".equals(user.getDelYn()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return user;
    }

    @GetMapping("/notifications")
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "false") boolean unreadOnly, HttpSession session, Model model) {
        if (session.getAttribute("loginUser") == null) return "redirect:/login?error=login-required";
        Long recipient = user(session).getId();
        model.addAttribute("notificationPage", notifications.list(recipient, page, unreadOnly));
        model.addAttribute("unreadOnly", unreadOnly);
        model.addAttribute("unreadCount", notifications.unreadCount(recipient));
        model.addAttribute("currentMenu", "notifications");
        return "notifications";
    }

    @GetMapping("/api/notifications/unread-count") @ResponseBody
    public Map<String, Long> unreadCount(HttpSession session, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("count", notifications.unreadCount(user(session).getId()));
    }

    @PostMapping("/notifications/{id}/open")
    public String open(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        String path = notifications.open(user(session).getId(), id);
        if (path == null) {
            redirect.addFlashAttribute("notificationMessage", "삭제되었거나 더 이상 볼 수 없는 게시물입니다.");
            return "redirect:/notifications";
        }
        return "redirect:" + path;
    }

    @PostMapping("/notifications/{id}/read")
    public String read(@PathVariable Long id, HttpSession session) {
        notifications.markRead(user(session).getId(), id);
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAll(HttpSession session) {
        notifications.markAllRead(user(session).getId());
        return "redirect:/notifications";
    }
}
