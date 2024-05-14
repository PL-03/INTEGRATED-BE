package com.pl03.kanban.services.impl;

import com.pl03.kanban.entities.Status;
import com.pl03.kanban.entities.TaskV2;
import com.pl03.kanban.exceptions.InvalidStatusNameException;
import com.pl03.kanban.exceptions.TaskNotFoundException;
import com.pl03.kanban.repositories.StatusRepository;
import com.pl03.kanban.repositories.TaskV2Repository;
import com.pl03.kanban.services.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Override
    public Status createStatus(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Status name cannot be empty");
        }
        Status status = new Status();
        status.setName(name.trim());
        status.setDescription(description == null || description.trim().isEmpty() ? null : description.trim());
        return statusRepository.save(status);
    }

    @Override
    public List<Status> getAllStatuses() {
        return statusRepository.findAll();
    }

    @Override
    public Status getStatusById(int id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Status with id " + id + " does not exist"));
    }

    @Override
    public Status updateStatus(int id, String name, String description) {
        if (id == 1) {
            throw new IllegalArgumentException("Cannot modify the default 'No Status' status");
        }
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Status with id " + id + " does not exist"));

        if (name == null || name.trim().isEmpty()) {
            throw new InvalidStatusNameException("Status name cannot be null or empty");
        }

        status.setName(name.trim());
        status.setDescription(description == null || description.trim().isEmpty() ? null : description.trim());
        return statusRepository.save(status);
    }

    @Override
    public Status deleteStatus(int id) {
        if (id == 1) {
            throw new IllegalArgumentException("Cannot delete the default 'No Status' status");
        }
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Status with id " + id + " does not exist"));
        statusRepository.delete(status);
        return status;
    }

    @Override
    public void deleteStatusAndTransferTasks(int id, int newStatusId) {
        Status currentStatus = statusRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Status with id " + id + " does not exist"));
        Status newStatus = statusRepository.findById(newStatusId)
                .orElseThrow(() -> new TaskNotFoundException("Status with id " + newStatusId + " does not exist"));

        List<TaskV2> tasksWithCurrentStatus = taskV2Repository.findByStatus(currentStatus);
        tasksWithCurrentStatus.forEach(task -> task.setStatus(newStatus));
        taskV2Repository.saveAll(tasksWithCurrentStatus);

        statusRepository.delete(currentStatus);
    }
}