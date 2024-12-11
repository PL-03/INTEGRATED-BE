package com.pl03.kanban.kanban_entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

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
    private AccessRight accessRight; // Access level for the collaborator

    @Column(name = "addedOn", nullable = false, updatable = false)
    @CreationTimestamp
    private Timestamp addedOn;

    @Column(name = "name", length = 100, nullable = false)
    private String name; // Name of the collaborator

    @Column(name = "email", length = 50, nullable = false)
    private String email; // Email of the collaborator

    public enum AccessRight {
        READ, WRITE, PENDING
    }
}


