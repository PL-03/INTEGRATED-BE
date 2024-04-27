package com.pl03.kanban.services;

import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.entities.Task;
import com.pl03.kanban.repositories.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }


    public List<GetAllTaskDto> getAllTasks() {
        return taskRepository.findAllTasks();
    }

    public Task getTaskById(int id) {
        Task task = taskRepository.findTaskById(id);
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Task with id " + id + " does not exist");
        }
        return task;
    }

}
