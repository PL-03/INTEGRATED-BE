package com.pl03.kanban.configs;

import com.pl03.kanban.user_entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;


import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtToken {

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // Generate a secure key for HS512
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

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
                .signWith(secretKey)
                .compact();
    }
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
