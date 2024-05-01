package com.pl03.kanban.dtos;

import com.pl03.kanban.entities.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetAllTaskDto {
    private int id;
    private String title;
    private String assignees;
    private Task.TaskStatus status;
}