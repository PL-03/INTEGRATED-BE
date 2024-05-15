package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.entities.Status;
import com.pl03.kanban.entities.TaskV2;
import com.pl03.kanban.exceptions.InvalidTaskTitleException;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.repositories.StatusRepository;
import com.pl03.kanban.repositories.TaskV2Repository;
import com.pl03.kanban.services.TaskV2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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
        TaskV2 task = mapToEntity(addEditTaskDto);
        TaskV2 savedTask = taskV2Repository.save(task);
        return mapToAddEditTaskDto(savedTask);
    }

    @Override
    public List<GetAllTaskDto> getAllTasks() {
        List<TaskV2> tasks = taskV2Repository.findAll();
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

        // Validate the input
        if (addEditTaskDto.getTitle() == null || addEditTaskDto.getTitle().trim().isEmpty()) {
            throw new InvalidTaskTitleException("Task title cannot be null or empty");
        } else {
            task.setTitle(addEditTaskDto.getTitle().trim());
        }
        task.setDescription(addEditTaskDto.getDescription() != null ? addEditTaskDto.getDescription().trim() : null);
        task.setAssignees(addEditTaskDto.getAssignees() != null ? addEditTaskDto.getAssignees().trim() : null);

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
}