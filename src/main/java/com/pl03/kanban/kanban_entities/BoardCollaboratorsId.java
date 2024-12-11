package com.pl03.kanban.kanban_entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class BoardCollaboratorsId implements Serializable {
    private String boardId; // The ID of the board
    private String userId; // The ID of the user
}

