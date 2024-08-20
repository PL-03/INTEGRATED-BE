package com.pl03.kanban.services;

import com.pl03.kanban.dtos.UserDto;
import org.springframework.http.ResponseEntity;

public interface UserService {
    ResponseEntity<?> login(UserDto loginRequest);
}

