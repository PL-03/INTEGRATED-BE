//package com.pl03.kanban.controllers;
//
//import com.pl03.kanban.exceptions.ItemNotFoundException;
//import com.pl03.kanban.kanban_entities.FileStorage;
//import com.pl03.kanban.kanban_entities.Repositories.FileStorageRepository;
//import com.pl03.kanban.services.FileStorageService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/v3/boards/{boardId}/tasks/{taskId}/files")
//@Slf4j
//public class FileStorageController {
//
//    @Autowired
//    private FileStorageService fileStorageService;
//    @Autowired
//    private FileStorageRepository fileStorageRepository;
//
//    @PostMapping
//    public ResponseEntity<Map<String, Object>> uploadFiles(
//            @PathVariable String boardId,
//            @PathVariable int taskId,
//            @RequestParam("files") List<MultipartFile> files,
//            @RequestHeader("Authorization") String authHeader) {
//
//        Map<String, Object> response = fileStorageService.uploadFiles(boardId, taskId, files);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/{fileName}")
//    public ResponseEntity<byte[]> downloadFile(
//            @PathVariable String boardId,
//            @PathVariable int taskId,
//            @PathVariable String fileName,
//            @RequestHeader(value = "Authorization", required = false) String authHeader) {
//
//        byte[] fileData = fileStorageService.downloadFile(boardId, taskId, fileName);
//
//        // Get the file metadata to set the correct content type
//        FileStorage fileMetadata = fileStorageRepository.findByNameAndTaskId(fileName, taskId)
//                .orElseThrow(() -> new ItemNotFoundException("File not found: " + fileName));
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.parseMediaType(fileMetadata.getType()))
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
//                .body(fileData);
//    }
//
//    @DeleteMapping("/{fileName}")
//    public ResponseEntity<Void> deleteFile(
//            @PathVariable String boardId,
//            @PathVariable int taskId,
//            @PathVariable String fileName,
//            @RequestHeader("Authorization") String authHeader) {
//
//        fileStorageService.deleteFile(boardId, taskId, fileName);
//        return ResponseEntity.noContent().build();
//    }
//}
