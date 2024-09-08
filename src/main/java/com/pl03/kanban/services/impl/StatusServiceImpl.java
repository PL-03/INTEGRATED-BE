package com.pl03.kanban.services.impl;

import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.kanban_entities.*;
import com.pl03.kanban.exceptions.InvalidStatusFieldException;
import com.pl03.kanban.services.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StatusServiceImpl implements StatusService {

    private final StatusRepository statusRepository;
    private final TaskV2Repository taskV2Repository;
    private final BoardRepository boardRepository;

    private static final List<String> DEFAULT_STATUS_NAMES = Arrays.asList("No Status", "Done");
    private static final int MAX_STATUS_NAME_LENGTH = 50;
    private static final int MAX_STATUS_DESCRIPTION_LENGTH = 200;

    @Autowired
    public StatusServiceImpl(StatusRepository statusRepository, TaskV2Repository taskV2Repository, BoardRepository boardRepository) {
        this.statusRepository = statusRepository;
        this.taskV2Repository = taskV2Repository;
        this.boardRepository = boardRepository;
    }


    private boolean isStatusNameDefault(String name) {
        return DEFAULT_STATUS_NAMES.stream()
                .anyMatch(protectedName -> protectedName.equalsIgnoreCase(name)); //return true if status name is matched
    }

    private boolean isStatusNameTaken(String name, int excludedId) {
        List<Status> statuses = statusRepository.findByNameIgnoreCaseAndIdNot(name.trim().toUpperCase(), excludedId);
        return !statuses.isEmpty();
    }


    @Override
    public List<Status> getAllStatuses(String boardId) {
        return statusRepository.findByBoardBoardId(boardId);
    }

    @Override
    public Status getStatusById(String boardId, int id) {
        return statusRepository.findByIdAndBoardBoardId(id, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist in board " + boardId));
    }


    @Override
    public Status createStatus(String boardId, Status status) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        ErrorResponse errorResponse = validateStatusFields(status.getName(), status.getDescription(), 0, boardId);

        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidStatusFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        status.setBoard(board);
        return statusRepository.save(status);
    }

    @Override
    public Status updateStatus(String boardId, int id, Status updatedStatus) {
        Status status = getStatusById(boardId, id);

        ErrorResponse errorResponse = validateStatusFields(updatedStatus.getName(), updatedStatus.getDescription(), id, boardId);

        if (isStatusNameDefault(status.getName())) {
            if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
                throw new InvalidStatusFieldException(status.getName() + " cannot be modified", errorResponse.getErrors());
            } else {
                throw new InvalidStatusFieldException(status.getName() + " cannot be modified");
            }
        }

        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidStatusFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        status.setName(updatedStatus.getName() == null || updatedStatus.getName().trim().isEmpty() ? status.getName()
                : updatedStatus.getName().trim());
        status.setDescription(updatedStatus.getDescription());
        return statusRepository.save(status);
    }


    @Override
    public Status deleteStatus(String boardId, int id) {
        Status status = getStatusById(boardId, id);

        if (isStatusNameDefault(status.getName())) {
            throw new InvalidStatusFieldException(status.getName() + " cannot be deleted");
        }

        List<TaskV2> tasksWithStatus = taskV2Repository.findByStatus(status);
        if (!tasksWithStatus.isEmpty()) {
            throw new InvalidStatusFieldException("Destination status for task transfer not specified");
        }

        statusRepository.delete(status);
        return status;
    }

    @Override
    public void deleteStatusAndTransferTasks(String boardId, int id, int newStatusId) {
        Status currentStatus = getStatusById(boardId, id);
        Status newStatus = getStatusById(boardId, newStatusId);

        if (isStatusNameDefault(currentStatus.getName())) {
            throw new InvalidStatusFieldException(currentStatus.getName() + " cannot be deleted");
        }

        if (id == newStatusId) {
            throw new InvalidStatusFieldException("destination status for task transfer must be different from current status");
        }

        List<TaskV2> tasksWithCurrentStatus = taskV2Repository.findByStatus(currentStatus);
        tasksWithCurrentStatus.forEach(task -> task.setStatus(newStatus));
        taskV2Repository.saveAll(tasksWithCurrentStatus);

        statusRepository.delete(currentStatus);
    }


    private ErrorResponse validateStatusFields(String name, String description, int currentStatusId, String boardId) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation error. Check 'errors' field for details", "");

        if (name == null || name.trim().isEmpty()) {
            errorResponse.addValidationError(Status.Fields.name, "must not be null");
        } else if (name.trim().length() > MAX_STATUS_NAME_LENGTH) {
            errorResponse.addValidationError(Status.Fields.name, "size must be between 0 and " + MAX_STATUS_NAME_LENGTH);
        } else if (isStatusNameTaken(name, currentStatusId, boardId)) {
            errorResponse.addValidationError(Status.Fields.name, "must be unique within the board");
        }

        if (description != null && description.trim().length() > MAX_STATUS_DESCRIPTION_LENGTH) {
            errorResponse.addValidationError(Status.Fields.description, "size must be between 0 and " + MAX_STATUS_DESCRIPTION_LENGTH);
        }

        return errorResponse.getErrors().isEmpty() ? null : errorResponse;
    }

    private boolean isStatusNameTaken(String name, int excludedId, String boardId) {
        List<Status> statuses = statusRepository.findByBoardBoardId(boardId);
        return statuses.stream()
                .anyMatch(status -> status.getId() != excludedId && status.getName().equalsIgnoreCase(name.trim()));
    }

    @Override
    public void addDefaultStatus(String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        List<Status> defaultStatuses = Arrays.asList(
                new Status(0, "No Status", "A status has not been assigned", board),
                new Status(0, "To Do", "The task is included in the project", board),
                new Status(0, "Doing", "The task is being worked on", board),
                new Status(0, "Done", "The task has been completed", board)
        );

        // Save all the default statuses to the database
        statusRepository.saveAll(defaultStatuses);
    }

}