package com.pl03.kanban.dtos;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TaskDetailDtoWithAttachments extends TaskDetailDto {
    private List<FileAttachmentDto> attachments = new ArrayList<>();
}
