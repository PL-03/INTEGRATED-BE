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
        this.title = title == null ? null : title.trim();
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public void setAssignees(String assignees) {
        this.assignees = assignees == null ? null : assignees.trim();
    }

    public void setStatus(String status) {
        this.status = status == null ? "NO_STATUS" : status; //defaut is NO_STATUS
    }
}
