package com.pl03.kanban.dtos;

import com.pl03.kanban.kanban_entities.BoardCollaborators;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponse {
    private String id;
    private String name;
    private OwnerResponse owner;
    private Visibility visibility;
    private List<CollaboratorResponse> collaborators;

    public enum Visibility {
        PRIVATE, PUBLIC
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OwnerResponse {
        private String oid;
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CollaboratorResponse {
        private String oid;
        private String name;
        private String email;
        private BoardCollaborators.AccessRight accessRight;
        private BoardCollaborators.AccessRight assignedAccessRight; // Assigned right during invitation
        private Timestamp addedOn; // Date the collaborator was added
    }
}
