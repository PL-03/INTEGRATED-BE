package com.pl03.kanban.kanban_entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "status", schema = "kanban_entities")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statusId")
    private int id;

    @Column(name = "statusName", nullable = false, length = 50)
    private String name;

    @Column(name = "statusDescription", length = 200)
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boardId", nullable = false)
    private Board board;

    public void setName(String name) {
        this.name = name == null || name.trim().isEmpty() ? null : name.trim();
    }

    public void setDescription(String description) {
        this.description = description == null || description.trim().isEmpty() ? null : description.trim();
    }
}