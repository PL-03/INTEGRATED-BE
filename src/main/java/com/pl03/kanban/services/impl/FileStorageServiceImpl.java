package com.pl03.kanban.services.impl;

import com.pl03.kanban.exceptions.ConflictException;
import com.pl03.kanban.exceptions.InvalidFileException;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.kanban_entities.FileStorage;
import com.pl03.kanban.kanban_entities.TaskV3;
import com.pl03.kanban.kanban_entities.repositories.FileStorageRepository;
import com.pl03.kanban.kanban_entities.repositories.TaskV3Repository;
import com.pl03.kanban.services.FileStorageService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        List<String> unaddedFiles = new ArrayList<>();
        Set<String> existingFilenames = task.getFiles().stream()
                .map(FileStorage::getName)
                .collect(Collectors.toSet());

        int availableSlots = MAX_FILES - existingFilenames.size();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);

            // Skip if we've reached the limit
            if (i >= availableSlots) {
                unaddedFiles.add(file.getOriginalFilename());
                continue;
            }

            // Validate file size
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new InvalidFileException("File " + file.getOriginalFilename() + " exceeds maximum size of 20MB");
            }

            // Check for duplicate filename in current task
            if (existingFilenames.contains(file.getOriginalFilename())) {
                throw new ConflictException("File " + file.getOriginalFilename() + " already exists in this task");
            }

            try {
                // store file
                String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                Path targetLocation = this.fileStorageLocation.resolve(fileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                // Create file storage entry
                FileStorage fileStorage = new FileStorage();
                fileStorage.setName(fileName); // Store original filename for display
                fileStorage.setType(file.getContentType());
                fileStorage.setPath(targetLocation.toString());
                fileStorage.setTask(task);

                fileStorageRepository.save(fileStorage);
                task.getFiles().add(fileStorage); // Add to task's collection
                existingFilenames.add(fileName);

            } catch (IOException ex) {
                throw new RuntimeException("Could not store file " + file.getOriginalFilename(), ex);
            }
        }

        return unaddedFiles;
    }

    @Override
    public void deleteFiles(List<Long> fileIds, TaskV3 task) {
        for (Long fileId : fileIds) {
            FileStorage file = fileStorageRepository.findById(fileId)
                    .orElseThrow(() -> new ItemNotFoundException("File not found with id: " + fileId));

            if (file.getTask().getId() != (task.getId())) {
                throw new ItemNotFoundException("File " + fileId + " does not belong to task " + task.getId());
            }

            try {
                Files.deleteIfExists(Paths.get(file.getPath()));
                fileStorageRepository.delete(file);
            } catch (IOException ex) {
                throw new RuntimeException("Error deleting file " + file.getName(), ex);
            }
        }
    }
    @Override
    public Resource loadFileAsResource(String fileName, int taskId) {
        FileStorage fileMetadata = fileStorageRepository.findByName(fileName)
                .filter(file -> file.getTask().getId() == taskId)
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

    public String getFileContentType(Resource resource) {
        try {
            return Files.probeContentType(Paths.get(resource.getURI()));
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE; // Default to binary if type cannot be determined
        }
    }
}
