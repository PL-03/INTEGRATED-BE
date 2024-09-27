package com.pl03.kanban.services;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.dtos.TaskDetailDto;

import java.util.List;

public interface TaskV3Service {
    AddEditTaskDto createTask(String boardId, AddEditTaskDto addEditTaskDto, String requesterOid);
    List<GetAllTaskDto> getAllTasks(String boardId, String sortBy, List<String> filterStatuses, String requesterOid);
    TaskDetailDto getTaskById(String boardId, int taskId, String userId);
    AddEditTaskDto deleteTaskById(String boardId, int taskId, String userId);
    AddEditTaskDto updateTask(String boardId, int taskId, AddEditTaskDto addEditTaskDto, String userId);
}