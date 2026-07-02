package com.virtualmarket.polymarket.service;

import com.virtualmarket.polymarket.dto.WalletResponse;
import com.virtualmarket.polymarket.dto.WalletTransactionResponse;
import com.virtualmarket.polymarket.entity.User;
import com.virtualmarket.polymarket.entity.Wallet;
import com.virtualmarket.polymarket.entity.WalletTransaction;
import com.virtualmarket.polymarket.repository.UserRepository;
import com.virtualmarket.polymarket.repository.WalletRepository;
import com.virtualmarket.polymarket.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class WalletApiService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletApiService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Wallet not found"));

        WalletResponse response = new WalletResponse();
        response.setWalletId(wallet.getId());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setBalance(wallet.getBalance());
        response.setReliabilityScore(user.getReliabilityScore());

        return response;
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getWalletTransactionsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private WalletTransactionResponse mapToResponse(WalletTransaction transaction) {
        WalletTransactionResponse response = new WalletTransactionResponse();
        response.setTransactionId(transaction.getId());
        response.setUserId(transaction.getUser().getId());
        response.setUsername(transaction.getUser().getUsername());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setBalanceAfter(transaction.getBalanceAfter());
        response.setDescription(transaction.getDescription());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }
}
