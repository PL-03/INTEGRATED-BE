package com.pl03.kanban.services.impl;

import com.pl03.kanban.utils.JwtTokenUtils;
import com.pl03.kanban.dtos.UserDto;
import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.kanban_entities.Users;
import com.pl03.kanban.kanban_entities.Repositories.UsersRepository;
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
import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository; // shared user db
    private final UsersRepository usersRepository; // our own user db
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
            User user = userRepository.findByUsername(loginRequest.getUserName());

            if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                throw new AuthenticationException("The username or password is incorrect.");
            }

            String accessToken = jwtTokenUtils.generateAccessToken(user);
            String refreshToken = jwtTokenUtils.generateRefreshToken(user);

            Users newUser = usersRepository.findByOid(user.getOid()).orElse(null);

            if (newUser == null) {
                newUser = createNewUser(user, loginRequest);
            }

            Map<String, String> tokens = new HashMap<>();
            tokens.put("access_token", accessToken);
            tokens.put("refresh_token", refreshToken);

            return ResponseEntity.ok(tokens);

        } catch (AuthenticationException ex) {
            // Handle the authentication error
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), request.getRequestURI());
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public ResponseEntity<?> refreshToken(String refreshToken) {
        if (!jwtTokenUtils.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        Map<String, Object> claims = jwtTokenUtils.getClaimsFromToken(refreshToken);
        String oid = (String) claims.get("oid");
        User user = userRepository.findByOid(oid);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        String newAccessToken = jwtTokenUtils.generateAccessToken(user);
        return ResponseEntity.ok(Map.of("access_token", newAccessToken));
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



