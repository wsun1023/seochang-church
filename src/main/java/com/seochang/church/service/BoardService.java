package com.seochang.church.service;

import com.seochang.church.entity.Board;
import com.seochang.church.repository.BoardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Board> getActiveBoards(int page, String keyword) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 10, org.springframework.data.domain.Sort.by("createdAt").descending());
        if (keyword != null && !keyword.trim().isEmpty()) {
            return boardRepository.findActiveByKeyword(keyword.trim(), pageable);
        }
        return boardRepository.findByDelYn("N", pageable);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Board> getActiveBoardsByCategory(String category, int page, String keyword) {
        if (category == null || category.isEmpty() || "all".equals(category)) {
            return getActiveBoards(page, keyword);
        }
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 10, org.springframework.data.domain.Sort.by("createdAt").descending());
        if (keyword != null && !keyword.trim().isEmpty()) {
            return boardRepository.findActiveByCategoryAndKeyword(category, keyword.trim(), pageable);
        }
        return boardRepository.findByDelYnAndCategory("N", category, pageable);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Board> getAdminBoards(int page, String keyword) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 10, org.springframework.data.domain.Sort.by("createdAt").descending());
        if (keyword != null && !keyword.trim().isEmpty()) {
            return boardRepository.findByKeyword(keyword.trim(), pageable);
        }
        return boardRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Board getBoard(Long id) {
        return boardRepository.findById(id).orElse(null);
    }

    public void increaseViewCount(Long id) {
        Board board = getBoard(id);
        if (board != null) {
            board.setViewCount(board.getViewCount() + 1);
            boardRepository.save(board);
        }
    }

    public Board saveBoard(Board board) {
        return boardRepository.save(board);
    }

    public void deleteBoard(Long id) {
        Board board = getBoard(id);
        if (board != null) {
            board.setDelYn("Y");
            boardRepository.save(board);
        }
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Board> getAllBoards(org.springframework.data.domain.Pageable pageable) {
        return boardRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public long getTotalBoardCount() {
        return boardRepository.countByDelYn("N");
    }

    @Transactional(readOnly = true)
    public long getBoardCountByCategory(String category) {
        return boardRepository.countByDelYnAndCategory("N", category);
    }
}
