package com.seochang.church.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@Controller
public class FileDownloadController {

    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("file") String fileName, 
                                                 @RequestParam("originalName") String originalName) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(fileName).normalize();

            // 보안 취약점 방지: 요청된 파일이 uploadDir 안에 있는지 확인 (Path Traversal 방어)
            if (!filePath.startsWith(uploadPath) || filePath.equals(uploadPath)) {
                return ResponseEntity.status(403).build(); // 권한 없음(Forbidden) 반환
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable() && Files.isRegularFile(filePath)) {
                String encodedOriginalName = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedOriginalName + "\"; filename*=UTF-8''" + encodedOriginalName)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/pdf/view")
    public ResponseEntity<Resource> viewPdfInline(@RequestParam("file") String fileName,
                                                  @RequestParam(value = "originalName", required = false, defaultValue = "bulletin.pdf") String originalName) {
        try {
            if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                return ResponseEntity.status(403).build();
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(fileName).normalize();

            if (!filePath.startsWith(uploadPath) || filePath.equals(uploadPath)) {
                return ResponseEntity.status(403).build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable() && Files.isRegularFile(filePath)) {
                String safeName = (originalName == null || originalName.isBlank()) ? "bulletin.pdf" : originalName;
                String encodedOriginalName = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedOriginalName + "\"; filename*=UTF-8''" + encodedOriginalName)
                        .header("X-Content-Type-Options", "nosniff")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
