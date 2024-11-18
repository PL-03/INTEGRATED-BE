package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.*;
import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.kanban_entities.*;
import com.pl03.kanban.kanban_entities.Repositories.BoardCollaboratorsRepository;
import com.pl03.kanban.kanban_entities.Repositories.BoardRepository;
import com.pl03.kanban.kanban_entities.Repositories.StatusV3Repository;
import com.pl03.kanban.kanban_entities.Repositories.TaskV3Repository;
import com.pl03.kanban.services.FileStorageService;
import com.pl03.kanban.utils.ListMapper;
import com.pl03.kanban.exceptions.InvalidTaskFieldException;
import com.pl03.kanban.services.TaskV3Service;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class TaskV3ServiceImpl implements TaskV3Service {
    private final TaskV3Repository taskV3Repository;
    private final StatusV3Repository statusV3Repository;
    private final BoardCollaboratorsRepository boardCollaboratorsRepository;
    private final ModelMapper modelMapper;
    private final ListMapper listMapper;
    private final BoardRepository boardRepository;

    private final FileStorageService fileStorageService;

    @Autowired
    public TaskV3ServiceImpl(TaskV3Repository taskV3Repository, StatusV3Repository statusV3Repository,
                             BoardCollaboratorsRepository boardCollaboratorsRepository, ModelMapper modelMapper, ListMapper listMapper, BoardRepository boardRepository, FileStorageService fileStorageService) {
        this.taskV3Repository = taskV3Repository;
        this.statusV3Repository = statusV3Repository;
        this.boardCollaboratorsRepository = boardCollaboratorsRepository;
        this.modelMapper = modelMapper;
        this.listMapper = listMapper;
        this.boardRepository = boardRepository;
        this.fileStorageService = fileStorageService;

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
    public AddEditTaskDto createTask(String boardId, AddEditTaskDto addEditTaskDto, String userId) {
        Board board = BoardServiceImpl.validateBoardAccessAndOwnerShip(boardId, userId, boardRepository, boardCollaboratorsRepository);

        // Check if DTO is null or empty
        if (addEditTaskDto == null || isEmptyTaskDto(addEditTaskDto)) {
            throw new InvalidTaskFieldException("Task's input must have at least task's title to create task", null);
        }

        // Validate task fields
        ErrorResponse errorResponse = validateTaskFields(addEditTaskDto);
        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidTaskFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        // Map DTO to Task entity and associate it with the board
        TaskV3 task = modelMapper.map(addEditTaskDto, TaskV3.class);
        task.setBoard(board);

        // Set the status to "No Status" if the status field is null or empty
        StatusV3 statusV3;
        if (addEditTaskDto.getStatus() == null || addEditTaskDto.getStatus().isEmpty()) {
            statusV3 = statusV3Repository.findByNameAndBoardId("No Status", boardId)
                    .orElseThrow(() -> new ItemNotFoundException("Default status 'No Status' does not exist in board id: " + boardId));
        } else {
            statusV3 = statusV3Repository.findById(Integer.parseInt(addEditTaskDto.getStatus()))
                    .orElseThrow(() -> new ItemNotFoundException("Status with id " + addEditTaskDto.getStatus() + " does not exist in board id: " + boardId));
        }
        task.setStatusV3(statusV3);

        TaskV3 savedTask = taskV3Repository.save(task);
        return modelMapper.map(savedTask, AddEditTaskDto.class);
    }


    @Override
    public List<GetAllTaskDto> getAllTasks(String boardId, String sortBy, List<String> filterStatuses, String userId) {
        //find board first
        BoardServiceImpl.getBoardAndCheckAccess(boardId, userId, boardRepository, boardCollaboratorsRepository);

        List<TaskV3> tasks;

        if (sortBy == null && (filterStatuses == null || filterStatuses.isEmpty())) {
            tasks = taskV3Repository.findByBoardId(boardId);
        } else if (sortBy == null) {
            List<StatusV3> filteredStatusV3s = statusV3Repository.findByNameInAndBoardId(filterStatuses, boardId);
            tasks = taskV3Repository.findByStatusV3InAndBoardId(filteredStatusV3s, boardId);
        } else if (!sortBy.equals("statusV3.name")) {
            throw new InvalidTaskFieldException("invalid filter parameter");
        } else if (filterStatuses == null || filterStatuses.isEmpty()) {
            tasks = taskV3Repository.findByBoardId(boardId, Sort.by(Sort.Direction.ASC, sortBy));
        } else {
            List<StatusV3> filteredStatusV3s = statusV3Repository.findByNameInAndBoardId(filterStatuses, boardId);
            tasks = taskV3Repository.findByStatusV3InAndBoardId(filteredStatusV3s, boardId, Sort.by(Sort.Direction.ASC, sortBy));
        }

        return listMapper.mapList(tasks, GetAllTaskDto.class, modelMapper);
    }

    @Override
    public TaskDetailDto getTaskById(String boardId, int taskId, String userId) {
        //find board
        BoardServiceImpl.getBoardAndCheckAccess(boardId, userId, boardRepository, boardCollaboratorsRepository);

        TaskV3 task = taskV3Repository.findByIdAndBoardId(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board id: " + boardId));

        // Map the entity to TaskDetailDto
        TaskDetailDto taskDetailDto = modelMapper.map(task, TaskDetailDto.class);

        // Manually set the status to the status name only
        taskDetailDto.setStatus(task.getStatusV3().getName());
        // set attachments
        taskDetailDto.setAttachments(fileStorageService.getFilesByTask(task));

        return taskDetailDto;
    }

    @Override
    public AddEditTaskDto deleteTaskById(String boardId, int taskId, String userId) {
        BoardServiceImpl.validateBoardAccessAndOwnerShip(boardId, userId, boardRepository, boardCollaboratorsRepository);

        TaskV3 task = taskV3Repository.findByIdAndBoardId(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board id: " + boardId));
        taskV3Repository.delete(task);
        return modelMapper.map(task, AddEditTaskDto.class);
    }

    @Override
    public AddEditTaskDto updateTask(String boardId, int taskId, AddEditTaskDto addEditTaskDto, String userId) {
        // Validate board and task access
        BoardServiceImpl.validateBoardAccessAndOwnerShip(boardId, userId, boardRepository, boardCollaboratorsRepository);

        // Check if the task exists in the board
        TaskV3 task = taskV3Repository.findByIdAndBoardId(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board id: " + boardId));

        // Check for file-only update scenario
        boolean hasFileChanges = (addEditTaskDto.getNewFiles() != null && !addEditTaskDto.getNewFiles().isEmpty()) ||
                (addEditTaskDto.getFilesToDelete() != null && !addEditTaskDto.getFilesToDelete().isEmpty());
        boolean hasTaskChanges = addEditTaskDto.getTitle() != null ||
                addEditTaskDto.getDescription() != null ||
                addEditTaskDto.getAssignees() != null ||
                addEditTaskDto.getStatus() != null;

        // Throw exception if no changes at all
        if (!hasFileChanges && !hasTaskChanges) {
            throw new InvalidTaskFieldException("Task's input must have at least task's title to update task", null);
        }

        // Existing task update logic
        if (hasTaskChanges) {
            // Validate task fields
            ErrorResponse errorResponse = validateTaskFields(addEditTaskDto);
            if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
                throw new InvalidTaskFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
            }

            // Update task fields
            task.setTitle(addEditTaskDto.getTitle() == null ? task.getTitle() : addEditTaskDto.getTitle().trim());
            task.setDescription(addEditTaskDto.getDescription() != null ? addEditTaskDto.getDescription().trim() : null);
            task.setAssignees(addEditTaskDto.getAssignees() != null ? addEditTaskDto.getAssignees().trim() : null);

            // Status update logic...
            if (addEditTaskDto.getStatus() != null && !addEditTaskDto.getStatus().isEmpty()) {
                StatusV3 statusV3 = statusV3Repository.findById(Integer.parseInt(addEditTaskDto.getStatus()))
                        .orElseThrow(() -> new ItemNotFoundException("Status with id " + addEditTaskDto.getStatus() + " does not exist in board id: " + boardId));
                task.setStatusV3(statusV3);
            }
        }

        // Save the updated task
        TaskV3 updatedTask = taskV3Repository.save(task);

        // Handle file attachments
        FileUploadResponse fileResponse = null;
        if (hasFileChanges) {
            fileResponse = fileStorageService.handleFileAttachments(
                    boardId,
                    taskId,
                    addEditTaskDto.getAttachments(),
                    addEditTaskDto.getNewFiles(),
                    addEditTaskDto.getFilesToDelete(),
                    userId
            );
        }

        // Prepare response DTO
        AddEditTaskDto responseDto = modelMapper.map(updatedTask, AddEditTaskDto.class);

        // Set attachments
        if (fileResponse != null) {
            responseDto.setAttachments(fileResponse.getAttachments());
        } else {
            // Fetch current attachments if no file changes
            FileStorageDto[] currentAttachments = fileStorageService.getFilesByTask(updatedTask);
            responseDto.setAttachments(currentAttachments);
        }

        // Set the status name
        responseDto.setStatus(updatedTask.getStatusV3().getName());

        return responseDto;
    }

    private boolean isEmptyTaskDto(AddEditTaskDto dto) {
        // Consider file changes as non-empty
        boolean hasFileChanges = (dto.getNewFiles() != null && !dto.getNewFiles().isEmpty()) ||
                (dto.getFilesToDelete() != null && !dto.getFilesToDelete().isEmpty());

        return !hasFileChanges &&
                (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) &&
                (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) &&
                (dto.getAssignees() == null || dto.getAssignees().trim().isEmpty()) &&
                (dto.getStatus() == null || dto.getStatus().trim().isEmpty());
    }


    private ErrorResponse validateTaskFields(AddEditTaskDto addEditTaskDto) {
        // If there are file changes or no changes at all, skip validation
        boolean hasFileChanges = (addEditTaskDto.getNewFiles() != null && !addEditTaskDto.getNewFiles().isEmpty()) ||
                (addEditTaskDto.getFilesToDelete() != null && !addEditTaskDto.getFilesToDelete().isEmpty());

        if (hasFileChanges) {
            return null;
        }

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

