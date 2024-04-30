package com.pl03.kanban.dtos;

import com.pl03.kanban.entities.Task;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GetAllTaskDto {
    private int id;
    private String title;
    private String assignees;
    private Task.TaskStatus status;

    public GetAllTaskDto(int id, String title, String assignees, Task.TaskStatus status) {
        this.id = id;
        this.title = title;
        this.assignees = assignees;
        this.status = status;
    }
}