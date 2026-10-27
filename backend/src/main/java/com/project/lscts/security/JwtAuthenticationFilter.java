
package com.project.lscts.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();

        String authHeader = request.getHeader("Authorization");

        // DEBUG: Identify the request reaching this filter.
        logger.info("JWT FILTER URI: {}", requestUri);
        logger.info("JWT FILTER METHOD: {}", requestMethod);
        logger.info(
                "AUTH HEADER PRESENT: {}",
                authHeader != null
        );

        // No Bearer token: continue without JWT authentication.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            logger.info(
                    "No Bearer token. Continuing filter chain for {}",
                    requestUri
            );

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractUsername(token);

            if (email != null
                    && SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                if (userDetails.isEnabled()
                        && jwtService.isTokenValid(token, userDetails)) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                    logger.info("Authenticated user: {}",
                            userDetails.getUsername());

                    logger.info("User authorities: {}",
                            userDetails.getAuthorities());

                    logger.info(
                            "JWT authentication successful for request {}",
                            requestUri
                    );
                } else {
                    logger.warn(
                            "JWT validation failed for request {}",
                            requestUri
                    );
                }
            }

        } catch (JwtException | IllegalArgumentException ex) {

            logger.warn(
                    "Invalid or expired JWT for request {}",
                    requestUri
            );

            SecurityContextHolder.clearContext();

        } catch (UsernameNotFoundException ex) {

            logger.warn(
                    "JWT user not found for request {}",
                    requestUri
            );

            SecurityContextHolder.clearContext();
        }

        // Continue to the next filter.
        filterChain.doFilter(request, response);
    }
}
