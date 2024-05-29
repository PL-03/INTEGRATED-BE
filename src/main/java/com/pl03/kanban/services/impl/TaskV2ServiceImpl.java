package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.entities.Status;
import com.pl03.kanban.entities.TaskV2;
import com.pl03.kanban.exceptions.InvalidTaskFieldException;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.repositories.StatusRepository;
import com.pl03.kanban.repositories.TaskV2Repository;
import com.pl03.kanban.services.TaskV2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskV2ServiceImpl implements TaskV2Service {
    private final TaskV2Repository taskV2Repository;
    private final StatusRepository statusRepository;

    @Autowired
    public TaskV2ServiceImpl(TaskV2Repository taskV2Repository, StatusRepository statusRepository) {
        this.taskV2Repository = taskV2Repository;
        this.statusRepository = statusRepository;
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

        TaskV2 task = mapToEntity(addEditTaskDto);
        TaskV2 savedTask = taskV2Repository.save(task);
        return mapToAddEditTaskDto(savedTask);
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
            throw new InvalidTaskFieldException("invalid filter parameter"); //created this because of the requirement
        } else if (filterStatuses == null || filterStatuses.isEmpty()) {
            tasks = taskV2Repository.findAll(Sort.by(Sort.Direction.ASC, sortBy));
        } else {
            List<Status> filteredStatuses = statusRepository.findByNameIn(filterStatuses);
            tasks = taskV2Repository.findByStatusIn(filteredStatuses, Sort.by(Sort.Direction.ASC, sortBy));
        }
        return tasks.stream()
                .map(task -> new GetAllTaskDto(
                        task.getId(),
                        task.getTitle(),
                        task.getAssignees(),
                        task.getStatus().getName()))
                .collect(Collectors.toList());
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
        return mapToAddEditTaskDto(task);
    }

    @Override
    public AddEditTaskDto updateTask(AddEditTaskDto addEditTaskDto, int id) {
        TaskV2 task = taskV2Repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + id + " does not exist"));

        ErrorResponse errorResponse = validateTaskFields(addEditTaskDto);
        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidTaskFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        task.setTitle(addEditTaskDto.getTitle());
        task.setDescription(addEditTaskDto.getDescription());
        task.setAssignees(addEditTaskDto.getAssignees());

        if (addEditTaskDto.getStatus() != null && !addEditTaskDto.getStatus().isEmpty()) {
            Status status = statusRepository.findById(Integer.parseInt(addEditTaskDto.getStatus()))
                    .orElseThrow(() -> new ItemNotFoundException("Status with id " + addEditTaskDto.getStatus() + " does not exist"));
            task.setStatus(status);
        }

        TaskV2 updatedTask = taskV2Repository.save(task);
        return mapToAddEditTaskDto(updatedTask);
    }

    private TaskV2 mapToEntity(AddEditTaskDto addEditTaskDto) {
        if (addEditTaskDto.getStatus() == null || addEditTaskDto.getStatus().isEmpty()) {
            return new TaskV2(addEditTaskDto.getTitle(), addEditTaskDto.getDescription(), addEditTaskDto.getAssignees());
        } else {
            Status status = statusRepository.findById(Integer.parseInt(addEditTaskDto.getStatus()))
                    .orElseThrow(() -> new ItemNotFoundException("Status with id " + addEditTaskDto.getStatus() + " does not exist"));
            TaskV2 task = new TaskV2();
            task.setTitle(addEditTaskDto.getTitle());
            task.setDescription(addEditTaskDto.getDescription());
            task.setAssignees(addEditTaskDto.getAssignees());
            task.setStatus(status);
            return task;
        }
    }

    private AddEditTaskDto mapToAddEditTaskDto(TaskV2 task) {
        AddEditTaskDto addEditTaskDto = new AddEditTaskDto();
        addEditTaskDto.setId(task.getId());
        addEditTaskDto.setTitle(task.getTitle());
        addEditTaskDto.setDescription(task.getDescription());
        addEditTaskDto.setAssignees(task.getAssignees());
        addEditTaskDto.setStatus(String.valueOf(task.getStatus().getId()));
        return addEditTaskDto;
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