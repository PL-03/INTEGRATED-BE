package com.pl03.kanban.services.impl;

import com.pl03.kanban.entities.Status;
import com.pl03.kanban.entities.TaskV2;
import com.pl03.kanban.exceptions.InvalidStatusFiledException;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.repositories.StatusRepository;
import com.pl03.kanban.repositories.TaskV2Repository;
import com.pl03.kanban.services.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class StatusServiceImpl implements StatusService {

    private final StatusRepository statusRepository;
    private final TaskV2Repository taskV2Repository;

    @Autowired
    public StatusServiceImpl(StatusRepository statusRepository, TaskV2Repository taskV2Repository) {
        this.statusRepository = statusRepository;
        this.taskV2Repository = taskV2Repository;
    }

    private static final List<String> DEFAULT_STATUS_NAMES = Arrays.asList("No Status", "Done");

    private boolean isStatusNameDefault(String name) {
        return DEFAULT_STATUS_NAMES.stream()
                .anyMatch(protectedName -> protectedName.equalsIgnoreCase(name)); //return true if status name is matched
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
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidStatusFiledException("Status name cannot be null or empty");
        }
        if (isStatusNameTaken(name, 0)) {
            throw new InvalidStatusFiledException("Status name is already taken");
        }
        Status status = new Status();
        status.setName(name.trim());
        status.setDescription(description == null || description.trim().isEmpty() ? null : description.trim());
        return statusRepository.save(status);
    }

    @Override
    public Status updateStatus(int id, String name, String description) {
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist"));

        if (isStatusNameDefault(status.getName())) {
            throw new InvalidStatusFiledException("Cannot edit default status");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new InvalidStatusFiledException("Status name cannot be null or empty");
        }
        if (isStatusNameTaken(name, id)) {
            throw new InvalidStatusFiledException("Status name is already taken");
        }

        status.setName(name.trim());
        status.setDescription(description == null || description.trim().isEmpty() ? null : description.trim());
        return statusRepository.save(status);
    }

    @Override
    public Status deleteStatus(int id) {
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist"));

        if (isStatusNameDefault(status.getName())) {
            throw new InvalidStatusFiledException("Cannot delete default status");
        }

        statusRepository.delete(status);
        return status;
    }

    @Override
    public void deleteStatusAndTransferTasks(int id, int newStatusId) {
        Status currentStatus = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + id + " does not exist"));
        Status newStatus = statusRepository.findById(newStatusId)
                .orElseThrow(() -> new ItemNotFoundException("Status with id " + newStatusId + " does not exist"));

        List<TaskV2> tasksWithCurrentStatus = taskV2Repository.findByStatus(currentStatus);
        tasksWithCurrentStatus.forEach(task -> task.setStatus(newStatus));
        taskV2Repository.saveAll(tasksWithCurrentStatus);

        statusRepository.delete(currentStatus);
    }

    private boolean isStatusNameTaken(String name, int excludedId) {
        List<Status> statuses = statusRepository.findByNameIgnoreCaseAndIdNot(name.trim().toUpperCase(), excludedId);
        return !statuses.isEmpty();
    }
}