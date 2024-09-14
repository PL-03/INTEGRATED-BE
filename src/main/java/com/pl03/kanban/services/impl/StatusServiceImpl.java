package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.StatusDto;
import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.kanban_entities.*;
import com.pl03.kanban.exceptions.InvalidStatusFieldException;
import com.pl03.kanban.services.StatusService;
import com.pl03.kanban.utils.ListMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StatusServiceImpl implements StatusService {

    private final StatusRepository statusRepository;
    private final TaskV2Repository taskV2Repository;
    private final BoardRepository boardRepository;
    private final ListMapper listMapper;
    private final ModelMapper modelMapper;

    private static final List<String> DEFAULT_STATUS_NAMES = Arrays.asList("No Status", "Done");
    private static final int MAX_STATUS_NAME_LENGTH = 50;
    private static final int MAX_STATUS_DESCRIPTION_LENGTH = 200;

    @Autowired
    public StatusServiceImpl(StatusRepository statusRepository, TaskV2Repository taskV2Repository, BoardRepository boardRepository, ListMapper listMapper, ModelMapper modelMapper) {
        this.statusRepository = statusRepository;
        this.taskV2Repository = taskV2Repository;
        this.boardRepository = boardRepository;
        this.listMapper = listMapper;
        this.modelMapper = modelMapper;
    }


    private boolean isStatusNameDefault(String name) {
        return DEFAULT_STATUS_NAMES.stream()
                .anyMatch(protectedName -> protectedName.equalsIgnoreCase(name)); //return true if status name is matched
    }

    @Override
    public List<StatusDto> getAllStatuses(String boardId) {
        //find board first
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        List<Status> statuses = statusRepository.findByBoardBoardId(boardId);
        return listMapper.mapList(statuses, StatusDto.class, modelMapper);
    }


    @Override
    public StatusDto getStatusById(String boardId, int id) {
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        Status status = statusRepository.findByIdAndBoardBoardId(id, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist in board id: " + boardId));
        return modelMapper.map(status, StatusDto.class);
    }


    @Override
    public StatusDto createStatus(String boardId, StatusDto statusDto) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        ErrorResponse errorResponse = validateStatusFields(statusDto.getName(), statusDto.getDescription(), 0, boardId);

        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidStatusFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        Status status = modelMapper.map(statusDto, Status.class);
        status.setBoard(board);
        Status savedStatus = statusRepository.save(status);
        return modelMapper.map(savedStatus, StatusDto.class);
    }

    @Override
    public StatusDto updateStatus(String boardId, int id, StatusDto updatedStatusDto) {
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        Status status = statusRepository.findByIdAndBoardBoardId(id, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist in board id: " + boardId));

        ErrorResponse errorResponse = validateStatusFields(updatedStatusDto.getName(), updatedStatusDto.getDescription(), id, boardId);

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

        status.setName(updatedStatusDto.getName() == null || updatedStatusDto.getName().trim().isEmpty() ? status.getName()
                : updatedStatusDto.getName().trim());
        status.setDescription(updatedStatusDto.getDescription());
        Status updatedStatus = statusRepository.save(status);
        return modelMapper.map(updatedStatus, StatusDto.class);
    }


    @Override
    public StatusDto deleteStatus(String boardId, int id) {
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        Status status = statusRepository.findByIdAndBoardBoardId(id, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist in board id: " + boardId));

        if (isStatusNameDefault(status.getName())) {
            throw new InvalidStatusFieldException(status.getName() + " cannot be deleted");
        }

        List<TaskV2> tasksWithStatus = taskV2Repository.findByStatus(status);
        if (!tasksWithStatus.isEmpty()) {
            throw new InvalidStatusFieldException("Destination status for task transfer not specified");
        }

        statusRepository.delete(status);
        return modelMapper.map(status, StatusDto.class);
    }

    @Override
    public void deleteStatusAndTransferTasks(String boardId, int id, int newStatusId) {
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        Status currentStatus = statusRepository.findByIdAndBoardBoardId(id, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist in board id: " + boardId));
        Status newStatus = statusRepository.findByIdAndBoardBoardId(newStatusId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + newStatusId + " does not exist in board id: " + boardId));

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
        } else if (statusRepository.existsByNameIgnoreCaseAndBoardBoardId(name, boardId)) {
            errorResponse.addValidationError(Status.Fields.name, "Status name must be unique within the board");
        }

        if (description != null && description.trim().length() > MAX_STATUS_DESCRIPTION_LENGTH) {
            errorResponse.addValidationError(Status.Fields.description, "size must be between 0 and " + MAX_STATUS_DESCRIPTION_LENGTH);
        }

        return errorResponse.getErrors().isEmpty() ? null : errorResponse;
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