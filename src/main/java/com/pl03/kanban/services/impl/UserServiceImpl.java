package com.pl03.kanban.services.impl;

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

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ResponseEntity<String> login(UserDto loginRequest) {
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
            return ResponseEntity.ok("Login successful");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Username or Password is incorrect");
        }
    }
}
