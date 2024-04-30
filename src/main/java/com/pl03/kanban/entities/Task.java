package com.pl03.kanban.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "taskTitle", nullable = false, length = 100)
    private String title;

    @Column(name = "taskDescription", length = 500)
    private String description;

    @Column(name = "taskAssignees", length = 30)
    private String assignees;

    @Column(name = "taskStatus", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "createdOn", nullable = false, updatable = false, insertable = false)
    private Timestamp createdOn;

    @Column(name = "updatedOn", nullable = false, insertable = false)
    private Timestamp updatedOn;

    public enum TaskStatus {
        NO_STATUS, TO_DO, DOING, DONE
    }
}
