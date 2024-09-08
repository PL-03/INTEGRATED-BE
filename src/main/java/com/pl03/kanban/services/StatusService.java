package com.pl03.kanban.services;

import com.pl03.kanban.kanban_entities.Status;

import java.util.List;

public interface StatusService {
    Status createStatus(String boardId, Status status);
    List<Status> getAllStatuses(String boardId);
    Status getStatusById(String boardId, int id);
    Status updateStatus(String boardId, int id, Status status);
    Status deleteStatus(String boardId, int id);
    void deleteStatusAndTransferTasks(String boardId, int id, int newStatusId);
    void addDefaultStatus(String boardId);

}