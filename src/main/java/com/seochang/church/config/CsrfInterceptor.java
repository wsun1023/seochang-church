package com.seochang.church.config;

import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Synchronizer tokens protect both HTML forms and same-origin AJAX requests. */
@Component
public class CsrfInterceptor implements HandlerInterceptor {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute("csrfToken");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("csrfToken", token);
        }
        request.setAttribute("csrfToken", token);
        if (SAFE_METHODS.contains(request.getMethod())) return true;
        String supplied = request.getHeader("X-CSRF-TOKEN");
        if (supplied == null) supplied = request.getParameter("_csrf");
        if (supplied == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(403, "Invalid CSRF token");
            return false;
        }
        return true;
    }
}
