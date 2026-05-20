package com.example.trekkingapp.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> findById(Long userId) {
        return userRepository.findById(userId).map(this::toResponse);
    }

    public UserResponse toResponse(User user) {
        List<String> roles = user.getUserRoles()
                .stream()
                .map(userRole -> userRole.getRole().getRoleName())
                .toList();

        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getRoleSelected(),
                roles
        );
    }
}
