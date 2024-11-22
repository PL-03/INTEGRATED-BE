package com.pl03.kanban.services;

import com.pl03.kanban.kanban_entities.TaskV3;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    List<String> validateAndStoreFiles(List<MultipartFile> files, TaskV3 task);
    void deleteFiles(List<Long> fileIds, TaskV3 task);
    Resource loadFileAsResource(String fileName, int taskId);
}
