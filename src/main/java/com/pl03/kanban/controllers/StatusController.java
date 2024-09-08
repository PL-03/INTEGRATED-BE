package com.pl03.kanban.controllers;

import com.pl03.kanban.kanban_entities.Status;
import com.pl03.kanban.services.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173",
                        "http://intproj23.sit.kmutt.ac.th",
                        "http://intproj23.sit.kmutt.ac.th/pl3",
                        "http://intproj23.sit.kmutt.ac.th/pl3/status",
                        "http://ip23pl3.sit.kmutt.ac.th"})
@RestController
@RequestMapping("/boards/{boardId}/statuses")
public class StatusController {

    private final StatusService statusService;

    @Autowired
    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @PostMapping
    public ResponseEntity<Status> createStatus(@PathVariable String boardId, @RequestBody Status status) {
        Status createdStatus = statusService.createStatus(boardId, status);
        return new ResponseEntity<>(createdStatus, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Status>> getAllStatuses(@PathVariable String boardId) {
        List<Status> statuses = statusService.getAllStatuses(boardId);
        return new ResponseEntity<>(statuses, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Status> getStatusById(@PathVariable String boardId, @PathVariable int id) {
        Status status = statusService.getStatusById(boardId, id);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Status> updateStatus(@PathVariable String boardId, @PathVariable int id, @RequestBody Status status) {
        Status updatedStatus = statusService.updateStatus(boardId, id, status);
        return new ResponseEntity<>(updatedStatus, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Status> deleteStatus(@PathVariable String boardId, @PathVariable int id) {
        Status deletedStatus = statusService.deleteStatus(boardId, id);
        return new ResponseEntity<>(deletedStatus, HttpStatus.OK);
    }

    @DeleteMapping("/{id}/transfer/{newStatusId}")
    public ResponseEntity<Void> deleteStatusAndTransferTasks(@PathVariable String boardId, @PathVariable int id, @PathVariable int newStatusId) {
        statusService.deleteStatusAndTransferTasks(boardId, id, newStatusId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}