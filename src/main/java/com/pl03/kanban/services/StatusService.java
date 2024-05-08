package com.pl03.kanban.services;

import com.pl03.kanban.dtos.StatusDto;

import java.util.List;

public interface StatusService {
    StatusDto createStatus(StatusDto statusDto);
    List<StatusDto> getAllStatuses();
    StatusDto getStatusById(int id);
}