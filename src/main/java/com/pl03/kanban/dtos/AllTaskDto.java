package com.pl03.kanban.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AllTaskDto {
    private int id;
    private String taskTitle;
    private String taskAssignees;
    private String taskStatus;

    public AllTaskDto(int id, String taskTitle, String taskAssignees, String taskStatus) {
        this.id = id;
        this.taskTitle = taskTitle;
        this.taskAssignees = taskAssignees;
        this.taskStatus = taskStatus;
    }
}
