package com.pl03.kanban.controllers;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.FileUploadResponse;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.dtos.TaskDetailDto;
import com.pl03.kanban.kanban_entities.Repositories.BoardRepository;
import com.pl03.kanban.services.FileStorageService;
import com.pl03.kanban.services.TaskV3Service;
import com.pl03.kanban.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:5173",
        "http://intproj23.sit.kmutt.ac.th",
        "http://intproj23.sit.kmutt.ac.th/pl3",
        "http://intproj23.sit.kmutt.ac.th/pl3/status",
        "http://ip23pl3.sit.kmutt.ac.th",
        "https://intproj23.sit.kmutt.ac.th",
        "https://intproj23.sit.kmutt.ac.th/pl3",
        "https://intproj23.sit.kmutt.ac.th/pl3/status",
        "https://ip23pl3.sit.kmutt.ac.th"})
@RestController
@RequestMapping("/v3/boards/{boardId}/tasks")
public class TaskController {
    private final TaskV3Service taskV3Service;
    private final JwtTokenUtils jwtTokenUtils;

    private final BoardRepository boardRepository;

    private final FileStorageService fileStorageService;

    @Autowired
    public TaskController(TaskV3Service taskV3Service, JwtTokenUtils jwtTokenUtils, BoardRepository boardRepository, FileStorageService fileStorageService) {
        this.taskV3Service = taskV3Service;
        this.jwtTokenUtils = jwtTokenUtils;
        this.boardRepository = boardRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ResponseEntity<List<GetAllTaskDto>> getAllTasks(
            @PathVariable String boardId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) List<String> filterStatuses,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = null; //for public access
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenUtils.validateToken(token)) {
                userId = getUserIdFromToken(token);
            }
        }


        List<GetAllTaskDto> allTasks = taskV3Service.getAllTasks(boardId, sortBy, filterStatuses, userId);
        return ResponseEntity.status(HttpStatus.OK).body(allTasks);
    }

    @PostMapping
    public ResponseEntity<AddEditTaskDto> createTask(
            @PathVariable String boardId,
            @RequestBody(required = false) AddEditTaskDto addEditTaskDto,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String userId = getUserIdFromToken(token);

        AddEditTaskDto createdTask = taskV3Service.createTask(boardId, addEditTaskDto, userId);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDetailDto> getTaskById(@PathVariable String boardId, @PathVariable int taskId,
                                                     @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = null; //for public access
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenUtils.validateToken(token)) {
                userId = getUserIdFromToken(token);
            }
        }

        TaskDetailDto taskDto = taskV3Service.getTaskById(boardId, taskId, userId);
        return ResponseEntity.ok(taskDto);
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<Object> updateTask(
            @PathVariable String boardId,
            @PathVariable int taskId,
            @RequestPart(value = "task", required = false) AddEditTaskDto addEditTaskDto,
            @RequestPart(value = "filesToDelete", required = false) List<Long> filesToDelete,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String userId = getUserIdFromToken(token);

        // Set files to delete and new files in DTO
        if (addEditTaskDto == null) {
            addEditTaskDto = new AddEditTaskDto();
        }
        addEditTaskDto.setFilesToDelete(filesToDelete);
        addEditTaskDto.setNewFiles(files);

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
