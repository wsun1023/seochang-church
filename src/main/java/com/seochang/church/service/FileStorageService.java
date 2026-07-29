package com.seochang.church.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import com.seochang.church.entity.Attachment;
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

    public String store(MultipartFile file, String module) {
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
            
            String subDir = module + "/" + (isImage ? "img" : "file");
            String storedFileName = subDir + "/" + UUID.randomUUID().toString() + extension;
            
            Path destinationFile = Paths.get(uploadDir).resolve(Paths.get(storedFileName))
                    .normalize().toAbsolutePath();
                    
            if (!destinationFile.getParent().startsWith(Paths.get(uploadDir).normalize().toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }
            
            Files.createDirectories(destinationFile.getParent());
            
            // 프론트엔드에서 이미 압축 및 최적화를 수행하므로 원본 그대로 저장
            file.transferTo(destinationFile.toFile());
            
            return storedFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }
        
        try {
            Path file = Paths.get(uploadDir).resolve(fileName).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + fileName, e);
        }
    }

    /**
     * Delete files from disk and remove them from the collection based on requested IDs
     */
    public void deleteAttachments(List<Long> deleteFileIds, Collection<? extends Attachment> attachments) {
        if (deleteFileIds == null || deleteFileIds.isEmpty() || attachments == null) return;
        
        Iterator<? extends Attachment> iterator = attachments.iterator();
        while (iterator.hasNext()) {
            Attachment attachment = iterator.next();
            if (deleteFileIds.contains(attachment.getId())) {
                deleteFile(attachment.getStoredFileName());
                iterator.remove();
            }
        }
    }
}
