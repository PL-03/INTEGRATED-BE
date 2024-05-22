package com.pl03.kanban.services.impl;

import com.pl03.kanban.entities.Status;
import com.pl03.kanban.entities.TaskV2;
import com.pl03.kanban.exceptions.InvalidStatusFieldException;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.repositories.StatusRepository;
import com.pl03.kanban.repositories.TaskV2Repository;
import com.pl03.kanban.services.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Status createStatus(String name, String description) {
        List<Map<String, String>> errors = validateStatusFields(name, description, 0);

        if (!errors.isEmpty()) {
            throw new InvalidStatusFieldException("Validation error. Check 'errors' field for details", errors);
        }

        Status status = new Status();
        status.setName(name.trim());
        status.setDescription(description == null || description.trim().isEmpty() ? null : description.trim());
        return statusRepository.save(status);
    }

    @Override
    public Status updateStatus(int id, String name, String description) {
        List<Map<String, String>> errors = validateStatusFields(name, description, id); // Pass the id to exclude it from uniqueness check
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist"));

        if (isStatusNameDefault(status.getName())) {
            throw new InvalidStatusFieldException(status.getName() + " cannot be modified", errors);
        }

        if (!errors.isEmpty()) {
            throw new InvalidStatusFieldException("Validation error. Check 'errors' field for details", errors);
        }

        // Use the same name if the new name is null or empty
        status.setName(name == null || name.trim().isEmpty() ? status.getName() : name.trim());
        status.setDescription(description == null || description.trim().isEmpty() ? null : description.trim());
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
        if (id == newStatusId) {
            throw new InvalidStatusFieldException("destination status for task transfer must be different from current status");
        }
        List<TaskV2> tasksWithCurrentStatus = taskV2Repository.findByStatus(currentStatus);
        tasksWithCurrentStatus.forEach(task -> task.setStatus(newStatus));
        taskV2Repository.saveAll(tasksWithCurrentStatus);

        statusRepository.delete(currentStatus);
    }


    private List<Map<String, String>> validateStatusFields(String name, String description, int currentStatusId) {
        List<Map<String, String>> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", Status.Fields.name);
            error.put("message", "must not be null");
            errors.add(error);
        } else if (name.trim().length() > MAX_STATUS_NAME_LENGTH) {
            Map<String, String> error = new HashMap<>();
            error.put("field", Status.Fields.name);
            error.put("message", "size must be between 0 and " + MAX_STATUS_NAME_LENGTH );
            errors.add(error);
        } else if (isStatusNameTaken(name, currentStatusId)) { // Pass the currentStatusId to exclude it from uniqueness check
            Map<String, String> error = new HashMap<>();
            error.put("field", Status.Fields.name);
            error.put("message", "must be unique");
            errors.add(error);
        }

        if (description != null && description.trim().length() > MAX_STATUS_DESCRIPTION_LENGTH) {
            Map<String, String> error = new HashMap<>();
            error.put("field", Status.Fields.description);
            error.put("message", "size must be between 0 and " + MAX_STATUS_DESCRIPTION_LENGTH);
            errors.add(error);
        }

        return errors;
    }
}