package com.seochang.church.repository;

import com.seochang.church.entity.BoardAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardAttachmentRepository extends JpaRepository<BoardAttachment, Long> {
}
