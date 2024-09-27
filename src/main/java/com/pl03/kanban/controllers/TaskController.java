package com.pl03.kanban.controllers;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.dtos.TaskDetailDto;
import com.pl03.kanban.services.TaskV3Service;
import com.pl03.kanban.utils.JwtTokenUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:5173",
        "http://intproj23.sit.kmutt.ac.th",
        "http://intproj23.sit.kmutt.ac.th/pl3",
        "http://intproj23.sit.kmutt.ac.th/pl3/status",
        "http://ip23pl3.sit.kmutt.ac.th"})
@RestController
@RequestMapping("/v3/boards/{boardId}/tasks")
public class TaskController {
    private final TaskV3Service taskV3Service;
    private final JwtTokenUtils jwtTokenUtils;

    @Autowired
    public TaskController(TaskV3Service taskV3Service, JwtTokenUtils jwtTokenUtils) {
        this.taskV3Service = taskV3Service;
        this.jwtTokenUtils = jwtTokenUtils;
    }

    @GetMapping
    public ResponseEntity<List<GetAllTaskDto>> getAllTasks(
            @PathVariable String boardId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) List<String> filterStatuses,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = getUserIdFromToken(token);
        List<GetAllTaskDto> allTasks = taskV3Service.getAllTasks(boardId, sortBy, filterStatuses, userId);
        return ResponseEntity.status(HttpStatus.OK).body(allTasks);
    }

    @PostMapping
    public ResponseEntity<AddEditTaskDto> createTask(
            @PathVariable String boardId,
            @RequestBody AddEditTaskDto addEditTaskDto,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = getUserIdFromToken(token);
        AddEditTaskDto createdTask = taskV3Service.createTask(boardId, addEditTaskDto, userId);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDetailDto> getTaskById(@PathVariable String boardId, @PathVariable int taskId,
                                                     @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = getUserIdFromToken(token);
        TaskDetailDto taskDto = taskV3Service.getTaskById(boardId, taskId, userId);
        return ResponseEntity.ok(taskDto);
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<Object> updateTask(@PathVariable String boardId, @PathVariable int taskId,
                                             @RequestBody AddEditTaskDto addEditTaskDto,
                                             @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String userId = getUserIdFromToken(token);
        AddEditTaskDto response = taskV3Service.updateTask(boardId, taskId, addEditTaskDto, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<AddEditTaskDto> deleteTask(@PathVariable String boardId, @PathVariable int taskId,
                                                     @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String userId = getUserIdFromToken(token);
        AddEditTaskDto addEditTaskById = taskV3Service.deleteTaskById(boardId, taskId, userId);
        return ResponseEntity.ok(addEditTaskById);
    }

    private String getUserIdFromToken(String token) {
        Map<String, Object> claims = jwtTokenUtils.getClaimsFromToken(token);
        return (String) claims.get("oid");
    }
}
