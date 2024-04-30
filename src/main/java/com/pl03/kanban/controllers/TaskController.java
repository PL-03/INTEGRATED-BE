package com.pl03.kanban.controllers;

import com.pl03.kanban.dtos.AddTaskDto;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.entities.Task;
import com.pl03.kanban.services.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping
public class TaskController {
    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<GetAllTaskDto>> getAllTasks() {
        List<GetAllTaskDto> allTasks = taskService.getAllTasks();
        return ResponseEntity.status(HttpStatus.OK).body(allTasks);
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<Task> taskDetail(@PathVariable int id) {
        Task task = taskService.getTaskById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public AddTaskDto addTask(@RequestBody AddTaskDto addTaskDto) {
        Task task = new Task();
        task.setTitle(addTaskDto.getTitle());
        task.setDescription(addTaskDto.getDescription());
        task.setAssignees(addTaskDto.getAssignees());
        task.setStatus(addTaskDto.getStatus() != null ? Task.TaskStatus.valueOf(addTaskDto.getStatus()) : Task.TaskStatus.NO_STATUS);
        Task savedTask = taskService.addTask(task);

        addTaskDto.setId(savedTask.getId());
        addTaskDto.setStatus(savedTask.getStatus().name());

        return addTaskDto;
    }
}
