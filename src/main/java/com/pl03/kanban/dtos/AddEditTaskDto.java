package com.pl03.kanban.dtos;

import lombok.Data;

@Data
public class AddEditTaskDto {
    private int id;
    private String title;
    private String description;
    private String assignees;
    private String status;
}
