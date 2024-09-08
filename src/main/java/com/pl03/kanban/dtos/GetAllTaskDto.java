package com.pl03.kanban.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class GetAllTaskDto {
    private int id;
    private String title;
    private String assignees;
    private String status;
    private String boardId;
}