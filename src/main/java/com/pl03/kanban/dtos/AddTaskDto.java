package com.pl03.kanban.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AddTaskDto {
    private int id;
    private String title;
    private String description;
    private String assignees;
    private String status;
}