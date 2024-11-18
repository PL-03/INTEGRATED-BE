package com.pl03.kanban.services;

import com.pl03.kanban.dtos.FileStorageDto;
import com.pl03.kanban.dtos.FileUploadResponse;
import com.pl03.kanban.exceptions.ConflictException;
import com.pl03.kanban.exceptions.InvalidFileException;
import com.pl03.kanban.exceptions.ItemNotFoundException;
import com.pl03.kanban.kanban_entities.FileStorage;
import com.pl03.kanban.kanban_entities.Repositories.FileStorageRepository;
import com.pl03.kanban.kanban_entities.Repositories.TaskV3Repository;
import com.pl03.kanban.kanban_entities.TaskV3;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileStorageService {
    private static final int MAX_FILES = 10;
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB

    @Autowired
    private FileStorageRepository fileStorageRepository;
    @Autowired
    private TaskV3Repository taskV3Repository;
    @Autowired
    private ModelMapper modelMapper;

    public FileUploadResponse uploadFiles(String boardId, int taskId, List<MultipartFile> files, String userId) {
        // Find the task
        TaskV3 task = taskV3Repository.findByIdAndBoardId(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board id: " + boardId));

        // Check existing attachments
        List<FileStorage> existingAttachments = fileStorageRepository.findByTask(task);
        int currentAttachmentCount = existingAttachments.size();

        FileUploadResponse response = new FileUploadResponse();
        List<String> rejectedFiles = new ArrayList<>();
        List<FileStorage> attachmentsToSave = new ArrayList<>();

        // Process files
        for (MultipartFile file : files) {
            // Check if we've reached MAX_FILES
            if (currentAttachmentCount >= MAX_FILES) {
                rejectedFiles.add(file.getOriginalFilename());
                continue;
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                rejectedFiles.add(file.getOriginalFilename());
                continue;
            }

            // Check unique filename within task
            boolean filenameExists = existingAttachments.stream()
                    .anyMatch(f -> f.getName().equals(file.getOriginalFilename()));

            if (filenameExists) {
                rejectedFiles.add(file.getOriginalFilename());
                continue;
            }

            try {
                FileStorage fileStorage = new FileStorage();
                fileStorage.setName(file.getOriginalFilename());
                fileStorage.setType(file.getContentType());
                fileStorage.setFileData(file.getBytes());
                fileStorage.setAddedOn(new Timestamp(System.currentTimeMillis()));
                fileStorage.setTask(task);

                attachmentsToSave.add(fileStorage);
                currentAttachmentCount++;
            } catch (IOException e) {
                rejectedFiles.add(file.getOriginalFilename());
            }
        }

        // Save attachments
        List<FileStorage> savedAttachments = fileStorageRepository.saveAll(attachmentsToSave);

        // Prepare response
        response.setSuccess(!savedAttachments.isEmpty());
        response.setAttachments(savedAttachments.stream()
                .map(file -> modelMapper.map(file, FileStorageDto.class))
                .toArray(FileStorageDto[]::new));

        if (!rejectedFiles.isEmpty()) {
            response.setMessage("Each task can have at most " + MAX_FILES + " files. The following files are not added: "
                    + String.join(", ", rejectedFiles));
        }

        return response;
    }

    public FileUploadResponse deleteFile(String boardId, int taskId, Long fileId, String userId) {

        // Find the task
        TaskV3 task = taskV3Repository.findByIdAndBoardId(taskId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Task with id " + taskId + " does not exist in board id: " + boardId));

        // Find and delete the file
        FileStorage fileToDelete = fileStorageRepository.findById(fileId)
                .orElseThrow(() -> new ItemNotFoundException("File not found"));


        if (fileToDelete.getTask().getId() != task.getId()) {
            throw new AccessDeniedException("File does not belong to this task");
        }

        fileStorageRepository.delete(fileToDelete);

        // Fetch remaining attachments
        List<FileStorage> remainingAttachments = fileStorageRepository.findByTask(task);

        FileUploadResponse response = new FileUploadResponse();
        response.setSuccess(true);
        response.setAttachments(remainingAttachments.stream()
                .map(file -> modelMapper.map(file, FileStorageDto.class))
                .toArray(FileStorageDto[]::new));

        return response;
    }

    // Method to get files for a task (used in task detail retrieval)
    public FileStorageDto[] getFilesByTask(TaskV3 task) {
        List<FileStorage> attachments = fileStorageRepository.findByTask(task);
        return attachments.stream()
                .map(file -> modelMapper.map(file, FileStorageDto.class))
                .toArray(FileStorageDto[]::new);
    }

    private void validateFiles(List<MultipartFile> files, List<FileStorage> existingAttachments) {
        // Check if adding new files would exceed max files
        if (existingAttachments.size() + files.size() > MAX_FILES) {
            throw new InvalidFileException("Cannot add files. Total files would exceed the maximum limit of " + MAX_FILES);
        }

        // Create a set of existing filenames for efficient lookup
        Set<String> existingFilenames = existingAttachments.stream()
                .map(FileStorage::getName)
                .collect(Collectors.toSet());

        // Validate each file
        for (MultipartFile file : files) {
            // Check file name uniqueness
            if (existingFilenames.contains(file.getOriginalFilename())) {
                throw new ConflictException("File with name " + file.getOriginalFilename() + " already exists.");
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new InvalidFileException("File " + file.getOriginalFilename() + " exceeds maximum file size of " + MAX_FILE_SIZE + " bytes.");
            }
        }
    }

    public FileUploadResponse handleFileAttachments(String boardId, int taskId, FileStorageDto[] existingAttachments,
                                                    List<MultipartFile> newFiles, List<Long> filesToDelete,
                                                    String userId) {
        FileUploadResponse finalResponse = new FileUploadResponse();
        finalResponse.setSuccess(true);

        // Delete specified files first
        if (filesToDelete != null && !filesToDelete.isEmpty()) {
            for (Long fileId : filesToDelete) {
                try {
                    FileUploadResponse deleteResponse = deleteFile(boardId, taskId, fileId, userId);
                    // Merge deleted files' response
                    finalResponse.setAttachments(deleteResponse.getAttachments());
                } catch (Exception e) {
                    finalResponse.setSuccess(false);
                    finalResponse.setMessage("Error deleting some files: " + e.getMessage());
                }
            }
        }

        // Upload new files
        if (newFiles != null && !newFiles.isEmpty()) {
            FileUploadResponse uploadResponse = uploadFiles(boardId, taskId, newFiles, userId);

            // Merge new files with existing/deleted files
            List<FileStorageDto> mergedAttachments = new ArrayList<>();
            if (finalResponse.getAttachments() != null) {
                mergedAttachments.addAll(Arrays.asList(finalResponse.getAttachments()));
            }
            mergedAttachments.addAll(Arrays.asList(uploadResponse.getAttachments()));

            finalResponse.setAttachments(mergedAttachments.toArray(new FileStorageDto[0]));

            // Update success and message if upload had issues
            if (!uploadResponse.isSuccess()) {
                finalResponse.setSuccess(false);
                finalResponse.setMessage(uploadResponse.getMessage());
            }
        }

        return finalResponse;
    }
}
