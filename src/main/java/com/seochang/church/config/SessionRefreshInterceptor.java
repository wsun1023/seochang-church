package com.seochang.church.config;

import com.seochang.church.entity.User;
import com.seochang.church.service.UserService;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionRefreshInterceptor implements HandlerInterceptor {
    private final UserService users;

    public SessionRefreshInterceptor(UserService users) { this.users = users; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) return true;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("loginUser") instanceof User previous) {
            User current = users.getUserById(previous.getId());
            if (current == null || !current.isApproved() || !"N".equals(current.getDelYn())) {
                session.invalidate();
            } else {
                session.setAttribute("loginUser", current);
            }
        }
        return true;
    }
}
