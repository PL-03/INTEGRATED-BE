package com.pl03.kanban.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.utils.JwtTokenUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

import io.jsonwebtoken.security.SignatureException;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenUtils jwtTokenUtils;

    public JwtAuthFilter(JwtTokenUtils jwtTokenUtils) {
        this.jwtTokenUtils = jwtTokenUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (shouldSkipAuthentication(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "No token provided");
                return;
            }

            String token = authHeader.substring(7);

            // Validate the token
            jwtTokenUtils.validateToken(token);

            Map<String, Object> claims = jwtTokenUtils.getClaimsFromToken(token);
            JwtUserDetails userDetails = new JwtUserDetails(claims);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
        } catch (MalformedJwtException e) {
            setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token is not well-formed");
        } catch (SignatureException e) {
            setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "JWT token has been tampered with");
        } catch (IllegalArgumentException e) {
            setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "JWT token compact of handler are invalid");
        } catch (Exception e) {
            setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "An error occurred while processing the JWT");
        }
    }

    private void setErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");

        ErrorResponse errorResponse = new ErrorResponse(status, message, "Authentication error");

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/login".equals(request.getServletPath()) || shouldSkipAuthentication(request);
    }

    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        return request.getMethod().equals("GET") && request.getServletPath().startsWith("/v3/boards");
    }
}
