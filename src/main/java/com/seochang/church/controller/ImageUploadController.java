package com.seochang.church.controller;

import com.seochang.church.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageUploadController {

    private final FileStorageService fileStorageService;

    public ImageUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile image) {
        try {
            String storedFileName = fileStorageService.store(image, "editor");
            // The fileStorageService returns the relative path inside the uploadDir.
            // Spring Boot configures /uploads/** to serve files from uploadDir.
            String imageUrl = "/uploads/" + storedFileName;
            
            Map<String, String> response = new HashMap<>();
            response.put("url", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
