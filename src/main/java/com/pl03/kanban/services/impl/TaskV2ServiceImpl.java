package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.entities.Status;
import com.pl03.kanban.entities.TaskV2;
import com.pl03.kanban.exceptions.InvalidTaskFiledException;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.repositories.StatusRepository;
import com.pl03.kanban.repositories.TaskV2Repository;
import com.pl03.kanban.services.TaskV2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    public AddEditTaskDto createTask(AddEditTaskDto addEditTaskDto) {
        List<Map<String, String>> errors = validateTaskFields(addEditTaskDto);
        if (addEditTaskDto.getTitle() == null || addEditTaskDto.getTitle().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "title");
            error.put("message", "Title is required");
            errors.add(error);
        }

        if (!errors.isEmpty()) {
            throw new InvalidTaskFiledException("Validation error. Check 'errors' field for details", errors);
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

        List<Map<String, String>> errors = validateTaskFields(addEditTaskDto);
        if (!errors.isEmpty()) {
            throw new InvalidTaskFiledException("Validation error. Check 'errors' field for details", errors);
        }
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

    private List<Map<String, String>> validateTaskFields(AddEditTaskDto addEditTaskDto) {
        List<Map<String, String>> errors = new ArrayList<>();

        if (addEditTaskDto.getTitle() == null || addEditTaskDto.getTitle().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "title");
            error.put("message", "Title cannot be null or empty");
            errors.add(error);
        } else if (addEditTaskDto.getTitle().trim().length() > 100) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "title");
            error.put("message", "Title cannot exceed 100 characters");
            errors.add(error);
        }

        if (addEditTaskDto.getDescription() != null && addEditTaskDto.getDescription().trim().length() > 500) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "description");
            error.put("message", "size must be between 0 and 500");
            errors.add(error);
        }

        if (addEditTaskDto.getAssignees() != null && addEditTaskDto.getAssignees().trim().length() > 30) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "assignees");
            error.put("message", "Assignees cannot exceed 30 characters");
            errors.add(error);
        }

        return errors;
    }
}