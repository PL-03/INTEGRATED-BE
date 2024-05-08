package com.pl03.kanban.controllers;

import com.pl03.kanban.dtos.StatusDto;
import com.pl03.kanban.services.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/v2")
public class StatusController {

    private final StatusService statusService;

    @Autowired
    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<StatusDto>> getAllStatuses() {
        List<StatusDto> statuses = statusService.getAllStatuses();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/statuses/{id}")
    public ResponseEntity<StatusDto> getStatusById(@PathVariable int id) {
        StatusDto status = statusService.getStatusById(id);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/statuses")
    public ResponseEntity<StatusDto> createStatus(@RequestBody StatusDto statusDto) {
        StatusDto createdStatus = statusService.createStatus(statusDto);
        return new ResponseEntity<>(createdStatus, HttpStatus.CREATED);
    }
}