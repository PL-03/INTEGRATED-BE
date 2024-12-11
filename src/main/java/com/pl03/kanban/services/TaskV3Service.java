package com.pl03.kanban.services;

import com.pl03.kanban.dtos.*;

import java.util.List;

public interface TaskV3Service {
    AddEditTaskDto createTask(String boardId, AddEditTaskDto addEditTaskDto, String requesterOid);
    List<GetAllTaskDto> getAllTasks(String boardId, String sortBy, List<String> filterStatuses, String requesterOid);
    TaskDetailDtoWithAttachments getTaskById(String boardId, int taskId, String userId);
    AddEditTaskDto deleteTaskById(String boardId, int taskId, String userId);
    AddEditTaskDtoWithAttachments updateTask(String boardId, int taskId, AddEditTaskDtoWithAttachments addEditTaskDto, String userId);
}