package com.seochang.church.service;

import com.seochang.church.entity.Gallery;
import com.seochang.church.entity.GalleryAttachment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class GalleryPhotoService {
    private final FileStorageService storage;
    public GalleryPhotoService(FileStorageService storage) { this.storage = storage; }

    /** Tokens describe existing photo IDs and zero-based indexes in the submitted upload list. */
    @Transactional
    public void update(Gallery gallery, List<MultipartFile> uploads, List<Long> deletedIds,
                       List<String> photoOrder, String coverPhoto) {
        List<MultipartFile> files = uploads == null ? List.of() : uploads.stream().filter(f -> !f.isEmpty()).toList();
        Set<Long> deleted = deletedIds == null ? Set.of() : new HashSet<>(deletedIds);
        Set<Long> existing = new HashSet<>();
        gallery.getAttachments().forEach(a -> existing.add(a.getId()));
        if (!existing.containsAll(deleted)) throw invalid("이 사진첩에 속하지 않는 사진은 삭제할 수 없습니다.");

        Map<String, GalleryAttachment> photos = new LinkedHashMap<>();
        gallery.getPhotos().stream().filter(a -> !deleted.contains(a.getId()))
                .forEach(a -> photos.put("existing:" + a.getId(), a));
        List<String> expected = new ArrayList<>(photos.keySet());
        for (int i = 0; i < files.size(); i++) expected.add("new:" + i);
        if (expected.isEmpty() || expected.size() > 50) throw invalid("사진은 최소 1장, 최대 50장까지 등록할 수 있습니다.");
        List<String> order = photoOrder == null || photoOrder.isEmpty() ? expected : photoOrder;
        if (order.size() != expected.size() || !new HashSet<>(order).equals(new HashSet<>(expected)))
            throw invalid("사진 목록이 변경되었습니다. 사진 순서를 다시 확인해주세요.");

        String cover = coverPhoto;
        if (cover == null || cover.isBlank()) {
            cover = photos.entrySet().stream().filter(e -> e.getValue().isCoverPhoto()).map(Map.Entry::getKey)
                    .findFirst().orElse(order.get(0));
        }
        if (!expected.contains(cover)) throw invalid("대표 사진은 저장할 사진 중에서 선택해주세요.");
        storage.validatePlan(gallery.getAttachments(), deletedIds, files, null, 50, 3);

        // All references and limits are checked before any file operation.
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String storedName = storage.store(file, "gallery");
            GalleryAttachment attachment = new GalleryAttachment();
            attachment.setGallery(gallery);
            attachment.setOriginalFileName(Optional.ofNullable(file.getOriginalFilename()).orElse("사진"));
            attachment.setStoredFileName(storedName);
            attachment.setFilePath("/uploads/" + storedName);
            attachment.setFileSize(file.getSize());
            attachment.setImage(true);
            gallery.getAttachments().add(attachment);
            photos.put("new:" + i, attachment);
        }
        storage.deleteAttachments(deletedIds, gallery.getAttachments());
        for (int i = 0; i < order.size(); i++) {
            GalleryAttachment photo = photos.get(order.get(i));
            photo.setSortOrder(i);
            photo.setCoverPhoto(order.get(i).equals(cover));
        }
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
