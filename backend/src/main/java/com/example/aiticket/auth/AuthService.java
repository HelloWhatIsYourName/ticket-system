package com.example.aiticket.auth;

import com.example.aiticket.security.JwtService;
import com.example.aiticket.system.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final String LOGIN_FAILURE_MESSAGE = "用户名或密码错误";

    private final UserAuthorityService authorityService;
    private final UserSecurityMapper mapper;
    private final AuditLogService auditLogService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAuthorityService authorityService,
                       UserSecurityMapper mapper,
                       AuditLogService auditLogService,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.authorityService = authorityService;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        UserAuthoritySnapshot snapshot = authorityService.loadByUsername(request.username())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.user().passwordHash()))
                .orElseThrow(() -> {
                    auditLogService.record(null, request.username(), "AUTH_LOGIN_FAILURE", "AUTH", null,
                            "FAILURE", "login failure", ipAddress, userAgent);
                    return new InvalidLoginException(LOGIN_FAILURE_MESSAGE);
                });

        UserAccount user = snapshot.user();
        mapper.updateLastLoginAt(user.id());
        auditLogService.record(user.id(), user.username(), "AUTH_LOGIN_SUCCESS", "AUTH", user.id().toString(),
                "SUCCESS", "login success", ipAddress, userAgent);
        String token = jwtService.createAccessToken(user.id(), user.username(), user.tokenVersion());

        return new LoginResponse(
                "Bearer",
                token,
                jwtService.expiresInSeconds(),
                new CurrentUserResponse.UserSummary(user.id(), user.username(), user.displayName()),
                snapshot.roles(),
                snapshot.permissions(),
                snapshot.menus()
        );
    }

    public CurrentUserResponse currentUser(Long userId) {
        UserAuthoritySnapshot snapshot = authorityService.loadByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在或已被禁用"));
        UserAccount user = snapshot.user();
        return new CurrentUserResponse(
                new CurrentUserResponse.UserSummary(user.id(), user.username(), user.displayName()),
                snapshot.roles(),
                snapshot.permissions(),
                snapshot.menus()
        );
    }
}
