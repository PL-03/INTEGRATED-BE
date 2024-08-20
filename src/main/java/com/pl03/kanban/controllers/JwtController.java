package com.pl03.kanban.controllers;


import com.pl03.kanban.configs.JwtToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwtController {

    private final JwtToken jwtTokenUtil;

    @Autowired
    public JwtController(JwtToken jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @GetMapping("/decode-token")
    public Map<String, Object> decodeToken(@RequestHeader("Authorization") String token) {
        // Remove the "Bearer " prefix from the token
        token = token.substring(7);

        return jwtTokenUtil.getClaimsFromToken(token);
    }
}
