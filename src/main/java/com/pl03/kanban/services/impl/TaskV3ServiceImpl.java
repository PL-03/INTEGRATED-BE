package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.AddEditTaskDto;
import com.pl03.kanban.dtos.TaskDetailDto;
import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.kanban_entities.*;
import com.pl03.kanban.utils.ListMapper;
import com.pl03.kanban.exceptions.InvalidTaskFieldException;
import com.pl03.kanban.services.TaskV3Service;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TaskV3ServiceImpl implements TaskV3Service {
    private final TaskV3Repository taskV3Repository;
    private final StatusV3Repository statusV3Repository;
    private final ModelMapper modelMapper;
    private final ListMapper listMapper;
    private final BoardRepository boardRepository;

    @Autowired
    public TaskV3ServiceImpl(TaskV3Repository taskV3Repository, StatusV3Repository statusV3Repository,
                             ModelMapper modelMapper, ListMapper listMapper, BoardRepository boardRepository) {
        this.taskV3Repository = taskV3Repository;
        this.statusV3Repository = statusV3Repository;
        this.modelMapper = modelMapper;
        this.listMapper = listMapper;
        this.boardRepository = boardRepository;

        // Custom mapping for status name
        modelMapper.typeMap(TaskV3.class, GetAllTaskDto.class).addMappings(mapper ->
                mapper.map(src -> src.getStatusV3().getName(), GetAllTaskDto::setStatus));

        modelMapper.typeMap(TaskV3.class, AddEditTaskDto.class).addMappings(mapper ->
                mapper.map(src -> src.getStatusV3().getName(), AddEditTaskDto::setStatus));
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
        TaskV3 task = modelMapper.map(addEditTaskDto, TaskV3.class);
        task.setBoard(board);

        // Set the status to "No Status" (ID 1) if the status field is null or empty
        StatusV3 statusV3;
        if (addEditTaskDto.getStatus() == null || addEditTaskDto.getStatus().isEmpty()) {
            statusV3 = statusV3Repository.findById(1)
                    .orElseThrow(() -> new ItemNotFoundException("Default status 'No Status' with id 1 does not exist"));
        } else {
            statusV3 = statusV3Repository.findById(Integer.parseInt(addEditTaskDto.getStatus()))
                    .orElseThrow(() -> new ItemNotFoundException("Status with id " + addEditTaskDto.getStatus() + " does not exist in board id: " + boardId));
        }
        task.setStatusV3(statusV3);

        TaskV3 savedTask = taskV3Repository.save(task);
        return modelMapper.map(savedTask, AddEditTaskDto.class);
    }


    @Override
    public List<GetAllTaskDto> getAllTasks(String boardId, String sortBy, List<String> filterStatuses) {
        //find board first
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        List<TaskV3> tasks;

        if (sortBy == null && (filterStatuses == null || filterStatuses.isEmpty())) {
            tasks = taskV3Repository.findByBoardBoardId(boardId);
        } else if (sortBy == null) {
            List<StatusV3> filteredStatusV3s = statusV3Repository.findByNameInAndBoardBoardId(filterStatuses, boardId);
            tasks = taskV3Repository.findByStatusV3InAndBoardBoardId(filteredStatusV3s, boardId);
        } else if (!sortBy.equals("status.name")) {
            throw new InvalidTaskFieldException("invalid filter parameter");
        } else if (filterStatuses == null || filterStatuses.isEmpty()) {
            tasks = taskV3Repository.findByBoardBoardId(boardId, Sort.by(Sort.Direction.ASC, sortBy));
        } else {
            List<StatusV3> filteredStatusV3s = statusV3Repository.findByNameInAndBoardBoardId(filterStatuses, boardId);
            tasks = taskV3Repository.findByStatusV3InAndBoardBoardId(filteredStatusV3s, boardId, Sort.by(Sort.Direction.ASC, sortBy));
        }

        return listMapper.mapList(tasks, GetAllTaskDto.class, modelMapper);
    }

    @Override
    public TaskDetailDto getTaskById(String boardId, int taskId) {
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        TaskV3 task = taskV3Repository.findByIdAndBoardBoardId(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board id: " + boardId));

        // Map the entity to TaskDetailDto
        TaskDetailDto taskDetailDto = modelMapper.map(task, TaskDetailDto.class);

        // Manually set the status to the status name only
        taskDetailDto.setStatus(task.getStatusV3().getName());

        return taskDetailDto;
    }

    @Override
    public AddEditTaskDto deleteTaskById(String boardId, int taskId) {
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        TaskV3 task = taskV3Repository.findByIdAndBoardBoardId(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board id: " + boardId));
        taskV3Repository.delete(task);
        return modelMapper.map(task, AddEditTaskDto.class);
    }

    @Override
    public AddEditTaskDto updateTask(String boardId, int taskId, AddEditTaskDto addEditTaskDto) {
        // First, check if the board exists
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        // Check if the task exists in the board
        TaskV3 task = taskV3Repository.findByIdAndBoardBoardId(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board id: " + boardId));

        // Validate task fields
        ErrorResponse errorResponse = validateTaskFields(addEditTaskDto);
        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidTaskFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        // Update task details using ModelMapper
        modelMapper.map(addEditTaskDto, task);

        // Set the status if provided
        if (addEditTaskDto.getStatus() != null && !addEditTaskDto.getStatus().isEmpty()) {
            StatusV3 statusV3 = statusV3Repository.findById(Integer.parseInt(addEditTaskDto.getStatus()))
                    .orElseThrow(() -> new ItemNotFoundException("Status with id " + addEditTaskDto.getStatus() + " does not exist in board id: " + boardId));
            task.setStatusV3(statusV3);
        }

        // No change to boardId - keep the task associated with the current board
        task.setBoard(board);

        // Save the updated task
        TaskV3 updatedTask = taskV3Repository.save(task);

        // Return the mapped DTO
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

            if (errorResponse.getErrors().isEmpty() && !statusV3Repository.existsById(Integer.parseInt(addEditTaskDto.getStatus()))) {
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
