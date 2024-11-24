package com.pl03.kanban.dtos;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class AddEditTaskDto {
    private int id;
    private String title;
    private String description;
    private String assignees;
    private String status;
    private String boardId;
}