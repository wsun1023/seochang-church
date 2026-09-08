package com.seochang.church.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    private final SessionRefreshInterceptor sessionRefreshInterceptor;
    private final CsrfInterceptor csrfInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final LoginInterceptor loginInterceptor;

    public WebConfig(AdminInterceptor adminInterceptor, LoginInterceptor loginInterceptor, SessionRefreshInterceptor sessionRefreshInterceptor, CsrfInterceptor csrfInterceptor) {
        this.sessionRefreshInterceptor = sessionRefreshInterceptor;
        this.csrfInterceptor = csrfInterceptor;
        this.adminInterceptor = adminInterceptor;
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir);
        String uploadAbsolutePath = uploadPath.toFile().getAbsolutePath();
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadAbsolutePath + "/");
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(sessionRefreshInterceptor);
        registry.addInterceptor(csrfInterceptor);
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**", "/notices/new", "/notices/*/edit", "/notices/*/delete", 
                                 "/gallery/new", "/gallery/*/edit", "/gallery/*/delete");

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/boards", "/boards/**", "/api/boards/**", "/api/images/**");
    }
}
