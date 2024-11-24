package com.pl03.kanban.services;

import com.pl03.kanban.kanban_entities.TaskV3;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public interface FileStorageService {
    List<String> validateAndStoreFiles(List<MultipartFile> files, TaskV3 task);
    void deleteAllFiles(TaskV3 task);
    Resource loadFileAsResource(String fileName, int taskId);
    String getFileContentType(Resource resource);
    void deleteFilesByNames(Set<String> fileNames, TaskV3 task);
//    void deleteAllTaskFiles(TaskV3 task);
    Set<Long> getExistingFileIds(int taskId);
}
