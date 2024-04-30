package com.pl03.kanban.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GetAllTaskDto {
    private int id;
    private String title;
    private String assignees;
    private String status;

    public GetAllTaskDto(int id, String title, String assignees, String status) {
        this.id = id;
        this.title = title;
        this.assignees = assignees;
        this.status = status;
    }
}
