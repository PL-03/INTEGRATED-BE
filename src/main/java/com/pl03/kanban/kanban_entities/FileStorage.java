package com.pl03.kanban.kanban_entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "file_storage", schema = "kanban_entities")
@AllArgsConstructor
@NoArgsConstructor
public class FileStorage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(name = "added_on", nullable = false, updatable = false, insertable = false)
    private Timestamp addedOn;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private TaskV3 task;
}
