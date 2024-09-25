package com.pl03.kanban.utils;

import com.pl03.kanban.user_entities.User;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenUtils {

    @Value("${jwt.secret}")
    private String SECRET_KEY;
//    SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;



    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", user.getName());
        claims.put("oid", user.getOid());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole());
        return createToken(claims, user.getUsername(), expiration);
    }

//    public String generateToken(User user) {
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("name", user.getName());
//        claims.put("oid", user.getOid());
//        claims.put("email", user.getEmail());
//        claims.put("role", user.getRole());
//
//        // 30 minutes
//        long expiration = 1800L;
//        return Jwts.builder()
//                .setHeaderParam("typ", "JWT") //set header according to postman requirements
//                .setClaims(claims)
//                .setIssuer("https://intproj23.sit.kmutt.ac.th/pl3/")
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
//                .signWith(signatureAlgorithm, SECRET_KEY)
//                .compact();
//    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "https://intproj23.sit.kmutt.ac.th/pl3/");
        claims.put("oid", user.getOid());
        return createToken(claims, user.getUsername(), refreshExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .setHeaderParam("typ", "JWT") //set header according to postman requirements
                .setClaims(claims)
                .setIssuer("https://intproj23.sit.kmutt.ac.th/pl3/")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

//    public Claims getClaimsFromToken(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(SECRET_KEY)
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//    }
//
//    public boolean validateToken(String token) {
//        try {
//            Jwts.parserBuilder()
//                    .setSigningKey(SECRET_KEY)
//                    .build()
//                    .parseClaimsJws(token);
//            return true;
//        } catch (MalformedJwtException e) {
//            // Log the specific error for better debugging
//            System.out.println("Malformed JWT: " + e.getMessage());
//        } catch (ExpiredJwtException e) {
//            System.out.println("Expired JWT: " + e.getMessage());
//        } catch (UnsupportedJwtException e) {
//            System.out.println("Unsupported JWT: " + e.getMessage());
//        } catch (IllegalArgumentException e) {
//            System.out.println("JWT validation failed: " + e.getMessage());
//        }
//        // Token validation failed
//        return false;
//    }
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Map<String, Object> getClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }



}
