package com.pl03.kanban.controllers;

import com.pl03.kanban.dtos.CollaboratorRequest;
import com.pl03.kanban.dtos.CollaboratorResponse;
import com.pl03.kanban.services.CollaboratorService;
import com.pl03.kanban.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@CrossOrigin(origins = {"http://localhost:5173",
        "http://intproj23.sit.kmutt.ac.th",
        "http://intproj23.sit.kmutt.ac.th/pl3",
        "http://intproj23.sit.kmutt.ac.th/pl3/status",
        "http://ip23pl3.sit.kmutt.ac.th",
        "https://intproj23.sit.kmutt.ac.th",
        "https://intproj23.sit.kmutt.ac.th/pl3",
        "https://intproj23.sit.kmutt.ac.th/pl3/status",
        "https://ip23pl3.sit.kmutt.ac.th"})
@RestController
@RequestMapping("/v3/boards")
public class CollaboratorController {
    private final JwtTokenUtils jwtTokenUtils;
    private final CollaboratorService collaboratorService;
    @Autowired
    public CollaboratorController(JwtTokenUtils jwtTokenUtils, CollaboratorService collaboratorService) {
        this.jwtTokenUtils = jwtTokenUtils;
        this.collaboratorService = collaboratorService;
    }

    @GetMapping("/{id}/collabs")
    public ResponseEntity<List<CollaboratorResponse>> getBoardCollaborators(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String requesterOid = getRequesterOid(authHeader);
        List<CollaboratorResponse> collaborators = collaboratorService.getBoardCollaborators(id, requesterOid);
        return ResponseEntity.ok(collaborators);
    }

    @GetMapping("/{id}/collabs/{collabOid}")
    public ResponseEntity<CollaboratorResponse> getBoardCollaboratorByOid(
            @PathVariable String id,
            @PathVariable String collabOid,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String requesterOid = getRequesterOid(authHeader);
        CollaboratorResponse collaborator = collaboratorService.getBoardCollaboratorByOid(id, collabOid, requesterOid);
        return ResponseEntity.ok(collaborator);
    }

    @PostMapping("/{id}/collabs")
    public ResponseEntity<CollaboratorResponse> addBoardCollaborator(
            @PathVariable String id,
            @RequestBody(required = false) CollaboratorRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String ownerOid = getUserIdFromToken(authHeader.substring(7));
        CollaboratorResponse response = collaboratorService.addBoardCollaborator(id, request, ownerOid);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/collabs/{userOid}/accept")
    public ResponseEntity<CollaboratorResponse> acceptInvitation(
            @PathVariable String id,
            @PathVariable String userOid) {
        CollaboratorResponse response = collaboratorService.acceptInvitation(id, userOid);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}/collabs/{userOid}/decline")
    public ResponseEntity<Void> declineInvitation(
            @PathVariable String id,
            @PathVariable String userOid) {
        collaboratorService.declineInvitation(id, userOid);
        return ResponseEntity.ok().build();
    }


    @PatchMapping("/{id}/collabs/{collabOid}")
    public ResponseEntity<CollaboratorResponse> updateCollaboratorAccessRight(
            @PathVariable String id,
            @PathVariable String collabOid,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {
        String requesterOid = getUserIdFromToken(authHeader.substring(7));
        String accessRight = request.get("accessRight");
        CollaboratorResponse response = collaboratorService.updateCollaboratorAccessRight(id, collabOid, accessRight, requesterOid);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/collabs/{collabOid}")
    public ResponseEntity<Void> removeCollaborator(
            @PathVariable String id,
            @PathVariable String collabOid,
            @RequestHeader("Authorization") String authHeader) {
        String requesterOid = getUserIdFromToken(authHeader.substring(7));
        collaboratorService.removeCollaborator(id, collabOid, requesterOid);
        return ResponseEntity.ok().build();
    }

    private String getRequesterOid(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenUtils.validateToken(token)) {
                return getUserIdFromToken(token);
            }
        }
        return null;
    }

    private String getUserIdFromToken(String token) {
        Map<String, Object> claims = jwtTokenUtils.getClaimsFromToken(token);
        return (String) claims.get("oid");
    }
}
