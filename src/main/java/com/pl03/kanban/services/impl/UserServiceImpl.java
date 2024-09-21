package com.pl03.kanban.services.impl;

import com.pl03.kanban.configs.JwtTokenUtils;
import com.pl03.kanban.dtos.UserDto;
import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.kanban_entities.Users;
import com.pl03.kanban.kanban_entities.UsersRepository;
import com.pl03.kanban.services.UserService;
import com.pl03.kanban.user_entities.User;
import com.pl03.kanban.user_entities.UserRepository;
import io.jsonwebtoken.Claims;
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

    private final UserRepository userRepository; // Existing repository for authentication
    private final UsersRepository usersRepository; // New repository for the Users table
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtils jwtTokenUtils;

    @Autowired
    public UserServiceImpl(UsersRepository usersRepository, PasswordEncoder passwordEncoder, JwtTokenUtils jwtTokenUtils, UserRepository userRepository) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtTokenUtils = jwtTokenUtils;
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
            // First, authenticate using the existing UserRepository
            User user = userRepository.findByUsername(loginRequest.getUserName());

            if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                throw new AuthenticationException("The username or password is incorrect.");
            }

            // Generate JWT token
            String token = jwtTokenUtils.generateToken(user);

            // Check if the user exists in the new Users table
            Users newUser = usersRepository.findByOid(user.getOid())
                    .orElseThrow(() -> new ItemNotFoundException("User with oid " + user.getOid() + " does not exist"));

            if (newUser == null) {
                // First-time login: create new user in the Users table
                newUser = createNewUser(user, loginRequest);
            }

            return ResponseEntity.ok(Map.of("access_token", token));

        } catch (AuthenticationException ex) {
            // Handle the authentication error
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), request.getRequestURI());
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    }

    private Users createNewUser(User authenticatedUser, UserDto loginRequest) {
        Users newUser = new Users();
        newUser.setOid(authenticatedUser.getOid());
        newUser.setUsername(authenticatedUser.getUsername());
        newUser.setName(authenticatedUser.getName());
        newUser.setEmail(authenticatedUser.getEmail());

        // Save the new user
        return usersRepository.save(newUser);
    }
}


