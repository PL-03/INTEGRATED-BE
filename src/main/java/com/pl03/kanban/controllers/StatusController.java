package com.pl03.kanban.controllers;

import com.pl03.kanban.dtos.StatusDto;
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
    public ResponseEntity<StatusDto> createStatus(@PathVariable String boardId, @RequestBody StatusDto status,
                                                  @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = getUserIdFromToken(token);
        StatusDto createdStatus = statusService.createStatus(boardId, status, userId);
        return new ResponseEntity<>(createdStatus, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<StatusDto>> getAllStatuses(@PathVariable String boardId, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = getUserIdFromToken(token);
        List<StatusDto> statuses = statusService.getAllStatuses(boardId, userId);
        return new ResponseEntity<>(statuses, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatusDto> getStatusById(@PathVariable String boardId, @PathVariable int id,
                                                   @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = getUserIdFromToken(token);
        StatusDto status = statusService.getStatusById(boardId, id, userId);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StatusDto> updateStatus(@PathVariable String boardId, @PathVariable int id,
                                                  @RequestBody StatusDto status, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = getUserIdFromToken(token);
        StatusDto updatedStatus = statusService.updateStatus(boardId, id, status, userId);
        return new ResponseEntity<>(updatedStatus, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StatusDto> deleteStatus(@PathVariable String boardId, @PathVariable int id,
                                                  @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = getUserIdFromToken(token);
        StatusDto deletedStatus = statusService.deleteStatus(boardId, id, userId);
        return new ResponseEntity<>(deletedStatus, HttpStatus.OK);
    }

    @DeleteMapping("/{id}/{newStatusId}")
    public ResponseEntity<Void> deleteStatusAndTransferTasks(@PathVariable String boardId, @PathVariable int id,
                                                             @PathVariable int newStatusId, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtTokenUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = getUserIdFromToken(token);
        statusService.deleteStatusAndTransferTasks(boardId, id, newStatusId, userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String getUserIdFromToken(String token) {
        Map<String, Object> claims = jwtTokenUtils.getClaimsFromToken(token);
        return (String) claims.get("oid");
    }
}