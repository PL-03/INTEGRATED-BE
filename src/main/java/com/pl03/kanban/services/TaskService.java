package com.pl03.kanban.services;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.entities.Task;

import java.util.List;

public interface TaskService {
    AddEditTaskDto createTask(AddEditTaskDto addEditTaskDto);
    List<GetAllTaskDto> getAllTasks();
    Task getTaskById(int id);

    AddEditTaskDto deleteTaskById(int id);
    AddEditTaskDto updateTask(AddEditTaskDto addEditTaskDto, int id);
}