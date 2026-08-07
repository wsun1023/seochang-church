package com.seochang.church.service;

import com.seochang.church.entity.Notice;
import com.seochang.church.repository.NoticeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public Page<Notice> getNotices(String category, int page, String keyword) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        if ("all".equals(category) || category == null || category.isEmpty()) {
            if (hasKeyword) {
                return noticeRepository.findByTitleKeyword(keyword.trim(), pageable);
            } else {
                return noticeRepository.findAll(pageable);
            }
        } else {
            if (hasKeyword) {
                return noticeRepository.findByCategoryAndTitleKeyword(category, keyword.trim(), pageable);
            } else {
                return noticeRepository.findByCategory(category, pageable);
            }
        }
    }

    public Page<Notice> getNoticesForAdmin(String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "id"));
        if (keyword != null && !keyword.trim().isEmpty()) {
            return noticeRepository.findByKeyword(keyword.trim(), pageable);
        }
        return noticeRepository.findAll(pageable);
    }

    public java.util.List<Notice> getAllNotices() {
        return noticeRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public java.util.List<Notice> getRecentNotices(int count) {
        Pageable pageable = PageRequest.of(0, count, Sort.by(Sort.Direction.DESC, "createdAt"));
        return noticeRepository.findAll(pageable).getContent();
    }

    public long getTotalNoticeCount() {
        return noticeRepository.count();
    }

    @Transactional
    public Notice getNoticeAndIncreaseViewCount(Long id) {
        Notice notice = noticeRepository.findById(id).orElse(null);
        if (notice != null) {
            notice.setViewCount(notice.getViewCount() + 1);
            noticeRepository.save(notice);
        }
        return notice;
    }

    public Notice getNotice(Long id) {
        return noticeRepository.findById(id).orElse(null);
    }

    @Transactional
    public Notice saveNotice(Notice notice) {
        return noticeRepository.save(notice);
    }

    @Transactional
    public void deleteNotice(Long id) {
        noticeRepository.deleteById(id);
    }
}
