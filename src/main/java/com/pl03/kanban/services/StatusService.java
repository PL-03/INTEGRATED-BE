package com.pl03.kanban.services;

import com.pl03.kanban.kanban_entities.Status;

import java.util.List;

public interface StatusService {
    Status createStatus(Status status);
    List<Status> getAllStatuses();
    Status getStatusById(int id);
    Status updateStatus(int id, Status status);
    Status deleteStatus(int id);
    void deleteStatusAndTransferTasks(int id, int newStatusId);
}