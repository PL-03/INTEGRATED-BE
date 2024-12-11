package com.pl03.kanban.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileAttachmentDto {
    private Long id;
    private String name;
    private String type;
    private Timestamp addedOn;
}
