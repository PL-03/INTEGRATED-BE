package com.pl03.kanban.services.impl;

import com.pl03.kanban.exceptions.ErrorResponse;
import com.pl03.kanban.entities.Status;
import com.pl03.kanban.entities.TaskV2;
import com.pl03.kanban.exceptions.InvalidStatusFieldException;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.repositories.StatusRepository;
import com.pl03.kanban.repositories.TaskV2Repository;
import com.pl03.kanban.services.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StatusServiceImpl implements StatusService {

    private final StatusRepository statusRepository;
    private final TaskV2Repository taskV2Repository;

    private static final List<String> DEFAULT_STATUS_NAMES = Arrays.asList("No Status", "Done");
    private static final int MAX_STATUS_NAME_LENGTH = 50;
    private static final int MAX_STATUS_DESCRIPTION_LENGTH = 200;
    @Autowired
    public StatusServiceImpl(StatusRepository statusRepository, TaskV2Repository taskV2Repository) {
        this.statusRepository = statusRepository;
        this.taskV2Repository = taskV2Repository;
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
    public List<Status> getAllStatuses() {
        return statusRepository.findAll();
    }

    @Override
    public Status getStatusById(int id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist"));
    }


    @Override
    public Status createStatus(Status status) {
        ErrorResponse errorResponse = validateStatusFields(status.getName(), status.getDescription(), 0);

        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidStatusFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        return statusRepository.save(status);
    }

    @Override
    public Status updateStatus(int id, Status updatedStatus) {
        ErrorResponse errorResponse = validateStatusFields(updatedStatus.getName(), updatedStatus.getDescription(), id);
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist"));

        if (isStatusNameDefault(status.getName())) {
            if (errorResponse != null && !errorResponse.getErrors().isEmpty()) { //if there is sth in error list throw message and error list
                throw new InvalidStatusFieldException(status.getName() + " cannot be modified", errorResponse.getErrors());
            } else { //error list is empty throw only message
                throw new InvalidStatusFieldException(status.getName() + " cannot be modified");
            }
        }

        if (errorResponse != null && !errorResponse.getErrors().isEmpty()) {
            throw new InvalidStatusFieldException("Validation error. Check 'errors' field for details", errorResponse.getErrors());
        }

        // Use the same name if the new name is null or empty
        status.setName(updatedStatus.getName() == null || updatedStatus.getName().trim().isEmpty() ? status.getName()
                : updatedStatus.getName().trim());
        status.setDescription(updatedStatus.getDescription());
        return statusRepository.save(status);
    }


    @Override
    public Status deleteStatus(int id) {
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist"));

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
    public void deleteStatusAndTransferTasks(int id, int newStatusId) {
        Status currentStatus = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist"));
        Status newStatus = statusRepository.findById(newStatusId)
                .orElseThrow(() -> new InvalidStatusFieldException("the specified status for task transfer does not exist"));

        if (isStatusNameDefault(currentStatus.getName())) { //in case when deleting and transfer default status
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


    private ErrorResponse validateStatusFields(String name, String description, int currentStatusId) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation error. Check 'errors' field for details", "");

        if (name == null || name.trim().isEmpty()) {
            errorResponse.addValidationError(Status.Fields.name, "must not be null");
        } else if (name.trim().length() > MAX_STATUS_NAME_LENGTH) {
            errorResponse.addValidationError(Status.Fields.name, "size must be between 0 and " + MAX_STATUS_NAME_LENGTH);
        } else if (isStatusNameTaken(name, currentStatusId)) {
            errorResponse.addValidationError(Status.Fields.name, "must be unique");
        }

        if (description != null && description.trim().length() > MAX_STATUS_DESCRIPTION_LENGTH) {
            errorResponse.addValidationError(Status.Fields.description, "size must be between 0 and " + MAX_STATUS_DESCRIPTION_LENGTH);
        }

        // If there are no validation errors, return null
        if (errorResponse.getErrors().isEmpty()) {
            return null;
        }

        return errorResponse;
    }
}