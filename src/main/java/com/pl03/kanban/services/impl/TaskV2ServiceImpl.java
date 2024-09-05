package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.utils.ListMapper;
import com.pl03.kanban.kanban_entities.Status;
import com.pl03.kanban.kanban_entities.TaskV2;
import com.pl03.kanban.exceptions.InvalidTaskFieldException;
import com.pl03.kanban.kanban_entities.StatusRepository;
import com.pl03.kanban.kanban_entities.TaskV2Repository;
import com.pl03.kanban.services.TaskV2Service;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TaskV2ServiceImpl implements TaskV2Service {
    private final TaskV2Repository taskV2Repository;
    private final StatusRepository statusRepository;
    private final ModelMapper modelMapper;
    private final ListMapper listMapper;

    @Autowired
    public TaskV2ServiceImpl(TaskV2Repository taskV2Repository, StatusRepository statusRepository, ModelMapper modelMapper, ListMapper listMapper) {
        this.taskV2Repository = taskV2Repository;
        this.statusRepository = statusRepository;
        this.modelMapper = modelMapper;
        this.listMapper = listMapper;
    }

    private static final int MAX_TASK_TITLE_LENGTH = 100;
    private static final int MAX_TASK_DESCRIPTION_LENGTH = 500;
    private static final int MAX_TASK_ASSIGNEES_LENGTH = 30;

    @Override
    public AddEditTaskDto createTask(AddEditTaskDto addEditTaskDto) {
        ErrorResponse errorResponse = validateTaskFields(addEditTaskDto);

        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidTaskFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        TaskV2 task = modelMapper.map(addEditTaskDto, TaskV2.class);
        if (addEditTaskDto.getStatus() != null && !addEditTaskDto.getStatus().isEmpty()) {
            Status status = statusRepository.findById(Integer.parseInt(addEditTaskDto.getStatus()))
                    .orElseThrow(() -> new ItemNotFoundException("Status with id " + addEditTaskDto.getStatus() + " does not exist"));
            task.setStatus(status);
        }

        TaskV2 savedTask = taskV2Repository.save(task);
        return modelMapper.map(savedTask, AddEditTaskDto.class);
    }


    @Override
    public List<GetAllTaskDto> getAllTasks(String sortBy, List<String> filterStatuses) {
        List<TaskV2> tasks;
        if (sortBy == null && (filterStatuses == null || filterStatuses.isEmpty())) {
            tasks = taskV2Repository.findAll();
        } else if (sortBy == null) {
            List<Status> filteredStatuses = statusRepository.findByNameIn(filterStatuses);
            tasks = taskV2Repository.findByStatusIn(filteredStatuses);
        } else if (!sortBy.equals("status.name")) {
            throw new InvalidTaskFieldException("Invalid filter parameter");
        } else if (filterStatuses == null || filterStatuses.isEmpty()) {
            tasks = taskV2Repository.findAll(Sort.by(Sort.Direction.ASC, sortBy));
        } else {
            List<Status> filteredStatuses = statusRepository.findByNameIn(filterStatuses);
            tasks = taskV2Repository.findByStatusIn(filteredStatuses, Sort.by(Sort.Direction.ASC, sortBy));
        }

        // Use ListMapper to map tasks to GetAllTaskDto
        return listMapper.mapList(tasks, GetAllTaskDto.class, task -> modelMapper.map(task, GetAllTaskDto.class));
    }


    @Override
    public TaskV2 getTaskById(int id) {
        return taskV2Repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + id + " does not exist"));
    }

    @Override
    public AddEditTaskDto deleteTaskById(int id) {
        TaskV2 task = taskV2Repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + id + " does not exist"));

        taskV2Repository.delete(task);
        return modelMapper.map(task, AddEditTaskDto.class);
    }

    @Override
    public AddEditTaskDto updateTask(AddEditTaskDto addEditTaskDto, int id) {
        TaskV2 task = taskV2Repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + id + " does not exist"));

        ErrorResponse errorResponse = validateTaskFields(addEditTaskDto);
        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidTaskFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        modelMapper.map(addEditTaskDto, task);
        if (addEditTaskDto.getStatus() != null && !addEditTaskDto.getStatus().isEmpty()) {
            Status status = statusRepository.findById(Integer.parseInt(addEditTaskDto.getStatus()))
                    .orElseThrow(() -> new ItemNotFoundException("Status with id " + addEditTaskDto.getStatus() + " does not exist"));
            task.setStatus(status);
        }

        TaskV2 updatedTask = taskV2Repository.save(task);
        return modelMapper.map(updatedTask, AddEditTaskDto.class);
    }

    private ErrorResponse validateTaskFields(AddEditTaskDto addEditTaskDto) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation error. Check 'errors' field for details", "");

        if (addEditTaskDto.getTitle() == null || addEditTaskDto.getTitle().trim().isEmpty()) {
            errorResponse.addValidationError(AddEditTaskDto.Fields.title, "must not be null");
        } else if (addEditTaskDto.getTitle().trim().length() > MAX_TASK_TITLE_LENGTH) {
            errorResponse.addValidationError(AddEditTaskDto.Fields.title, "size must be between 0 and " + MAX_TASK_TITLE_LENGTH);
        }

        if (addEditTaskDto.getDescription() != null && addEditTaskDto.getDescription().trim().length() > MAX_TASK_DESCRIPTION_LENGTH) {
            errorResponse.addValidationError(AddEditTaskDto.Fields.description, "size must be between 0 and " + MAX_TASK_DESCRIPTION_LENGTH);
        }

        if (addEditTaskDto.getAssignees() != null && addEditTaskDto.getAssignees().trim().length() > MAX_TASK_ASSIGNEES_LENGTH) {
            errorResponse.addValidationError(AddEditTaskDto.Fields.assignees, "size must be between 0 and " + MAX_TASK_ASSIGNEES_LENGTH);
        }

        if (addEditTaskDto.getStatus() != null && !addEditTaskDto.getStatus().isEmpty()) {
            try {
                Integer.parseInt(addEditTaskDto.getStatus());
            } catch (NumberFormatException e) {
                errorResponse.addValidationError(AddEditTaskDto.Fields.status, "Invalid status ID");
            }

            if (errorResponse.getErrors().isEmpty() && !statusRepository.existsById(Integer.parseInt(addEditTaskDto.getStatus()))) {
                errorResponse.addValidationError(AddEditTaskDto.Fields.status, "does not exist");
            }
        }

        // If there are no validation errors, return null
        if (errorResponse.getErrors().isEmpty()) {
            return null;
        }

        return errorResponse;
    }
}
