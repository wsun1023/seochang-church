package com.seochang.church.controller;

import com.seochang.church.entity.User;
import com.seochang.church.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {
    private final UserService users;
    public ProfileController(UserService users) { this.users = users; }

    private User member(HttpSession session) {
        User login = (User) session.getAttribute("loginUser");
        if (login == null) return null;
        User user = users.getUserById(login.getId());
        if (user == null || !"N".equals(user.getDelYn()) || !user.isApproved()) {
            session.removeAttribute("loginUser");
            return null;
        }
        return user;
    }

    @GetMapping({"/profile", "/admin/profile"})
    public String profile(HttpSession session, Model model, jakarta.servlet.http.HttpServletRequest request) {
        User user = member(session);
        if (user == null) return "redirect:/login";
        boolean admin = "ADMIN".equals(user.getRole());
        if (admin && !request.getRequestURI().endsWith("/admin/profile")) return "redirect:/admin/profile";
        if (!admin && request.getRequestURI().endsWith("/admin/profile")) return "redirect:/profile";
        model.addAttribute("profile", user);
        model.addAttribute("currentMenu", "profile");
        model.addAttribute("profileAction", admin ? "/admin/profile" : "/profile");
        return admin ? "admin/profile" : "profile";
    }

    @PostMapping({"/profile", "/admin/profile"})
    public String update(HttpSession session, Model model, RedirectAttributes redirect,
                         @RequestParam String name,
                         @RequestParam(defaultValue = "") String baptismalName,
                         @RequestParam(defaultValue = "") String email,
                         @RequestParam(defaultValue = "") String district,
                         @RequestParam String currentPassword,
                         @RequestParam(defaultValue = "") String newPassword,
                         @RequestParam(defaultValue = "") String confirmPassword) {
        User user = member(session);
        if (user == null) return "redirect:/login";
        boolean admin = "ADMIN".equals(user.getRole());
        String path = admin ? "/admin/profile" : "/profile";
        try {
            User updated = users.updateProfile(user.getId(), name, baptismalName, email, district,
                    currentPassword, newPassword, confirmPassword);
            session.setAttribute("loginUser", updated);
            redirect.addFlashAttribute("success", "회원정보를 수정했습니다.");
            return "redirect:" + path;
        } catch (IllegalArgumentException error) {
            // Keep submitted profile fields, but never return passwords to the page.
            User draft = new User();
            draft.setUsername(user.getUsername());
            draft.setName(name); draft.setBaptismalName(baptismalName);
            draft.setEmail(email); draft.setDistrict(district);
            model.addAttribute("profile", draft);
            model.addAttribute("currentMenu", "profile");
            model.addAttribute("error", error.getMessage());
            model.addAttribute("profileAction", path);
            return admin ? "admin/profile" : "profile";
        }
    }
}
