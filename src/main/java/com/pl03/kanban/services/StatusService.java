package com.pl03.kanban.services;

import com.pl03.kanban.entities.Status;

import java.util.List;

public interface StatusService {
    Status createStatus(String name, String description);
    List<Status> getAllStatuses();
    Status getStatusById(int id);
    Status updateStatus(int id, String name, String description);
    Status deleteStatus(int id);
    void deleteStatusAndTransferTasks(int id, int newStatusId);
}