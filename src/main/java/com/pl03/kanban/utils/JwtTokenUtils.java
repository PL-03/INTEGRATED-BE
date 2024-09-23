package com.pl03.kanban.utils;

import com.pl03.kanban.user_entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import javax.crypto.SecretKey;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.security.Key;
import io.jsonwebtoken.io.Decoders;
@Component
public class JwtTokenUtils {

    @Value("${jwt.secret}")
    private String SECRET_KEY;
    SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

//    private SecretKey secretKey;


//    @PostConstruct
//    public void init() {
//        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
//
//        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
//
//        System.out.println(encodedKey);
//    }

//    private Key key() {
//        return Keys.hmacShaKeyFor(Decoders.BASE64.decode("test"));
//    }

    public String generateToken(User user) {
        Map<String, Object> information = new HashMap<>();
        information.put("name", user.getName());
        information.put("oid", user.getOid());
        information.put("email", user.getEmail());
        information.put("role", user.getRole());

        // 30 minutes
        long expiration = 1800L;
        return Jwts.builder()
                .setHeaderParam("typ", "JWT") //set header according to postman requirements
                .setClaims(information)
                .setIssuer("https://intproj23.sit.kmutt.ac.th/pl3/")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(signatureAlgorithm, SECRET_KEY)
                .compact();
    }
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            // Log the specific error for better debugging
            System.out.println("Malformed JWT: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.out.println("Expired JWT: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("Unsupported JWT: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("JWT validation failed: " + e.getMessage());
        }
        // Token validation failed
        return false;
    }



}
