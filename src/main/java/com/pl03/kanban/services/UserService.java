package com.pl03.kanban.services;

import org.springframework.http.ResponseEntity;
import com.pl03.kanban.dtos.UserDto;

public interface UserService {
    ResponseEntity<String> login(UserDto loginRequest);
}
