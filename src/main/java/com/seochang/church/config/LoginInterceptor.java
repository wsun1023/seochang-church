package com.seochang.church.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("loginUser") != null) {
            return true;
        }
        
        // Return alert page or redirect for unauthorized access
        if (request.getRequestURI().startsWith("/api/")) {
            response.sendError(401);
        } else if ("GET".equalsIgnoreCase(request.getMethod())) {
            String target = request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
            String redirect = "/login?error=login-required&redirectUrl=" + java.net.URLEncoder.encode(target, java.nio.charset.StandardCharsets.UTF_8);
            response.sendRedirect(redirect);
        } else {
            response.sendRedirect("/login?error=login-required");
        }
        return false;
    }
}
