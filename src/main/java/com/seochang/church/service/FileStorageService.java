package com.seochang.church.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import net.coobird.thumbnailator.Thumbnails;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    public void init() {
        try {
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    public String store(MultipartFile file) {
        init();
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            
            String originalFileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            boolean isImage = contentType != null && contentType.startsWith("image/");
            
            String extension = "";
            if (isImage) {
                extension = ".jpg";
            } else if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            
            String storedFileName = UUID.randomUUID().toString() + extension;
            
            Path destinationFile = Paths.get(uploadDir).resolve(Paths.get(storedFileName))
                    .normalize().toAbsolutePath();
                    
            if (!destinationFile.getParent().equals(Paths.get(uploadDir).toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }
            
            if (isImage) {
                Thumbnails.of(file.getInputStream())
                        .size(1280, 1280)
                        .outputFormat("jpg")
                        .outputQuality(0.85)
                        .useExifOrientation(true)
                        .toFile(destinationFile.toFile());
            } else {
                file.transferTo(destinationFile.toFile());
            }
            
            return storedFileName;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    public void deleteFile(String storedFileName) {
        try {
            Path file = Paths.get(uploadDir).resolve(Paths.get(storedFileName)).normalize().toAbsolutePath();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + storedFileName);
        }
    }
}
