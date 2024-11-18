package com.pl03.kanban.dtos;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
    private FileStorageDto[] attachments;
}
