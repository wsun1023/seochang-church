package com.seochang.church.service;

import com.seochang.church.entity.Banner;
import com.seochang.church.repository.BannerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BannerService {

    private final BannerRepository bannerRepository;
    private final FileStorageService fileStorageService;

    public BannerService(BannerRepository bannerRepository, FileStorageService fileStorageService) {
        this.bannerRepository = bannerRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Banner> getAllBanners() {
        return bannerRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Banner> getActivePopups() {
        return bannerRepository.findActiveBannersByType("POPUP", LocalDate.now());
    }

    public List<Banner> getActiveBanners() {
        return bannerRepository.findActiveBannersByType("BANNER", LocalDate.now());
    }

    public Banner getBannerById(Long id) {
        return bannerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid banner Id:" + id));
    }

    @Transactional
    public Banner saveBanner(Banner banner) {
        return bannerRepository.save(banner);
    }

    @Transactional
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner != null) {
            String imageUrl = banner.getImageUrl();
            if (imageUrl != null) {
                if (imageUrl.startsWith("/uploads/")) {
                    String storedFileName = imageUrl.substring("/uploads/".length());
                    fileStorageService.deleteFile(storedFileName);
                } else if (imageUrl.startsWith("uploads/")) {
                    String storedFileName = imageUrl.substring("uploads/".length());
                    fileStorageService.deleteFile(storedFileName);
                }
            }
            bannerRepository.delete(banner);
        }
    }

    @Transactional
    public void toggleActive(Long id) {
        Banner banner = getBannerById(id);
        banner.setActive(!banner.isActive());
        bannerRepository.save(banner);
    }
}
