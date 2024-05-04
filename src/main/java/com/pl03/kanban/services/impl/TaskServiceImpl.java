package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.entities.Task;
import com.pl03.kanban.exceptions.TaskNotFoundException;
import com.pl03.kanban.repositories.TaskRepository;
import com.pl03.kanban.services.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public AddEditTaskDto createTask(AddEditTaskDto addEditTaskDto) {
        Task task = mapToEntity(addEditTaskDto);
        Task savedTask = taskRepository.save(task);
        return mapToAddEditTaskDto(savedTask);
    }

    @Override
    public List<GetAllTaskDto> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        return tasks.stream()
                .map(task -> new GetAllTaskDto(
                        task.getId(),
                        task.getTitle(),
                        task.getAssignees(),
                        task.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public Task getTaskById(int id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " does not exist"));
    }

    @Override
    public AddEditTaskDto deleteTaskById(int id){
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " does not exist"));
        taskRepository.delete(task);
        return mapToAddEditTaskDto(task);
    }


    //map AddEditDto to task entity
    private Task mapToEntity(AddEditTaskDto addEditTaskDto) {
        Task task = new Task();
        task.setTitle(addEditTaskDto.getTitle());
        task.setDescription(addEditTaskDto.getDescription());
        task.setAssignees(addEditTaskDto.getAssignees());
        task.setStatus(Task.TaskStatus.valueOf(addEditTaskDto.getStatus()));
        return task;
    }

    //map Task entity to AddEditTaskDto
    private AddEditTaskDto mapToAddEditTaskDto(Task task) {
        AddEditTaskDto addEditTaskDto = new AddEditTaskDto();
        addEditTaskDto.setId(task.getId());
        addEditTaskDto.setTitle(task.getTitle());
        addEditTaskDto.setDescription(task.getDescription());
        addEditTaskDto.setAssignees(task.getAssignees());
        addEditTaskDto.setStatus(task.getStatus().toString());
        return addEditTaskDto;
    }
}