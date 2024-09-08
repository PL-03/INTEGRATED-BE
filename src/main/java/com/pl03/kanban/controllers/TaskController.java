package com.pl03.kanban.controllers;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.kanban_entities.TaskV2;
import com.pl03.kanban.services.TaskV2Service;
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
@RequestMapping("/v3/boards/{id}/tasks")
public class TaskController {
    private final TaskV2Service taskV2Service;

    @Autowired
    public TaskController(TaskV2Service taskV2Service) {
        this.taskV2Service = taskV2Service;
    }

    @GetMapping
    public ResponseEntity<List<GetAllTaskDto>> getAllTasks(
            @PathVariable String id,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) List<String> filterStatuses) {
        List<GetAllTaskDto> allTasks = taskV2Service.getAllTasks(id, sortBy, filterStatuses);
        return ResponseEntity.status(HttpStatus.OK).body(allTasks);
    }

    @PostMapping
    public ResponseEntity<AddEditTaskDto> createTask(@PathVariable String id, @RequestBody AddEditTaskDto addEditTaskDto) {
        AddEditTaskDto createdTask = taskV2Service.createTask(id, addEditTaskDto);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskV2> getTaskById(@PathVariable String id, @PathVariable int taskId) {
        TaskV2 task = taskV2Service.getTaskById(id, taskId);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<Object> updateTask(@PathVariable String id, @PathVariable int taskId, @RequestBody AddEditTaskDto addEditTaskDto) {
        AddEditTaskDto response = taskV2Service.updateTask(id, taskId, addEditTaskDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<AddEditTaskDto> deleteTask(@PathVariable String id, @PathVariable int taskId) {
        AddEditTaskDto addEditTaskById = taskV2Service.deleteTaskById(id, taskId);
        return ResponseEntity.ok(addEditTaskById);
    }
}
