package com.pl03.kanban.services.impl;

import com.pl03.kanban.configs.JwtToken;
import com.pl03.kanban.dtos.UserDto;
import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.services.UserService;
import com.pl03.kanban.user_entities.User;
import com.pl03.kanban.user_entities.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        List<ErrorResponse.ValidationError> errors = new ArrayList<>();

        // Manual validation for username
        if (loginRequest.getUserName() == null || loginRequest.getUserName().isEmpty()) {
            errors.add(new ErrorResponse.ValidationError("userName", "Username cannot be empty"));
        } else if (loginRequest.getUserName().length() > 50) {
            errors.add(new ErrorResponse.ValidationError("userName", "Username must be at most 50 characters long"));
        }

        // Manual validation for password
        if (loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            errors.add(new ErrorResponse.ValidationError("password", "Password cannot be empty"));
        } else if (loginRequest.getPassword().length() > 14) {
            errors.add(new ErrorResponse.ValidationError("password", "Password must be at most 14 characters long"));
        }

        // If there are validation errors, return them all
        if (!errors.isEmpty()) {
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation error. Check 'errors' field for details", request.getRequestURI());
            errorResponse.setErrors(errors);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        // If validation passes, proceed with authentication
        User user = userRepository.findByUsername(loginRequest.getUserName());

        if (user != null && passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            // Generate JWT token
            String token = jwtTokenUtil.generateToken(user);
            return ResponseEntity.ok(Map.of("access_token", token));
        } else {
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "The username or password is incorrect.", request.getRequestURI());
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    }
}
