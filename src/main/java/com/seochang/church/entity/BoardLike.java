package com.seochang.church.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "board_likes", schema = "seochang_church_db", 
       uniqueConstraints = {@UniqueConstraint(columnNames = {"board_id", "user_id"})})
public class BoardLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime createdAt = LocalDateTime.now();

    public BoardLike() {}

    public BoardLike(Board board, User user) {
        this.board = board;
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Board getBoard() { return board; }
    public void setBoard(Board board) { this.board = board; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
