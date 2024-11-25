package com.pl03.kanban.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class AddEditTaskDtoWithAttachments extends AddEditTaskDto {
    @JsonIgnore
    private List<MultipartFile> newAttachments;
    @JsonIgnore
    private List<Long> attachmentsToDelete;
    private List<FileAttachmentDto> existingAttachments;
}
