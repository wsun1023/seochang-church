package com.seochang.church.service;

import com.seochang.church.entity.Attachment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "txt", "csv", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "hwp", "hwpx", "zip");
    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    public void init() {
        try { Files.createDirectories(root()); }
        catch (IOException e) { throw new IllegalStateException("Cannot initialize file storage", e); }
    }

    private Path root() { return Paths.get(uploadDir).toAbsolutePath().normalize(); }
    private Path resolve(String name) {
        Path path = root().resolve(name).normalize();
        if (!path.startsWith(root()) || path.equals(root())) throw invalid("잘못된 파일 경로입니다.");
        return path;
    }
    private ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private boolean image(MultipartFile file) {
        return file.getContentType() != null && file.getContentType().startsWith("image/");
    }
    private String extension(MultipartFile file) {
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
    public void validateFile(MultipartFile file, boolean requireImage) {
        if (file == null || file.isEmpty() || file.getSize() > 10 * 1024 * 1024) throw invalid("파일은 1바이트 이상 10MB 이하여야 합니다.");
        if (requireImage && !image(file)) throw invalid("이미지 파일만 업로드할 수 있습니다.");
        if (image(file)) {
            try (var input = file.getInputStream(); ImageInputStream stream = ImageIO.createImageInputStream(input)) {
                if (stream == null) throw invalid("이미지를 읽을 수 없습니다.");
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw invalid("지원하지 않는 이미지 형식입니다.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(stream);
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > 25_000_000) throw invalid("이미지 해상도가 너무 큽니다.");
                    reader.read(0);
                } finally { reader.dispose(); }
            } catch (IOException e) { throw invalid("손상된 이미지입니다."); }
        } else if (!DOCUMENT_EXTENSIONS.contains(extension(file))) {
            throw invalid("지원하지 않는 첨부파일 형식입니다.");
        }
    }
    public void validatePlan(Collection<? extends Attachment> existing, List<Long> deleted, List<MultipartFile> images, List<MultipartFile> files, int maxImages, int maxFiles) {
        Set<Long> ids = deleted == null ? Set.of() : new HashSet<>(deleted);
        long imageCount = existing.stream().filter(a -> !ids.contains(a.getId()) && a.isImage()).count();
        long fileCount = existing.stream().filter(a -> !ids.contains(a.getId()) && !a.isImage()).count();
        if (images != null) imageCount += images.stream().filter(f -> !f.isEmpty()).count();
        if (files != null) fileCount += files.stream().filter(f -> !f.isEmpty()).count();
        if (imageCount > maxImages || fileCount > maxFiles) throw invalid("첨부파일 허용 개수를 초과했습니다.");
        if (images != null) images.stream().filter(f -> !f.isEmpty()).forEach(f -> validateFile(f, true));
        if (files != null) files.stream().filter(f -> !f.isEmpty()).forEach(f -> validateFile(f, false));
    }
    public String store(MultipartFile file, String module) {
        validateFile(file, "editor".equals(module) || "banner".equals(module));
        if (!Set.of("board", "notice", "gallery", "editor", "banner").contains(module)) throw invalid("잘못된 업로드 경로입니다.");
        String name = module + "/" + (image(file) ? "img/" : "file/") + UUID.randomUUID() + "." + (image(file) ? "jpg" : extension(file));
        Path destination = resolve(name);
        try {
            Files.createDirectories(destination.getParent());
            if (image(file)) {
                try (var input = file.getInputStream()) {
                    BufferedImage source = ImageIO.read(input);
                    BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
                    var graphics = rgb.createGraphics();
                    try { graphics.setColor(java.awt.Color.WHITE); graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight()); graphics.drawImage(source, 0, 0, null); }
                    finally { graphics.dispose(); }
                    if (!ImageIO.write(rgb, "jpg", destination.toFile())) throw new IOException("JPEG encoder unavailable");
                }
            } else file.transferTo(destination);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) remove(destination);
                    }
                });
            }
            return name;
        } catch (IOException | RuntimeException e) {
            remove(destination);
            throw new IllegalStateException("파일 저장에 실패했습니다.", e);
        }
    }
    public void deleteFile(String name) {
        if (name == null || name.isBlank()) return;
        Path path = resolve(name);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { remove(path); }
            });
        } else remove(path);
    }
    private void remove(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Failed to remove stored file {}", path, e); }
    }
    public void deleteAttachments(List<Long> ids, Collection<? extends Attachment> attachments) {
        if (ids == null || attachments == null) return;
        attachments.removeIf(a -> {
            if (!ids.contains(a.getId())) return false;
            deleteFile(a.getStoredFileName());
            return true;
        });
    }
}
