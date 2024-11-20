package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.*;
import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.kanban_entities.*;
import com.pl03.kanban.kanban_entities.repositories.BoardCollaboratorsRepository;
import com.pl03.kanban.kanban_entities.repositories.BoardRepository;
import com.pl03.kanban.kanban_entities.repositories.StatusV3Repository;
import com.pl03.kanban.kanban_entities.repositories.TaskV3Repository;
import com.pl03.kanban.services.FileStorageService;
import com.pl03.kanban.utils.ListMapper;
import com.pl03.kanban.exceptions.InvalidTaskFieldException;
import com.pl03.kanban.services.TaskV3Service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.pl03.kanban.services.FileStorageService.MAX_FILES;

@Service
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

        modelMapper.typeMap(TaskV3.class, AddEditTaskDtoWithAttachments.class).addMappings(mapper ->
                mapper.map(src -> src.getStatusV3().getName(), AddEditTaskDtoWithAttachments::setStatus));
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
    public TaskDetailDtoWithAttachments getTaskById(String boardId, int taskId, String userId) {
        BoardServiceImpl.getBoardAndCheckAccess(boardId, userId, boardRepository, boardCollaboratorsRepository);

        // Use the new repository method that fetches files eagerly
        TaskV3 task = taskV3Repository.findByIdAndBoardIdWithFiles(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board id: " + boardId));

        // Map the entity to TaskDetailDto
        TaskDetailDtoWithAttachments taskDetailDto = modelMapper.map(task, TaskDetailDtoWithAttachments.class);
        taskDetailDto.setStatus(task.getStatusV3().getName());

        // Map attachments
        List<FileAttachmentDto> attachments = task.getFiles().stream()
                .map(file -> modelMapper.map(file, FileAttachmentDto.class))
                .collect(Collectors.toList());
        taskDetailDto.setAttachments(attachments);

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
    @Transactional(transactionManager = "kanbanTransactionManager")
    public AddEditTaskDtoWithAttachments updateTask(String boardId, int taskId, AddEditTaskDtoWithAttachments addEditTaskDto, String userId) {
        BoardServiceImpl.validateBoardAccessAndOwnerShip(boardId, userId, boardRepository, boardCollaboratorsRepository);

        // Use the new repository method that fetches files eagerly
        TaskV3 task = taskV3Repository.findByIdAndBoardIdWithFiles(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board id: " + boardId));

        // Handle file attachments first
        List<String> unaddedFiles = new ArrayList<>();
        if (addEditTaskDto.getNewAttachments() != null && !addEditTaskDto.getNewAttachments().isEmpty()) {
            unaddedFiles = fileStorageService.validateAndStoreFiles(
                    addEditTaskDto.getNewAttachments(), task);
        }

        // Handle file deletions
        if (addEditTaskDto.getAttachmentsToDelete() != null && !addEditTaskDto.getAttachmentsToDelete().isEmpty()) {
            fileStorageService.deleteFiles(addEditTaskDto.getAttachmentsToDelete(), task);
        }

        // Validate and update other task fields
        if (addEditTaskDto == null || isEmptyTaskDto(addEditTaskDto)) {
            throw new InvalidTaskFieldException("Task's input must have at least task's title to update task", null);
        }

        ErrorResponse errorResponse = validateTaskFields(addEditTaskDto);
        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidTaskFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        // Update task fields
        task.setTitle(addEditTaskDto.getTitle() == null ? task.getTitle() : addEditTaskDto.getTitle().trim());
        task.setDescription(addEditTaskDto.getDescription() != null ? addEditTaskDto.getDescription().trim() : null);
        task.setAssignees(addEditTaskDto.getAssignees() != null ? addEditTaskDto.getAssignees().trim() : null);

        if (addEditTaskDto.getStatus() != null && !addEditTaskDto.getStatus().isEmpty()) {
            StatusV3 statusV3 = statusV3Repository.findById(Integer.parseInt(addEditTaskDto.getStatus()))
                    .orElseThrow(() -> new ItemNotFoundException("Status with id " + addEditTaskDto.getStatus() + " does not exist in board id: " + boardId));
            task.setStatusV3(statusV3);
        }

        // Save the updated task
        TaskV3 updatedTask = taskV3Repository.save(task);

        if (!unaddedFiles.isEmpty()) {
            throw new IllegalArgumentException("Each task can have at most " + MAX_FILES +
                    " files. The following files are not added: " + String.join(", ", unaddedFiles));
        }

        // Map existing attachments to FileAttachmentDto
        List<FileAttachmentDto> existingAttachments = updatedTask.getFiles().stream()
                .map(file -> new FileAttachmentDto(file.getId(), file.getName(), file.getType(), file.getAddedOn()))
                .collect(Collectors.toList());

        // Map to response DTO
        AddEditTaskDtoWithAttachments response = modelMapper.map(updatedTask, AddEditTaskDtoWithAttachments.class);
        response.setExistingAttachments(existingAttachments); // Include attachments in the response
        return response;
    }


    private boolean isEmptyTaskDto(AddEditTaskDto dto) {
        return (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) &&
                (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) &&
                (dto.getAssignees() == null || dto.getAssignees().trim().isEmpty()) &&
                (dto.getStatus() == null || dto.getStatus().trim().isEmpty());
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

