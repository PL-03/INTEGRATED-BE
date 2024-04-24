package com.pl03.kanban.services;

import com.pl03.kanban.dtos.AllTaskDto;
import com.pl03.kanban.models.Task;
import com.pl03.kanban.repositories.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }


    public List<AllTaskDto> getAllTasks() {
        return taskRepository.findAllTasks();
    }

}
