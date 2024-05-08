package com.pl03.kanban.controllers;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.entities.TaskV2;
import com.pl03.kanban.services.TaskV2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/v1")
public class TaskController {
    private final TaskV2Service taskV2Service;

    @Autowired
    public TaskController(TaskV2Service taskV2Service) {
        this.taskV2Service = taskV2Service;
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<GetAllTaskDto>> getAllTasks() {
        List<GetAllTaskDto> allTasks = taskV2Service.getAllTasks();
        return ResponseEntity.status(HttpStatus.OK).body(allTasks);
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskV2> getTaskById(@PathVariable int id) {
        TaskV2 task = taskV2Service.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @PostMapping("/tasks")
    public ResponseEntity<AddEditTaskDto> createTask(@RequestBody AddEditTaskDto addEditTaskDto) {
        AddEditTaskDto createdTask = taskV2Service.createTask(addEditTaskDto);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<AddEditTaskDto> updateTask(@RequestBody AddEditTaskDto addEditTaskDto, @PathVariable("id") int taskId) {
        AddEditTaskDto response = taskV2Service.updateTask(addEditTaskDto, taskId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<AddEditTaskDto> deleteTask(@PathVariable int id) {
        AddEditTaskDto addEditTaskById = taskV2Service.deleteTaskById(id);
        return ResponseEntity.ok(addEditTaskById);
    }
}