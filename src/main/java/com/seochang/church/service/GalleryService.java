package com.seochang.church.service;

import com.seochang.church.entity.Gallery;
import com.seochang.church.repository.GalleryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GalleryService {

    private final GalleryRepository galleryRepository;

    public GalleryService(GalleryRepository galleryRepository) {
        this.galleryRepository = galleryRepository;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Gallery> getActiveGalleries(int page, String keyword) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 12, org.springframework.data.domain.Sort.by("createdAt").descending());
        if (keyword != null && !keyword.trim().isEmpty()) {
            return galleryRepository.findByDelYnAndTitleContaining("N", keyword.trim(), pageable);
        }
        return galleryRepository.findByDelYn("N", pageable);
    }

    @Transactional(readOnly = true)
    public Gallery getGallery(Long id) {
        return galleryRepository.findById(id).orElse(null);
    }

    public void increaseViewCount(Long id) {
        Gallery gallery = getGallery(id);
        if (gallery != null) {
            gallery.setViewCount(gallery.getViewCount() + 1);
            galleryRepository.save(gallery);
        }
    }

    public Gallery saveGallery(Gallery gallery) {
        return galleryRepository.save(gallery);
    }

    public void deleteGallery(Long id) {
        Gallery gallery = getGallery(id);
        if (gallery != null) {
            gallery.setDelYn("Y");
            galleryRepository.save(gallery);
        }
    }
}
