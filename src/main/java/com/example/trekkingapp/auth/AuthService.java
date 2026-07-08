package com.example.trekkingapp.auth;

import com.example.trekkingapp.role.Role;
import com.example.trekkingapp.role.RoleRepository;
import com.example.trekkingapp.role.UserRole;
import com.example.trekkingapp.role.UserRoleRepository;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.trekkerprofile.TrekkerProfileService;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import com.example.trekkingapp.user.UserResponse;
import com.example.trekkingapp.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class AuthService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";
    private static final String AUTH_PROVIDER_GOOGLE = "GOOGLE";
    private static final String AUTH_PROVIDER_LOCAL = "LOCAL";
    private static final String ROLE_TREKKER = "TREKKER";
    private static final String ROLE_TOUR_PROVIDER = "TOUR_PROVIDER";
    private static final Set<String> SELECTABLE_ROLES = Set.of(ROLE_TREKKER, ROLE_TOUR_PROVIDER);
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";
    private static final String GOOGLE_LOGIN_ONLY_MESSAGE = "This account uses Google login";
    private static final String EMAIL_VERIFICATION_REQUIRED_MESSAGE = "Email verification required before login";
    private static final String OTP_INVALID_MESSAGE = "OTP is invalid or already used";
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int OTP_MAX_RESENDS = 5;

    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final TrekkerProfileService trekkerProfileService;
    private final TourProviderRepository tourProviderRepository;
    private final AuthOtpTokenRepository authOtpTokenRepository;
    private final OtpNotificationService otpNotificationService;
    private final Clock clock;
    private final SecureRandom secureRandom;

    @Autowired
    public AuthService(
            GoogleTokenVerifierService googleTokenVerifierService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            UserService userService,
            JwtService jwtService,
            CurrentUserService currentUserService,
            PasswordEncoder passwordEncoder,
            TrekkerProfileService trekkerProfileService,
            TourProviderRepository tourProviderRepository,
            AuthOtpTokenRepository authOtpTokenRepository,
            OtpNotificationService otpNotificationService
    ) {
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userService = userService;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
        this.trekkerProfileService = trekkerProfileService;
        this.tourProviderRepository = tourProviderRepository;
        this.authOtpTokenRepository = authOtpTokenRepository;
        this.otpNotificationService = otpNotificationService;
        this.clock = Clock.systemDefaultZone();
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public GoogleLoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = googleTokenVerifierService.verify(request.idToken());
        User user = userRepository.findByGoogleId(googleUserInfo.googleId())
                .orElseGet(() -> findOrCreateUser(googleUserInfo));

        ensureUserVerified(user);
        return new GoogleLoginResponse(jwtService.generateToken(user), userService.toResponse(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_CREDENTIALS_MESSAGE));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException(GOOGLE_LOGIN_ONLY_MESSAGE);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException(INVALID_CREDENTIALS_MESSAGE);
        }

        ensureUserVerified(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public OtpChallengeResponse registerTrekker(RegisterTrekkerRequest request) {
        validatePasswords(request.password(), request.confirmPassword());
        ensureEmailAvailable(request.email());

        User user = createLocalUser(
                request.fullName(),
                request.email(),
                request.phone(),
                request.dateOfBirth(),
                request.password()
        );
        assignRole(user, ROLE_TREKKER);
        trekkerProfileService.createProfile(user, request.trekkingExperience(), request.citizenIdImageUrl());

        return issueOtp(user, AuthOtpPurpose.REGISTER_VERIFY, false);
    }

    @Transactional
    public OtpChallengeResponse registerTourProvider(RegisterTourProviderRequest request) {
        validatePasswords(request.password(), request.confirmPassword());
        ensureEmailAvailable(request.email());

        User user = createLocalUser(
                request.fullName(),
                request.email(),
                request.phone(),
                request.dateOfBirth(),
                request.password()
        );
        assignRole(user, ROLE_TOUR_PROVIDER);
        createTourProviderProfile(user, request);

        return issueOtp(user, AuthOtpPurpose.REGISTER_VERIFY, false);
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        if (request.purpose() != AuthOtpPurpose.REGISTER_VERIFY) {
            throw new IllegalArgumentException("OTP verification is only available for registration");
        }

        User user = getLocalUserByEmail(request.email());
        if (STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("User is already verified");
        }

        AuthOtpToken token = validateOtp(user, request.purpose(), request.otp());
        token.setUsedAt(now());
        authOtpTokenRepository.save(token);

        user.setStatus(STATUS_ACTIVE);
        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional
    public OtpChallengeResponse resendOtp(ResendOtpRequest request) {
        if (request.purpose() != AuthOtpPurpose.REGISTER_VERIFY) {
            throw new IllegalArgumentException("OTP resend is only available for registration verification");
        }

        User user = getLocalUserByEmail(request.email());
        if (STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("User is already verified");
        }

        return issueOtp(user, request.purpose(), true);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(user1 -> AUTH_PROVIDER_LOCAL.equalsIgnoreCase(user1.getAuthProvider()))
                .filter(user1 -> STATUS_ACTIVE.equalsIgnoreCase(user1.getStatus()))
                .orElse(null);

        if (user == null) {
            return;
        }

        issueOtp(user, AuthOtpPurpose.PASSWORD_RESET, true);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validatePasswords(request.newPassword(), request.confirmPassword());

        User user = getLocalUserByEmail(request.email());
        ensureUserVerified(user);

        AuthOtpToken token = validateOtp(user, AuthOtpPurpose.PASSWORD_RESET, request.otp());
        token.setUsedAt(now());
        authOtpTokenRepository.save(token);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
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
        String requestedRole = request.role().trim().toUpperCase(Locale.ROOT);
        if (!SELECTABLE_ROLES.contains(requestedRole)) {
            throw new IllegalArgumentException("Only TREKKER or TOUR_PROVIDER can be selected by users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (Boolean.TRUE.equals(user.getRoleSelected())) {
            throw new IllegalArgumentException("Role already selected");
        }

        assignRole(user, requestedRole);
        user.setRoleSelected(true);
        User savedUser = userRepository.save(user);
        return userService.toResponse(savedUser);
    }

    private User createUser(GoogleUserInfo googleUserInfo) {
        User user = new User();
        user.setGoogleId(googleUserInfo.googleId());
        user.setEmail(normalizeEmail(googleUserInfo.email()));
        user.setFullName(googleUserInfo.fullName());
        user.setAvatarUrl(googleUserInfo.avatarUrl());
        user.setStatus(STATUS_ACTIVE);
        user.setRoleSelected(false);
        user.setAuthProvider(AUTH_PROVIDER_GOOGLE);
        return userRepository.save(user);
    }

    private User findOrCreateUser(GoogleUserInfo googleUserInfo) {
        return userRepository.findByEmail(normalizeEmail(googleUserInfo.email()))
                .map(existingUser -> linkOrValidateGoogleAccount(existingUser, googleUserInfo))
                .orElseGet(() -> createUser(googleUserInfo));
    }

    private User linkOrValidateGoogleAccount(User existingUser, GoogleUserInfo googleUserInfo) {
        String existingGoogleId = existingUser.getGoogleId();

        boolean canLinkGoogleAccount =
                existingGoogleId == null
                        || existingGoogleId.isBlank();

        if (canLinkGoogleAccount) {
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

    private User createLocalUser(String fullName, String email, String phone, java.time.LocalDate dateOfBirth, String password) {
        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizeEmail(email));
        user.setPhone(phone.trim());
        user.setDateOfBirth(dateOfBirth);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(STATUS_PENDING_VERIFICATION);
        user.setAuthProvider(AUTH_PROVIDER_LOCAL);
        user.setRoleSelected(true);
        return userRepository.save(user);
    }

    private void createTourProviderProfile(User user, RegisterTourProviderRequest request) {
        TourProvider tourProvider = new TourProvider();
        tourProvider.setUser(user);
        tourProvider.setCompanyName(request.companyName().trim());
        tourProvider.setDescription(request.description());
        tourProvider.setBusinessLicenseUrl(request.businessLicenseUrl());
        tourProvider.setCitizenIdImageUrl(request.citizenIdImageUrl());
        tourProvider.setPhone(request.phone().trim());
        tourProvider.setEmail(normalizeEmail(request.email()));
        tourProvider.setAddress(request.address());
        tourProvider.setStatus(STATUS_PENDING);
        tourProviderRepository.save(tourProvider);
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), userService.toResponse(user));
    }

    private OtpChallengeResponse issueOtp(User user, AuthOtpPurpose purpose, boolean isResend) {
        LocalDateTime currentTime = now();
        AuthOtpToken previousToken = authOtpTokenRepository
                .findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, purpose)
                .orElse(null);

        int resendCount = 0;
        if (previousToken != null) {
            if (previousToken.getCreatedAt().plusSeconds(OTP_RESEND_COOLDOWN_SECONDS).isAfter(currentTime)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OTP resend cooldown is active");
            }
            if (previousToken.getResendCount() >= OTP_MAX_RESENDS) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Maximum OTP resends exceeded");
            }

            resendCount = isResend ? previousToken.getResendCount() + 1 : previousToken.getResendCount();
            previousToken.setUsedAt(currentTime);
            authOtpTokenRepository.save(previousToken);
        }

        String otp = generateOtp();
        AuthOtpToken token = new AuthOtpToken();
        token.setUser(user);
        token.setPurpose(purpose);
        token.setTokenHash(passwordEncoder.encode(otp));
        token.setExpiresAt(currentTime.plusMinutes(OTP_EXPIRY_MINUTES));
        token.setAttemptCount(0);
        token.setResendCount(isResend ? resendCount : 0);

        AuthOtpToken savedToken = authOtpTokenRepository.save(token);
        otpNotificationService.sendOtpEmail(user.getEmail(), otp, purpose, savedToken.getExpiresAt());
        return new OtpChallengeResponse(
                user.getEmail(),
                purpose,
                savedToken.getExpiresAt(),
                currentTime.plusSeconds(OTP_RESEND_COOLDOWN_SECONDS)
        );
    }

    private AuthOtpToken validateOtp(User user, AuthOtpPurpose purpose, String otp) {
        AuthOtpToken token = authOtpTokenRepository
                .findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, purpose)
                .orElseThrow(() -> new IllegalArgumentException(OTP_INVALID_MESSAGE));

        LocalDateTime currentTime = now();
        if (token.getExpiresAt().isBefore(currentTime)) {
            token.setUsedAt(currentTime);
            authOtpTokenRepository.save(token);
            throw new IllegalArgumentException("OTP has expired");
        }

        if (token.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
            token.setUsedAt(currentTime);
            authOtpTokenRepository.save(token);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Maximum OTP attempts exceeded");
        }

        if (!passwordEncoder.matches(otp, token.getTokenHash())) {
            token.setAttemptCount(token.getAttemptCount() + 1);
            if (token.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
                token.setUsedAt(currentTime);
                authOtpTokenRepository.save(token);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Maximum OTP attempts exceeded");
            }

            authOtpTokenRepository.save(token);
            throw new IllegalArgumentException("OTP is incorrect");
        }

        return token;
    }

    private void assignRole(User user, String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException("Required role is missing"));

        if (!userRoleRepository.existsByUserAndRole(user, role)) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
            user.getUserRoles().add(userRole);
        }
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmail(normalizeEmail(email))) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    private void validatePasswords(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
    }

    private User getLocalUserByEmail(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!AUTH_PROVIDER_LOCAL.equalsIgnoreCase(Objects.toString(user.getAuthProvider(), ""))) {
            throw new IllegalArgumentException(GOOGLE_LOGIN_ONLY_MESSAGE);
        }

        return user;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureUserVerified(User user) {
        if (!STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, EMAIL_VERIFICATION_REQUIRED_MESSAGE);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int min = bound / 10;
        int value = secureRandom.nextInt(bound - min) + min;
        return String.valueOf(value);
    }
}
