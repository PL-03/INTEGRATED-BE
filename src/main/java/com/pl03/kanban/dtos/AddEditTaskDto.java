package com.pl03.kanban.dtos;

import lombok.Data;

@Data
public class AddEditTaskDto {
    private int id;
    private String title;
    private String description;
    private String assignees;
    private String status;

    public void setTitle(String title) {
        this.title = title == null || title.trim().isEmpty() ? null : title.trim();
    }

    public void setDescription(String description) {
        this.description = description == null || description.trim().isEmpty() ? null : description.trim();
    }

    public void setAssignees(String assignees) {
        this.assignees = assignees == null || assignees.trim().isEmpty() ? null : assignees.trim();
    }
}