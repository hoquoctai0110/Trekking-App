package com.example.trekkingapp.auth;

import com.example.trekkingapp.role.Role;
import com.example.trekkingapp.role.RoleRepository;
import com.example.trekkingapp.role.UserRole;
import com.example.trekkingapp.role.UserRoleRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import com.example.trekkingapp.user.UserResponse;
import com.example.trekkingapp.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AuthService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final Set<String> SELECTABLE_ROLES = Set.of("TREKKER", "TOUR_PROVIDER");

    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthService(
            GoogleTokenVerifierService googleTokenVerifierService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            UserService userService,
            JwtService jwtService,
            CurrentUserService currentUserService
    ) {
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userService = userService;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public GoogleLoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = googleTokenVerifierService.verify(request.idToken());
        User user = userRepository.findByGoogleId(googleUserInfo.googleId())
                .orElseGet(() -> findOrCreateUser(googleUserInfo));

        return new GoogleLoginResponse(jwtService.generateToken(user), userService.toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Long userId = currentUserService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return userService.toResponse(user);
    }

    @Transactional
    public UserResponse selectMyRole(SelectRoleRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        return selectRoleForUser(currentUserId, request);
    }

    @Transactional
    public UserResponse selectRole(Long userId, SelectRoleRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new IllegalArgumentException("You are not allowed to select role for another user");
        }

        return selectRoleForUser(userId, request);
    }

    private UserResponse selectRoleForUser(Long userId, SelectRoleRequest request) {
        String requestedRole = request.role().trim().toUpperCase();
        if (!SELECTABLE_ROLES.contains(requestedRole)) {
            throw new IllegalArgumentException("Only TREKKER or TOUR_PROVIDER can be selected by users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (Boolean.TRUE.equals(user.getRoleSelected())) {
            throw new IllegalArgumentException("Role already selected");
        }

        Role role = roleRepository.findByRoleName(requestedRole)
                .orElseThrow(() -> new IllegalStateException("Required role is missing: " + requestedRole));

        if (!userRoleRepository.existsByUserAndRole(user, role)) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
            user.getUserRoles().add(userRole);
        }

        user.setRoleSelected(true);
        User savedUser = userRepository.save(user);
        return userService.toResponse(savedUser);
    }

    private User createUser(GoogleUserInfo googleUserInfo) {
        User user = new User();
        user.setGoogleId(googleUserInfo.googleId());
        user.setEmail(googleUserInfo.email());
        user.setFullName(googleUserInfo.fullName());
        user.setAvatarUrl(googleUserInfo.avatarUrl());
        user.setStatus(STATUS_ACTIVE);
        user.setRoleSelected(false);
        return userRepository.save(user);
    }

    private User findOrCreateUser(GoogleUserInfo googleUserInfo) {
        return userRepository.findByEmail(googleUserInfo.email())
                .map(existingUser -> linkOrValidateGoogleAccount(existingUser, googleUserInfo))
                .orElseGet(() -> createUser(googleUserInfo));
    }

    private User linkOrValidateGoogleAccount(User existingUser, GoogleUserInfo googleUserInfo) {
        String existingGoogleId = existingUser.getGoogleId();
        if (existingGoogleId == null || existingGoogleId.isBlank()) {
            existingUser.setGoogleId(googleUserInfo.googleId());
            if (googleUserInfo.fullName() != null && !googleUserInfo.fullName().isBlank()) {
                existingUser.setFullName(googleUserInfo.fullName());
            }
            if (googleUserInfo.avatarUrl() != null && !googleUserInfo.avatarUrl().isBlank()) {
                existingUser.setAvatarUrl(googleUserInfo.avatarUrl());
            }
            return userRepository.save(existingUser);
        }

        if (!existingGoogleId.equals(googleUserInfo.googleId())) {
            throw new IllegalArgumentException("Email is already linked to another Google account");
        }

        return existingUser;
    }
}
