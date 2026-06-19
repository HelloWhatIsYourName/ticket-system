package com.example.aiticket.security;

import com.example.aiticket.system.UserAccount;
import com.example.aiticket.system.UserAuthorityService;
import com.example.aiticket.system.UserAuthoritySnapshot;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserAuthorityService authorityService;

    public JwtAuthenticationFilter(JwtService jwtService, UserAuthorityService authorityService) {
        this.jwtService = jwtService;
        this.authorityService = authorityService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring("Bearer ".length());
        try {
            JwtClaims claims = jwtService.parse(token);
            authorityService.loadByUserId(claims.userId())
                    .filter(snapshot -> tokenVersionMatches(snapshot.user(), claims))
                    .ifPresent(this::authenticate);
        } catch (IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean tokenVersionMatches(UserAccount user, JwtClaims claims) {
        return user.tokenVersion() == claims.tokenVersion();
    }

    private void authenticate(UserAuthoritySnapshot snapshot) {
        UserAccount user = snapshot.user();
        AuthenticatedUser principal = new AuthenticatedUser(
                user.id(),
                user.username(),
                user.displayName(),
                user.tokenVersion(),
                snapshot.roles(),
                snapshot.permissions()
        );
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                snapshot.permissions().stream().map(SimpleGrantedAuthority::new).toList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
