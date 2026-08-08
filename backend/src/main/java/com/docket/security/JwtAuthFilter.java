package com.docket.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Per-request filter that extracts the JWT from the Authorization header,
 * validates it, and sets the Spring Security context so downstream code
 * can access the authenticated user's identity.
 *
 * Unauthenticated paths (/api/health, /api/auth/**) are skipped — they're
 * already permitAll'd in SecurityConfig, but we short-circuit here too
 * to avoid unnecessary parsing.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.validateToken(token);

            // Build an authentication token with userId as principal and workspaceId as a detail
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            Integer.valueOf(claims.getSubject()), // principal = userId
                            null,                                  // credentials (not needed post-auth)
                            java.util.List.of()                    // authorities (MVP: no roles)
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (Exception e) {
            // Invalid/expired token — clear any existing context and let the
            // request continue; Spring Security will reject it as unauthenticated
            // if the endpoint requires auth.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/health")
                || path.equals("/api/auth/signup")
                || path.equals("/api/auth/login");
    }
}
