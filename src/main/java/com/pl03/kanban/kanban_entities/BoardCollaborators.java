package com.pl03.kanban.kanban_entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table(name = "board_collaborators", schema = "kanban_entities")
public class BoardCollaborators {

    @EmbeddedId
    private BoardCollaboratorsId id; // Use the composite key

    @ManyToOne
    @MapsId("boardId") // Maps the boardId field in the composite key
    @JoinColumn(name = "boardId", nullable = false)
    private Board board; // Reference to Board

    @ManyToOne
    @MapsId("userId") // Maps the userId field in the composite key
    @JoinColumn(name = "userId", nullable = false)
    private Users user; // Reference to Users

    @Enumerated(EnumType.STRING)
    @Column(name = "accessLevel", nullable = false)
    private AccessLevel accessLevel; // Access level for the collaborator

    public enum AccessLevel {
        READ, WRITE
    }
}


