package com.pl03.kanban.models;

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
    private String taskTitle;

    @Column(name = "taskDescription", length = 500)
    private String taskDescription;

    @Column(name = "taskAssignees", length = 30)
    private String taskAssignees;

    @Column(name = "taskStatus", nullable = false)
    private String taskStatus;

    @Column(name = "createdOn", nullable = false, updatable = false)
    private Timestamp createdOn;

    @Column(name = "updatedOn", nullable = false)
    private Timestamp updatedOn;
}
