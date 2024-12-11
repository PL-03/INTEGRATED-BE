package com.pl03.kanban.controllers;

import com.pl03.kanban.dtos.StatusDto;
import com.pl03.kanban.exceptions.InvalidStatusFieldException;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.exceptions.UnauthorizedAccessException;
import com.pl03.kanban.services.StatusService;
import com.pl03.kanban.utils.JwtTokenUtils;
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
        "http://ip23pl3.sit.kmutt.ac.th",
        "https://intproj23.sit.kmutt.ac.th",
        "https://intproj23.sit.kmutt.ac.th/pl3",
        "https://intproj23.sit.kmutt.ac.th/pl3/status",
        "https://ip23pl3.sit.kmutt.ac.th"})
@RestController
@RequestMapping("/v3/boards/{boardId}/statuses")
public class StatusController {

    private final StatusService statusService;
    private final JwtTokenUtils jwtTokenUtils;

    @Autowired
    public StatusController(StatusService statusService, JwtTokenUtils jwtTokenUtils) {
        this.statusService = statusService;
        this.jwtTokenUtils = jwtTokenUtils;
    }

    @PostMapping
    public ResponseEntity<?> createStatus(@PathVariable String boardId,
                                          @RequestBody(required = false) StatusDto status,
                                          @RequestHeader("Authorization") String authHeader) {
        String userId = validateTokenAndGetUserId(authHeader);
        StatusDto createdStatus = statusService.createStatus(boardId, status, userId);
        return new ResponseEntity<>(createdStatus, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<StatusDto>> getAllStatuses(@PathVariable String boardId,
                                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = null; //for public access
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenUtils.validateToken(token)) {
                userId = getUserIdFromToken(token);
            }
        }
        List<StatusDto> statuses = statusService.getAllStatuses(boardId, userId);
        return new ResponseEntity<>(statuses, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatusDto> getStatusById(@PathVariable String boardId, @PathVariable int id,
                                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = null; //for public access
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenUtils.validateToken(token)) {
                userId = getUserIdFromToken(token);
            }
        }
        StatusDto status = statusService.getStatusById(boardId, id, userId);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable String boardId, @PathVariable int id,
                                          @RequestBody(required = false) StatusDto status,
                                          @RequestHeader("Authorization") String authHeader) {
        String userId = validateTokenAndGetUserId(authHeader);
        StatusDto updatedStatus = statusService.updateStatus(boardId, id, status, userId);
        return new ResponseEntity<>(updatedStatus, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StatusDto> deleteStatus(@PathVariable String boardId, @PathVariable int id,
                                                  @RequestHeader("Authorization") String authHeader) {
        String userId = validateTokenAndGetUserId(authHeader);
        StatusDto deletedStatus = statusService.deleteStatus(boardId, id, userId);
        return new ResponseEntity<>(deletedStatus, HttpStatus.OK);
    }

    @DeleteMapping("/{id}/{newStatusId}")
    public ResponseEntity<Void> deleteStatusAndTransferTasks(@PathVariable String boardId, @PathVariable int id,
                                                             @PathVariable int newStatusId, @RequestHeader("Authorization") String authHeader) {
        String userId = validateTokenAndGetUserId(authHeader);
        statusService.deleteStatusAndTransferTasks(boardId, id, newStatusId, userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String validateTokenAndGetUserId(String authHeader) throws UnauthorizedAccessException {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedAccessException("Invalid Authorization header", null);
        }
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            throw new UnauthorizedAccessException("Invalid token", null);
        }
        return getUserIdFromToken(token);
    }

    private String getUserIdFromToken(String token) {
        Map<String, Object> claims = jwtTokenUtils.getClaimsFromToken(token);
        return (String) claims.get("oid");
    }
}