package com.example.aiticket.auth;

import com.example.aiticket.common.api.ApiResponse;
import com.example.aiticket.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.login(request, servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(authService.currentUser(principal.id()));
    }

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("pong");
    }

    @ExceptionHandler(InvalidLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> invalidLogin(InvalidLoginException ex) {
        return ApiResponse.fail(ex.getMessage());
    }
}
