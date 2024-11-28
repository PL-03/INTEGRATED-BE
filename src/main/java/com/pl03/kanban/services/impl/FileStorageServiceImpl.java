package com.pl03.kanban.services.impl;

import com.pl03.kanban.exceptions.ConflictException;
import com.pl03.kanban.exceptions.InvalidFileException;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.kanban_entities.FileStorage;
import com.pl03.kanban.kanban_entities.TaskV3;
import com.pl03.kanban.kanban_entities.repositories.FileStorageRepository;
import com.pl03.kanban.kanban_entities.repositories.TaskV3Repository;
import com.pl03.kanban.services.FileStorageService;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(transactionManager = "kanbanTransactionManager")
public class FileStorageServiceImpl implements FileStorageService {
    private final Path fileStorageLocation;
    private final FileStorageRepository fileStorageRepository;
    private final TaskV3Repository taskV3Repository;

    public static final int MAX_FILES = 10;
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    @Autowired
    public FileStorageServiceImpl(FileStorageRepository fileStorageRepository, TaskV3Repository taskV3Repository) {
        this.fileStorageRepository = fileStorageRepository;
        this.taskV3Repository = taskV3Repository;
        this.fileStorageLocation = Paths.get("task-attachments").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }
    @Override
    public List<String> validateAndStoreFiles(List<MultipartFile> files, TaskV3 task) {
        // Return early if files list is null or empty
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> unaddedFiles = new ArrayList<>();
        Set<String> existingFilenames = task.getFiles().stream()
                .map(FileStorage::getName)
                .collect(Collectors.toSet());

        int availableSlots = MAX_FILES - existingFilenames.size();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);

            // Skip if the file is empty or has no original filename
            if (file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
                continue;
            }

            if (i >= availableSlots) {
                unaddedFiles.add(file.getOriginalFilename());
                continue;
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new InvalidFileException("File " + file.getOriginalFilename() + " exceeds the maximum size of 20MB");
            }

            if (existingFilenames.contains(file.getOriginalFilename())) {
                throw new ConflictException("File " + file.getOriginalFilename() + " already exists in this task");
            }

            try {
                // Generate a unique name for storage
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;

                // Validate content type before proceeding
                String contentType = file.getContentType();
                if (contentType == null || contentType.trim().isEmpty()) {
                    throw new InvalidFileException("File " + originalFilename + " has invalid content type");
                }

                Path targetLocation = this.fileStorageLocation.resolve(uniqueFilename);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                // Store metadata
                FileStorage fileStorage = new FileStorage();
                fileStorage.setName(originalFilename);
                fileStorage.setType(contentType);
                fileStorage.setPath(targetLocation.toString());
                fileStorage.setTask(task);

                fileStorageRepository.save(fileStorage);
                task.getFiles().add(fileStorage);
                existingFilenames.add(originalFilename);

            } catch (IOException ex) {
                throw new RuntimeException("Could not store file " + file.getOriginalFilename(), ex);
            }
        }

        return unaddedFiles;
    }

    @Override
    public void deleteFilesByNames(Set<String> fileNames, TaskV3 task) {
        if (fileNames == null || fileNames.isEmpty() || task.getFiles() == null || task.getFiles().isEmpty()) {
            return;
        }

        List<FileStorage> filesToDelete = task.getFiles().stream()
                .filter(file -> fileNames.contains(file.getName()))
                .collect(Collectors.toList());

        for (FileStorage file : filesToDelete) {
            if (file.getType() == null) {
                // Log the issue but continue with deletion
                System.out.println(("FileStorage entity has a null 'type' property: {}" + file.getName()));
            }

            try {
                // Delete the file from the file system
                Path filePath = Paths.get(file.getPath()).normalize();
                boolean deleted = Files.deleteIfExists(filePath);
                if (!deleted) {
                    System.out.println("File not found on filesystem: {}"+ file.getPath());
                }

                // Remove file metadata from the database
                fileStorageRepository.delete(file);

            } catch (IOException e) {
                System.out.println("Error deleting file: " + file.getName() + e);
                throw new RuntimeException("Could not delete file: " + file.getName(), e);
            }
        }

        // Remove files from the task's collection
        task.getFiles().removeAll(filesToDelete);
    }

    @Override
    public void deleteAllFiles(TaskV3 task) {
        if (task.getFiles() == null || task.getFiles().isEmpty()) {
            return;
        }

        Set<String> fileNames = task.getFiles().stream()
                .map(FileStorage::getName)
                .collect(Collectors.toSet());

        try {
            deleteFilesByNames(fileNames, task);
        } catch (RuntimeException e) {
            // Log error but allow the task deletion to continue
            System.err.println("Error deleting files for task ID: " + task.getId() + ". Error: " + e.getMessage());
        }
    }


//    @Override
//    public void deleteAllTaskFiles(TaskV3 task) {
//        if (task.getFiles() == null || task.getFiles().isEmpty()) {
//            return;
//        }
//
//        List<FileStorage> filesToDelete = new ArrayList<>(task.getFiles());
//
//        for (FileStorage file : filesToDelete) {
//            try {
//                // Delete physical file
//                Path filePath = Paths.get(file.getPath()).normalize();
//                Files.deleteIfExists(filePath);
//
//                // Remove from database
//                fileStorageRepository.delete(file);
//            } catch (IOException e) {
//                throw new RuntimeException("Could not delete file: " + file.getName(), e);
//            }
//        }
//
//        // Clear the task's files collection
//        task.getFiles().clear();
//    }

    @Override
    public Set<Long> getExistingFileIds(int taskId) {
        return fileStorageRepository.findByTaskId(taskId)
                .stream()
                .map(FileStorage::getId)
                .collect(Collectors.toSet());
    }


    @Override
    public Resource loadFileAsResource(String fileName, int taskId) {
        FileStorage fileMetadata = fileStorageRepository.findByNameAndTask_Id(fileName, taskId)
                .orElseThrow(() -> new ItemNotFoundException("File not found or does not belong to the specified task"));

        try {
            Path filePath = Paths.get(fileMetadata.getPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ItemNotFoundException("File not found or unreadable at location " + fileMetadata.getPath());
            }
        } catch (MalformedURLException ex) {
            throw new ItemNotFoundException("File not found or invalid URI " + fileMetadata.getPath());
        }
    }

    @Override
    public String getFileContentType(Resource resource) {
        try {
            return Files.probeContentType(Paths.get(resource.getURI()));
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE; // Default to binary if type cannot be determined
        }
    }
//    @Override
//    public List<Long> getExistingFileIds(int taskId) {
//        // Fetch the list of files related to the task using fileStorageRepository
//        List<FileStorage> fileStorageList = fileStorageRepository.findByTaskId(taskId);
//
//        // Extract file IDs from the fileStorage list
//        return fileStorageList.stream()
//                .map(FileStorage::getId)  // Get the ID of each FileStorage entity
//                .collect(Collectors.toList());
//    }


}
