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
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                setErrorResponse(response, "No token provided");
                return;
            }

            String token = authHeader.substring(7);

            // Validate the token before extracting claims
            if (!jwtTokenUtils.validateToken(token)) {
                setErrorResponse(response, "Invalid or tampered token");
                return;
            }

            Map<String, Object> claims = jwtTokenUtils.getClaimsFromToken(token);
            JwtUserDetails userDetails = new JwtUserDetails(claims);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response); // Continue if token is valid
        } catch (ExpiredJwtException e) {
            setErrorResponse(response, "Token has expired");
        } catch (MalformedJwtException e) {
            setErrorResponse(response, "Token is not well-formed");
        } catch (SignatureException e) {
            setErrorResponse(response, "JWT token has been tampered with");
        } catch (IllegalArgumentException e) {
            setErrorResponse(response, "JWT token compact of handler are invalid");
        }
//        catch (Exception e) {
////            setErrorResponse(response, "An error occurred while processing the JWT");
////        }
    }

    private void setErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        ErrorResponse errorResponse = new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, message, "Authentication error");

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/login".equals(path);
    }
}