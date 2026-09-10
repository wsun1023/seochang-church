package com.seochang.church.service;

import com.seochang.church.entity.User;
import com.seochang.church.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notifications;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, NotificationService notifications) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notifications = notifications;
    }

    public User registerUser(String username, String rawPassword, String name, String baptismalName, String email, String district) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 존재하거나 사용 중인 아이디입니다.");
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        User newUser = new User(username, encodedPassword, name, baptismalName, email);
        newUser.setDistrict(district);
        return userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public User login(String username, String rawPassword) {
        Optional<User> userOptional = userRepository.findByUsernameAndDelYn(username, "N");
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }
        
        if (!user.isApproved()) {
            throw new IllegalArgumentException("관리자의 승인을 대기 중입니다.");
        }

        return user;
    }

    public void approveUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        boolean newlyApproved = !user.isApproved();
        user.setApproved(true);
        userRepository.save(user);
        if (newlyApproved) notifications.memberApproved(user);
    }

    public User updateProfile(Long id, String name, String baptismalName, String email, String district,
                              String currentPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        if (!"N".equals(user.getDelYn()) || !user.isApproved()) throw new IllegalArgumentException("사용할 수 없는 회원 계정입니다.");
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        name = profileField(name, "이름");
        baptismalName = profileField(baptismalName, "세례명");
        email = profileField(email, "이메일");
        district = profileField(district, "구역");
        if (name.isEmpty()) throw new IllegalArgumentException("이름을 입력해주세요.");
        if (!email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("이메일 형식을 확인해주세요.");
        if (!newPassword.isEmpty() || !confirmPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) throw new IllegalArgumentException("새 비밀번호 확인이 일치하지 않습니다.");
            if (newPassword.isBlank() || newPassword.length() < 8 || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
                throw new IllegalArgumentException("새 비밀번호는 8자 이상, UTF-8 기준 72바이트 이하여야 합니다.");
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        user.setName(name);
        user.setBaptismalName(baptismalName);
        user.setEmail(email);
        user.setDistrict(district);
        return userRepository.save(user);
    }

    private String profileField(String value, String label) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() > 255) throw new IllegalArgumentException(label + "은 255자 이내로 입력해주세요.");
        return trimmed;
    }

    public void changeUserRole(Long id, String role) {
        if (!java.util.Set.of("USER", "ADMIN").contains(role)) throw new IllegalArgumentException("잘못된 권한입니다.");
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        user.setRole(role);
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        user.setDelYn("Y");
        userRepository.save(user);
    }

    public String resetPassword(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        if ("Y".equals(user.getDelYn())) {
            throw new IllegalStateException("삭제되거나 탈퇴한 회원의 비밀번호는 초기화할 수 없습니다.");
        }
        String tempPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        return tempPassword;
    }

    private static final String TEMP_PW_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$";

    private String generateTemporaryPassword() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder("sc");
        for (int i = 0; i < 6; i++) {
            sb.append(TEMP_PW_CHARS.charAt(random.nextInt(TEMP_PW_CHARS.length())));
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<User> getAllUsers(org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<User> searchUsers(String keyword, org.springframework.data.domain.Pageable pageable) {
        return userRepository.searchUsers(keyword, pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public long getTotalUserCount() {
        return userRepository.countByDelYn("N");
    }

    @Transactional(readOnly = true)
    public long getPendingApprovalCount() {
        return userRepository.countByApprovedFalseAndDelYn("N");
    }
}
