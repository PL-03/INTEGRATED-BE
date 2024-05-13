package com.pl03.kanban.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "status")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statusId")
    private int id;

    @Column(name = "statusName", nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "statusDescription", length = 200)
    private String description;

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }
}