package com.pl03.kanban.controllers;

import com.pl03.kanban.services.FileStorageService;
import com.pl03.kanban.services.TaskV3Service;
import com.pl03.kanban.services.impl.FileStorageServiceImpl;
import com.pl03.kanban.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
public class FileAttachmentController {
    private final FileStorageService fileStorageService;
    private final TaskV3Service taskV3Service;
    private final JwtTokenUtils jwtTokenUtils;
    @Autowired
    public FileAttachmentController( FileStorageService fileStorageService, TaskV3Service taskV3Service, JwtTokenUtils jwtTokenUtils) {
        this.fileStorageService = fileStorageService;
        this.taskV3Service = taskV3Service;
        this.jwtTokenUtils = jwtTokenUtils;
    }

    @GetMapping("/{taskId}/attachments/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String boardId,
            @PathVariable int taskId,
            @PathVariable String fileName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenUtils.validateToken(token)) {
                userId = getUserIdFromToken(token);
            }
        }

        // Verify access to the board/task
        taskV3Service.getTaskById(boardId, taskId, userId); // Throws exception if access is denied

        Resource resource = fileStorageService.loadFileAsResource(fileName, taskId);
        String contentType = fileStorageService.getFileContentType(resource);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{taskId}/attachments/{fileName}/preview")
    public ResponseEntity<Resource> previewFile(
            @PathVariable String boardId,
            @PathVariable int taskId,
            @PathVariable String fileName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenUtils.validateToken(token)) {
                userId = getUserIdFromToken(token);
            }
        }

        // Verify access to the board/task
        taskV3Service.getTaskById(boardId, taskId, userId); // Throws exception if access is denied

        Resource resource = fileStorageService.loadFileAsResource(fileName, taskId);
        String contentType = fileStorageService.getFileContentType(resource);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    private String getUserIdFromToken(String token) {
        Map<String, Object> claims = jwtTokenUtils.getClaimsFromToken(token);
        return (String) claims.get("oid");
    }
}
