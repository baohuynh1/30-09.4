package com.example.identityservice.service;

import com.example.identityservice.dto.LoginRequest;
import com.example.identityservice.dto.RegisterRequest;
import com.example.identityservice.dto.TokenResponse;
import com.example.identityservice.dto.UserResponse;
import com.example.identityservice.entity.Role;
import com.example.identityservice.entity.User;
import com.example.identityservice.exception.UserNotFoundException;
import com.example.identityservice.exception.UsernameAlreadyExistsException;
import com.example.identityservice.repository.UserRepository;
import com.example.identityservice.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Hash BCrypt "giả" dùng khi username không tồn tại, để thời gian phản hồi
     * tương đương trường hợp sai mật khẩu (chống dò username qua timing).
     */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()), // BCrypt hash
                Role.USER // role mặc định, không cho client tự chọn
        );

        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Bài 3: đăng nhập stateless.
     * - Tìm user theo username.
     * - Đối soát bằng BCrypt: matches(raw, encoded) — KHÔNG dùng == / equals,
     *   vì BCrypt có salt ngẫu nhiên nên mỗi lần encode ra một chuỗi khác nhau.
     * - Sai username hay sai mật khẩu đều trả chung "Bad credentials".
     * - Thành công -> cấp JWT, server không lưu gì (không session).
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username()).orElse(null);

        String encodedPassword = (user != null) ? user.getPassword() : dummyHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), encodedPassword);

        if (user == null || !passwordMatches) {
            throw new BadCredentialsException("Bad credentials");
        }

        return new TokenResponse(jwtUtil.generateToken(user), "Bearer", jwtUtil.getExpirationMs() / 1000);
    }

    /** Bài 2: tìm user trong DB rồi cấp JWT (chỉ dùng để test, chưa kiểm tra mật khẩu). */
    @Transactional(readOnly = true)
    public TokenResponse generateTestToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        return new TokenResponse(jwtUtil.generateToken(user), "Bearer", jwtUtil.getExpirationMs() / 1000);
    }
}
