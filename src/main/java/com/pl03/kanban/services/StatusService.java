package com.pl03.kanban.services;

import com.pl03.kanban.dtos.StatusDto;

import java.util.List;

public interface StatusService {
    StatusDto createStatus(String boardId, StatusDto status, String userId);
    List<StatusDto> getAllStatuses(String boardId, String userId);
    StatusDto getStatusById(String boardId, int id, String userId);
    StatusDto updateStatus(String boardId, int id, StatusDto status, String userId);
    StatusDto deleteStatus(String boardId, int id, String userId);
    void deleteStatusAndTransferTasks(String boardId, int id, int newStatusId, String userId);
    void addDefaultStatus(String boardId);

}