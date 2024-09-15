package com.pl03.kanban.dtos;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.sql.Timestamp;

@Data
@FieldNameConstants
public class TaskDetailDto {
    private int id;
    private String title;
    private String description;
    private String assignees;
    private String status;
    private String boardId;
    private Timestamp createdOn;
    private Timestamp updatedOn;
}
