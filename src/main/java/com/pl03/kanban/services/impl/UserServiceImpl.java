package com.pl03.kanban.services.impl;

import com.pl03.kanban.configs.JwtTokenUtils;
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

import javax.naming.AuthenticationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtils jwtTokenUtilsUtil;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenUtils jwtTokenUtilsUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtilsUtil = jwtTokenUtilsUtil;
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

        try {
            User user = userRepository.findByUsername(loginRequest.getUserName());

            if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                throw new AuthenticationException("The username or password is incorrect.");
            }

            // Generate JWT token
            String token = jwtTokenUtilsUtil.generateToken(user);
            return ResponseEntity.ok(Map.of("access_token", token));

        } catch (AuthenticationException ex) {
            // Handle the authentication error
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), request.getRequestURI());
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    }

}
