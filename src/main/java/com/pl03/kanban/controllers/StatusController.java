package com.pl03.kanban.controllers;

import com.pl03.kanban.entities.Status;
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
@RequestMapping
public class StatusController {

    private final StatusService statusService;

    @Autowired
    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<Status>> getAllStatuses() {
        List<Status> statuses = statusService.getAllStatuses();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/statuses/{id}")
    public ResponseEntity<Status> getStatusById(@PathVariable int id) {
        Status status = statusService.getStatusById(id);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/statuses")
    public ResponseEntity<Status> createStatus(@RequestBody Status status) {
        Status createdStatus = statusService.createStatus(status);
        return new ResponseEntity<>(createdStatus, HttpStatus.CREATED);
    }

    @PutMapping("/statuses/{id}")
    public ResponseEntity<Status> updateStatus(@RequestBody Status status, @PathVariable int id) {
        Status updatedStatus = statusService.updateStatus(id, status);
        return ResponseEntity.ok(updatedStatus);
    }

    @DeleteMapping("/statuses/{id}")
    public ResponseEntity<Status> deleteStatus(@PathVariable int id) {
        Status deletedStatus = statusService.deleteStatus(id);
        return ResponseEntity.ok(deletedStatus);
    }

    @DeleteMapping("/statuses/{id}/{newId}")
    public ResponseEntity<Object> deleteStatusAndTransferTasks(@PathVariable int id, @PathVariable int newId) {
        statusService.deleteStatusAndTransferTasks(id, newId);
        return ResponseEntity.ok("[]");
    }
}