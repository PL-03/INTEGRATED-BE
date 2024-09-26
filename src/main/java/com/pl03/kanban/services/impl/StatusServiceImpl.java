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

    private final StatusV3Repository statusV3Repository;
    private final TaskV3Repository taskV3Repository;
    private final BoardRepository boardRepository;
    private final ListMapper listMapper;
    private final ModelMapper modelMapper;

    private static final List<String> DEFAULT_STATUS_NAMES = Arrays.asList("No Status", "Done");
    private static final int MAX_STATUS_NAME_LENGTH = 50;
    private static final int MAX_STATUS_DESCRIPTION_LENGTH = 200;

    @Autowired
    public StatusServiceImpl(StatusV3Repository statusV3Repository, TaskV3Repository taskV3Repository, BoardRepository boardRepository, ListMapper listMapper, ModelMapper modelMapper) {
        this.statusV3Repository = statusV3Repository;
        this.taskV3Repository = taskV3Repository;
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

        List<StatusV3> statusV3s = statusV3Repository.findByBoardId(boardId);
        return listMapper.mapList(statusV3s, StatusDto.class, modelMapper);
    }


    @Override
    public StatusDto getStatusById(String boardId, int id) {
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        StatusV3 statusV3 = statusV3Repository.findByIdAndBoardId(id, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist in board id: " + boardId));
        return modelMapper.map(statusV3, StatusDto.class);
    }


    @Override
    public StatusDto createStatus(String boardId, StatusDto statusDto) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        ErrorResponse errorResponse = validateStatusFields(statusDto.getName(), statusDto.getDescription(), 0, boardId);

        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidStatusFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        StatusV3 statusV3 = modelMapper.map(statusDto, StatusV3.class);
        statusV3.setBoard(board);
        StatusV3 savedStatusV3 = statusV3Repository.save(statusV3);
        return modelMapper.map(savedStatusV3, StatusDto.class);
    }

    @Override
    public StatusDto updateStatus(String boardId, int id, StatusDto updatedStatusDto) {
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        StatusV3 statusV3 = statusV3Repository.findByIdAndBoardId(id, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist in board id: " + boardId));

        ErrorResponse errorResponse = validateStatusFields(updatedStatusDto.getName(), updatedStatusDto.getDescription(), id, boardId);

        if (isStatusNameDefault(statusV3.getName())) {
            if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
                throw new InvalidStatusFieldException(statusV3.getName() + " cannot be modified", errorResponse.getErrors());
            } else {
                throw new InvalidStatusFieldException(statusV3.getName() + " cannot be modified");
            }
        }

        // Check if the status name is different before validating for uniqueness
        if (!statusV3.getName().equalsIgnoreCase(updatedStatusDto.getName())) {
            errorResponse = validateStatusFields(updatedStatusDto.getName(), updatedStatusDto.getDescription(), id, boardId);

            // If validation errors are found, throw an exception
            if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
                throw new InvalidStatusFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
            }
        }

        statusV3.setName(updatedStatusDto.getName() == null || updatedStatusDto.getName().trim().isEmpty() ? statusV3.getName()
                : updatedStatusDto.getName().trim());
        statusV3.setDescription(updatedStatusDto.getDescription());
        StatusV3 updatedStatusV3 = statusV3Repository.save(statusV3);
        return modelMapper.map(updatedStatusV3, StatusDto.class);
    }


    @Override
    public StatusDto deleteStatus(String boardId, int id) {
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        StatusV3 statusV3 = statusV3Repository.findByIdAndBoardId(id, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist in board id: " + boardId));

        if (isStatusNameDefault(statusV3.getName())) {
            throw new InvalidStatusFieldException(statusV3.getName() + " cannot be deleted");
        }

        List<TaskV3> tasksWithStatus = taskV3Repository.findByStatusV3(statusV3);
        if (!tasksWithStatus.isEmpty()) {
            throw new InvalidStatusFieldException("Destination status for task transfer not specified");
        }

        statusV3Repository.delete(statusV3);
        return modelMapper.map(statusV3, StatusDto.class);
    }

    @Override
    public void deleteStatusAndTransferTasks(String boardId, int id, int newStatusId) {
        boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        StatusV3 currentStatusV3 = statusV3Repository.findByIdAndBoardId(id, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist in board id: " + boardId));
        StatusV3 newStatusV3 = statusV3Repository.findByIdAndBoardId(newStatusId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + newStatusId + " does not exist in board id: " + boardId));

        if (isStatusNameDefault(currentStatusV3.getName())) {
            throw new InvalidStatusFieldException(currentStatusV3.getName() + " cannot be deleted");
        }

        if (id == newStatusId) {
            throw new InvalidStatusFieldException("destination status for task transfer must be different from current status");
        }

        List<TaskV3> tasksWithCurrentStatus = taskV3Repository.findByStatusV3(currentStatusV3);
        tasksWithCurrentStatus.forEach(task -> task.setStatusV3(newStatusV3));
        taskV3Repository.saveAll(tasksWithCurrentStatus);

        statusV3Repository.delete(currentStatusV3);
    }


    private ErrorResponse validateStatusFields(String name, String description, int currentStatusId, String boardId) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation error. Check 'errors' field for details", "");

        if (name == null || name.trim().isEmpty()) {
            errorResponse.addValidationError(StatusV3.Fields.name, "must not be null");
        } else if (name.trim().length() > MAX_STATUS_NAME_LENGTH) {
            errorResponse.addValidationError(StatusV3.Fields.name, "size must be between 0 and " + MAX_STATUS_NAME_LENGTH);
        } else if (statusV3Repository.existsByNameIgnoreCaseAndBoardId(name, boardId)) {
            errorResponse.addValidationError(StatusV3.Fields.name, "Status name must be unique within the board");
        }

        if (description != null && description.trim().length() > MAX_STATUS_DESCRIPTION_LENGTH) {
            errorResponse.addValidationError(StatusV3.Fields.description, "size must be between 0 and " + MAX_STATUS_DESCRIPTION_LENGTH);
        }

        return errorResponse.getErrors().isEmpty() ? null : errorResponse;
    }


    @Override
    public void addDefaultStatus(String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board with id " + boardId + " does not exist"));

        // Use the constructor without timestamps
        List<StatusV3> defaultStatusV3s = Arrays.asList(
                new StatusV3(0, "No Status", "A status has not been assigned", board),
                new StatusV3(0, "To Do", "The task is included in the project", board),
                new StatusV3(0, "Doing", "The task is being worked on", board),
                new StatusV3(0, "Done", "The task has been completed", board)
        );

        // Save all the default statuses to the database
        statusV3Repository.saveAll(defaultStatusV3s);
    }
}