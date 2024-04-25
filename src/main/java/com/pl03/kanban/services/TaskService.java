package com.pl03.kanban.services;

import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.dtos.GetTaskDto;
import com.pl03.kanban.entities.Task;
import com.pl03.kanban.repositories.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public GetTaskDto getTaskById(int id) {
        Task task = taskRepository.findTaskById(id);
        if (task != null) {
            return new GetTaskDto(task.getId(), task.getTaskTitle(), task.getTaskDescription(), task.getTaskAssignees(), task.getTaskStatus(), task.getCreatedOn(), task.getUpdatedOn());
        }
        return null;  // auto send 404 if return null
    }

}
