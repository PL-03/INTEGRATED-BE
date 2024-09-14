package com.pl03.kanban.services;

import com.pl03.kanban.dtos.StatusDto;
import com.pl03.kanban.kanban_entities.Status;

import java.util.List;

public interface StatusService {
    StatusDto createStatus(String boardId, StatusDto status);
    List<StatusDto> getAllStatuses(String boardId);
    StatusDto getStatusById(String boardId, int id);
    StatusDto updateStatus(String boardId, int id, StatusDto status);
    StatusDto deleteStatus(String boardId, int id);
    void deleteStatusAndTransferTasks(String boardId, int id, int newStatusId);
    void addDefaultStatus(String boardId);

}