package com.seochang.church.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", schema = "seochang_church_db")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name; // 성명 (이름)

    @Column(name = "baptismal_name", nullable = true, insertable = true, updatable = true)
    private String baptismalName; // 세례명 (예: 바오로, 마리아 등)

    private String email;

    @Column(nullable = true)
    private String district; // 구역 (예: 1구역, 2구역 등)

    @Column(nullable = false)
    private String role = "USER"; // 기본값: USER, 관리자는 ADMIN

    @Column(name = "del_yn", nullable = false)
    private String delYn = "N"; // 기본값: N, 삭제/탈퇴 시 Y
    
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean approved = false; // 기본값: 미승인

    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {
    }

    public User(String username, String password, String name, String baptismalName, String email) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.baptismalName = baptismalName;
        this.email = email;
        this.role = "USER";
        this.delYn = "N";
        this.approved = false;
    }

    public User(String username, String password, String name, String baptismalName, String email, String role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.baptismalName = baptismalName;
        this.email = email;
        this.role = role != null ? role : "USER";
        this.delYn = "N";
        this.approved = false;
    }

    // 이름 + 세례명을 결합하여 표기용 이름을 반환하는 편의 메서드
    public String getDisplayName() {
        if (baptismalName != null && !baptismalName.trim().isEmpty()) {
            return name + " (" + baptismalName + ")";
        }
        return name;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaptismalName() {
        return baptismalName;
    }

    public void setBaptismalName(String baptismalName) {
        this.baptismalName = baptismalName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDelYn() {
        return delYn;
    }

    public void setDelYn(String delYn) {
        this.delYn = delYn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
}
