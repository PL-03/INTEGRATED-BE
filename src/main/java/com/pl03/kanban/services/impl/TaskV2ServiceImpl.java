package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.kanban_entities.*;
import com.pl03.kanban.utils.ListMapper;
import com.pl03.kanban.exceptions.InvalidTaskFieldException;
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
    private final BoardRepository boardRepository;

    @Autowired
    public TaskV2ServiceImpl(TaskV2Repository taskV2Repository, StatusRepository statusRepository,
                             ModelMapper modelMapper, ListMapper listMapper, BoardRepository boardRepository) {
        this.taskV2Repository = taskV2Repository;
        this.statusRepository = statusRepository;
        this.modelMapper = modelMapper;
        this.listMapper = listMapper;
        this.boardRepository = boardRepository;

        // Custom mapping for status name
        modelMapper.typeMap(TaskV2.class, GetAllTaskDto.class).addMappings(mapper ->
                mapper.map(src -> src.getStatus().getName(), GetAllTaskDto::setStatus));

        modelMapper.typeMap(TaskV2.class, AddEditTaskDto.class).addMappings(mapper ->
                mapper.map(src -> src.getStatus().getName(), AddEditTaskDto::setStatus));
    }

    private static final int MAX_TASK_TITLE_LENGTH = 100;
    private static final int MAX_TASK_DESCRIPTION_LENGTH = 500;
    private static final int MAX_TASK_ASSIGNEES_LENGTH = 30;

    @Override
    public AddEditTaskDto createTask(String boardId, AddEditTaskDto addEditTaskDto) {
        // Validate task fields
        ErrorResponse errorResponse = validateTaskFields(addEditTaskDto);
        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidTaskFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        // Find the board by ID
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        // Map DTO to Task entity and associate it with the board
        TaskV2 task = modelMapper.map(addEditTaskDto, TaskV2.class);
        task.setBoard(board);

        // Set the status if provided
        if (addEditTaskDto.getStatus() != null && !addEditTaskDto.getStatus().isEmpty()) {
            Status status = statusRepository.findById(Integer.parseInt(addEditTaskDto.getStatus()))
                    .orElseThrow(() -> new ItemNotFoundException("Status with id " + addEditTaskDto.getStatus() + " does not exist"));
            task.setStatus(status);
        }

        TaskV2 savedTask = taskV2Repository.save(task);
        return modelMapper.map(savedTask, AddEditTaskDto.class);
    }

    @Override
    public List<GetAllTaskDto> getAllTasks(String boardId, String sortBy, List<String> filterStatuses) {
        List<TaskV2> tasks;

        if (sortBy == null && (filterStatuses == null || filterStatuses.isEmpty())) {
            tasks = taskV2Repository.findByBoardBoardId(boardId);
        } else if (sortBy == null) {
            List<Status> filteredStatuses = statusRepository.findByNameInAndBoardBoardId(filterStatuses, boardId);
            tasks = taskV2Repository.findByStatusInAndBoardBoardId(filteredStatuses, boardId);
        } else if (!sortBy.equals("status.name")) {
            throw new InvalidTaskFieldException("invalid filter parameter");
        } else if (filterStatuses == null || filterStatuses.isEmpty()) {
            tasks = taskV2Repository.findByBoardBoardId(boardId, Sort.by(Sort.Direction.ASC, sortBy));
        } else {
            List<Status> filteredStatuses = statusRepository.findByNameInAndBoardBoardId(filterStatuses, boardId);
            tasks = taskV2Repository.findByStatusInAndBoardBoardId(filteredStatuses, boardId, Sort.by(Sort.Direction.ASC, sortBy));
        }

        return listMapper.mapList(tasks, GetAllTaskDto.class, modelMapper);
    }

    @Override
    public TaskV2 getTaskById(String boardId, int taskId) {
        return taskV2Repository.findByIdAndBoardBoardId(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board " + boardId));
    }

    @Override
    public AddEditTaskDto deleteTaskById(String boardId, int taskId) {
        TaskV2 task = taskV2Repository.findByIdAndBoardBoardId(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board " + boardId));
        taskV2Repository.delete(task);
        return modelMapper.map(task, AddEditTaskDto.class);
    }

    @Override
    public AddEditTaskDto updateTask(String boardId, int taskId, AddEditTaskDto addEditTaskDto) {
        // Find the task by ID
        TaskV2 task = taskV2Repository.findByIdAndBoardBoardId(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board " + boardId));

        // Validate task fields
        ErrorResponse errorResponse = validateTaskFields(addEditTaskDto);
        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidTaskFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        // Update task details
        modelMapper.map(addEditTaskDto, task);

        // Set the status if provided
        if (addEditTaskDto.getStatus() != null && !addEditTaskDto.getStatus().isEmpty()) {
            Status status = statusRepository.findById(Integer.parseInt(addEditTaskDto.getStatus()))
                    .orElseThrow(() -> new ItemNotFoundException("Status with id " + addEditTaskDto.getStatus() + " does not exist"));
            task.setStatus(status);
        }

        // Ensure the task is associated with the correct board
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));
        task.setBoard(board);

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

