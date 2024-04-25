package com.pl03.kanban.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
public class GetTaskDto {
    private int id;
    private String taskTitle;
    private String taskDescription;
    private String taskAssignees;
    private String taskStatus;
    private Timestamp createdOn;
    private Timestamp updatedOn;

    public GetTaskDto(int id, String taskTitle, String taskDescription, String taskAssignees, String taskStatus, Timestamp createdOn, Timestamp updatedOn) {
        this.id = id;
        this.taskTitle = taskTitle;
        this.taskDescription = taskDescription;
        this.taskAssignees = taskAssignees;
        this.taskStatus = taskStatus;
        this.createdOn = createdOn;
        this.updatedOn = updatedOn;
    }
}
