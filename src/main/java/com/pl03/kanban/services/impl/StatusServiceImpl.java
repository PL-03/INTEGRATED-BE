package com.pl03.kanban.services.impl;

import com.pl03.kanban.dtos.StatusDto;
import com.pl03.kanban.entities.Status;
import com.pl03.kanban.entities.TaskV2;
import com.pl03.kanban.exceptions.TaskNotFoundException;
import com.pl03.kanban.repositories.StatusRepository;
import com.pl03.kanban.repositories.TaskV2Repository;
import com.pl03.kanban.services.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatusServiceImpl implements StatusService {

    private final StatusRepository statusRepository;

    @Autowired
    public StatusServiceImpl(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @Override
    public StatusDto createStatus(StatusDto statusDto) {
        Status status = mapToEntity(statusDto);
        Status savedStatus = statusRepository.save(status);
        return mapToDto(savedStatus);
    }

    @Override
    public List<StatusDto> getAllStatuses() {
        List<Status> statuses = statusRepository.findAll();
        return statuses.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public StatusDto getStatusById(int id) {
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Status with id " + id + " does not exist"));
        return mapToDto(status);
    }


    private Status mapToEntity(StatusDto statusDto) {
        Status status = new Status();
        status.setName(statusDto.getName());
        status.setDescription(statusDto.getDescription());
        return status;
    }

    private StatusDto mapToDto(Status status) {
        StatusDto statusDto = new StatusDto();
        statusDto.setId(status.getStatusId());
        statusDto.setName(status.getName());
        statusDto.setDescription(status.getDescription());
        return statusDto;
    }
}