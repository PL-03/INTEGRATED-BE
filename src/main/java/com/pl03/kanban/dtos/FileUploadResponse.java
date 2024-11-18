package com.pl03.kanban.dtos;

import lombok.Data;

@Data
public class FileUploadResponse {
    private boolean success;
    private String message;
    private FileStorageDto[] attachments;
}
