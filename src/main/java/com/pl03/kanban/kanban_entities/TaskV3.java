package com.pl03.kanban.kanban_entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "taskv2", schema = "kanban_entities")
@AllArgsConstructor
@NoArgsConstructor
public class TaskV3 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "taskTitle", nullable = false, length = 100)
    private String title;

    @Column(name = "taskDescription", length = 500)
    private String description;

    @Column(name = "taskAssignees", length = 30)
    private String assignees;

    @ManyToOne
    @JoinColumn(name = "taskStatusId", referencedColumnName = "statusId", nullable = false)
    private StatusV3 statusV3;

    @Column(nullable = false, updatable = false, insertable = false)
    private Timestamp createdOn;

    @Column(nullable = false, updatable = false, insertable = false)
    private Timestamp updatedOn;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "boardId", nullable = false)
    private Board board;
}
