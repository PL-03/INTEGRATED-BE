package com.pl03.kanban.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@FieldNameConstants
public class AddEditTaskDto {
    private int id;
    private String title;
    private String description;
    private String assignees;
    private String status;
    private String boardId;
    private FileStorageDto[] attachments;

    @JsonIgnore
    private List<Long> filesToDelete;
    @JsonIgnore
    private List<MultipartFile> newFiles;
}