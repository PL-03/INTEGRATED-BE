package com.pl03.kanban.kanban_entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.sql.Timestamp;

@Entity
@Table(name = "statusv3", schema = "kanban_entities")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class StatusV3 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statusId")
    private int id;

    @Column(name = "statusName", nullable = false, length = 50)
    private String name;

    @Column(name = "statusDescription", length = 200)
    private String description;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "boardId", nullable = false)
    private Board board;

    @Column(nullable = false, updatable = false, insertable = false)
    private Timestamp createdOn;

    @Column(nullable = false, updatable = false, insertable = false)
    private Timestamp updatedOn;

    public void setName(String name) {
        this.name = name == null || name.trim().isEmpty() ? null : name.trim();
    }

    public void setDescription(String description) {
        this.description = description == null || description.trim().isEmpty() ? null : description.trim();
    }

    // Constructor without timestamps (id, name, description, board)
    public StatusV3(int id, String name, String description, Board board) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.board = board;
    }
}