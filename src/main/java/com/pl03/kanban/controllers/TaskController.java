package com.pl03.kanban.controllers;

import com.pl03.kanban.dtos.AllTaskDto;
import com.pl03.kanban.services.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/itb-kk/v1")
public class TaskController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<AllTaskDto>> getAllTasks() {
        List<AllTaskDto> allTasks = taskService.getAllTasks();
        return ResponseEntity.status(HttpStatus.OK).body(allTasks);
    }
}
