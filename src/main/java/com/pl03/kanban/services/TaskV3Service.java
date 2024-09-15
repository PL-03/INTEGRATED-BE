package com.pl03.kanban.services;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.dtos.TaskDetailDto;

import java.util.List;

public interface TaskV3Service {
    AddEditTaskDto createTask(String boardId, AddEditTaskDto addEditTaskDto);
    List<GetAllTaskDto> getAllTasks(String boardId, String sortBy, List<String> filterStatuses);
    TaskDetailDto getTaskById(String boardId, int taskId);
    AddEditTaskDto deleteTaskById(String boardId, int taskId);
    AddEditTaskDto updateTask(String boardId, int taskId, AddEditTaskDto addEditTaskDto);
}