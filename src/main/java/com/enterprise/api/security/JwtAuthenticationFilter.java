package com.enterprise.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that processes JWT tokens from HTTP requests.
 * Validates tokens and sets up Spring Security authentication context.
 * 
 * Requirements addressed:
 * - 4.1: JWT token processing for request authentication
 * - 4.2: Token validation and security context setup
 * - 4.3: Integration with Spring Security filter chain
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, 
                                   CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Processes each HTTP request to extract and validate JWT tokens.
     * Sets up authentication context if token is valid.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                authenticateUser(jwt, request);
            } else if (StringUtils.hasText(jwt)) {
                logger.debug("Invalid JWT token in request to: {}", request.getRequestURI());
            }
        } catch (Exception ex) {
            logger.error("Cannot set user authentication in security context", ex);
            // Clear any existing authentication
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from the Authorization header.
     * 
     * @param request the HTTP request
     * @return the JWT token or null if not present or invalid format
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX_LENGTH);
        }
        
        return null;
    }

    /**
     * Authenticates the user based on the JWT token.
     * Loads user details and sets up Spring Security authentication context.
     * 
     * @param jwt the JWT token
     * @param request the HTTP request
     */
    private void authenticateUser(String jwt, HttpServletRequest request) {
        try {
            // Extract user ID from token for more efficient lookup
            Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
            
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                
                // Validate that the user account is still active
                if (customUserDetailsService.isAccountValid(userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, 
                                    null, 
                                    userDetails.getAuthorities());
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    logger.debug("User authenticated successfully: {} for request: {}", 
                            userDetails.getUsername(), request.getRequestURI());
                } else {
                    logger.warn("User account is not valid: {} for request: {}", 
                            userDetails.getUsername(), request.getRequestURI());
                }
            }
        } catch (Exception ex) {
            logger.error("Failed to authenticate user from JWT token", ex);
            throw ex;
        }
    }

    /**
     * Determines if this filter should be applied to the request.
     * Can be overridden to skip certain paths or request types.
     * 
     * @param request the HTTP request
     * @return true if the filter should NOT be applied, false otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip JWT processing for public endpoints
        return isPublicEndpoint(path);
    }

    /**
     * Checks if the request path is a public endpoint that doesn't require authentication.
     * 
     * @param path the request path
     * @return true if the path is public, false otherwise
     */
    private boolean isPublicEndpoint(String path) {
        // Define public endpoints that don't require JWT authentication
        String[] publicPaths = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/captcha",
            "/actuator/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/favicon.ico",
            "/error"
        };
        
        for (String publicPath : publicPaths) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Logs authentication details for debugging purposes.
     * 
     * @param request the HTTP request
     * @param jwt the JWT token
     * @param authenticated whether authentication was successful
     */
    private void logAuthenticationAttempt(HttpServletRequest request, String jwt, boolean authenticated) {
        if (logger.isDebugEnabled()) {
            String username = null;
            try {
                username = jwtTokenProvider.getUsernameFromToken(jwt);
            } catch (Exception e) {
                username = "unknown";
            }
            
            logger.debug("JWT Authentication attempt - User: {}, Path: {}, Method: {}, Success: {}", 
                    username, request.getRequestURI(), request.getMethod(), authenticated);
        }
    }
}