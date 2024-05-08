package com.pl03.kanban.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "taskv2")
@AllArgsConstructor
@NoArgsConstructor
public class TaskV2 {

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
    private Status status;

    @Column(nullable = false, updatable = false, insertable = false)
    private Timestamp createdOn;

    @Column(nullable = false, updatable = false, insertable = false)
    private Timestamp updatedOn;

    public TaskV2(String title, String description, String assignees) {
        this.title = title;
        this.description = description;
        this.assignees = assignees;
        this.status = new Status(1, "No Status", null); // Set default status
    }
}