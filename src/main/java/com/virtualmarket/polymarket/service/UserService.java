package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.AuthResponse;
import com.virtualmarket.polymarket.dto.LoginRequest;
import com.virtualmarket.polymarket.dto.RegisterRequest;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.entity.Wallet;
import com.virtualmarket.polymarket.enums.UserRole;
import com.virtualmarket.polymarket.repository.UserRepository;
import com.virtualmarket.polymarket.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, WalletService walletService, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(BAD_REQUEST, "Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(BAD_REQUEST, "Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);

        User savedUser = userRepository.save(user);
        Wallet wallet = walletService.createWalletForUser(savedUser);

        return toAuthResponse(savedUser, wallet);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String usernameOrEmail = request.getUsernameOrEmail().trim();
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail.toLowerCase()))
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid username/email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid username/email or password");
        }

        Wallet wallet = walletService.getWalletByUserId(user.getId());
        return toAuthResponse(user, wallet);
    }

    private AuthResponse toAuthResponse(User user, Wallet wallet) {
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                wallet.getBalance(),
                jwtService.generateToken(user)
        );
    }
}
