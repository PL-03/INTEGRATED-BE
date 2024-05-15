package com.pl03.kanban.services;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.entities.TaskV2;

import java.util.List;

public interface TaskV2Service {
    AddEditTaskDto createTask(AddEditTaskDto addEditTaskDto);
    List<GetAllTaskDto> getAllTasks(String sortBy, List<String> filterStatuses);
    TaskV2 getTaskById(int id);
    AddEditTaskDto deleteTaskById(int id);
    AddEditTaskDto updateTask(AddEditTaskDto addEditTaskDto, int id);

//    List<GetAllTaskDto> getAllTasksSortedAndFiltered (String sortBy, List<String> filterStatuses);
}