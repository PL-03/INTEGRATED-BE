package com.pl03.kanban.services.impl;

import com.pl03.kanban.configs.JwtToken;
import com.pl03.kanban.dtos.UserDto;
import com.pl03.kanban.services.UserService;
import com.pl03.kanban.user_entities.User;
import com.pl03.kanban.user_entities.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtToken jwtTokenUtil;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtToken jwtTokenUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public ResponseEntity<?> login(UserDto loginRequest) {
        List<String> errors = new ArrayList<>();

        // Validate username
        if (loginRequest.getUserName() == null || loginRequest.getUserName().isEmpty()) {
            errors.add("Username cannot be empty");
        } else if (loginRequest.getUserName().length() > 50) {
            errors.add("Username must be at most 50 characters long");
        }

        // Validate password
        if (loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            errors.add("Password cannot be empty");
        } else if (loginRequest.getPassword().length() > 14) {
            errors.add("Password must be at most 14 characters long");
        }

        // If there are validation errors, return them all
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(String.join(", ", errors));
        }

        // If validation passes, proceed with authentication
        User user = userRepository.findByUsername(loginRequest.getUserName());

        if (user != null && passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            // Generate JWT token
            String token = jwtTokenUtil.generateToken(user);
            return ResponseEntity.ok(Map.of("access_token", token));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The username or password is incorrect.");
        }
    }
}

