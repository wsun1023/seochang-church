package com.seochang.church.controller;

import com.seochang.church.entity.User;
import com.seochang.church.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                            @org.springframework.web.bind.annotation.RequestHeader(value = "Referer", required = false) String referer,
                            jakarta.servlet.http.HttpServletRequest request,
                            Model model) {
        String target = sanitizeRedirectUrl(redirectUrl);
        if (target == null && referer != null) {
            target = extractRelativePathFromReferer(referer, request);
        }
        if (target != null && !target.equals("/")) {
            model.addAttribute("redirectUrl", target);
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                        HttpSession session, jakarta.servlet.http.HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        try {
            User user = userService.login(username, password);
            request.changeSessionId();
            session.setAttribute("loginUser", user);
            String target = sanitizeRedirectUrl(redirectUrl);
            return "redirect:" + (target != null ? target : "/");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            String target = sanitizeRedirectUrl(redirectUrl);
            if (target != null && !target.equals("/")) {
                redirectAttributes.addAttribute("redirectUrl", target);
            }
            return "redirect:/login";
        }
    }

    public static String sanitizeRedirectUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.startsWith("/\\")) {
            return null;
        }
        if (trimmed.contains("\\") || trimmed.contains("\r") || trimmed.contains("\n")) {
            return null;
        }
        int queryIdx = trimmed.indexOf('?');
        String path = queryIdx != -1 ? trimmed.substring(0, queryIdx) : trimmed;
        if (path.contains(":") || path.contains("@")) {
            return null;
        }
        if (path.equals("/login") || path.equals("/signup") || path.equals("/logout")) {
            return "/";
        }
        return trimmed;
    }

    private String extractRelativePathFromReferer(String referer, jakarta.servlet.http.HttpServletRequest request) {
        try {
            java.net.URI uri = java.net.URI.create(referer);
            String reqHost = request.getServerName();
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(reqHost)) {
                return null;
            }
            String path = uri.getRawPath();
            if (path == null || path.isEmpty()) return null;
            String query = uri.getRawQuery();
            String fullPath = path + (query != null ? "?" + query : "");
            return sanitizeRedirectUrl(fullPath);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam("username") String username,
                         @RequestParam("password") String password,
                         @RequestParam("name") String name,
                         @RequestParam(value = "baptismalName", required = false) String baptismalName,
                         @RequestParam(value = "email", required = false) String email,
                         @RequestParam(value = "district", required = false) String district,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(username, password, name, baptismalName, email, district);
            redirectAttributes.addFlashAttribute("successMessage", "회원가입이 성공적으로 완료되었습니다! 로그인해 주세요.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/signup";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
