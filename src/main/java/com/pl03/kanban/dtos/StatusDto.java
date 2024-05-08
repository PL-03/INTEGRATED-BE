package com.pl03.kanban.dtos;

import lombok.Data;

@Data
public class StatusDto {
    private int id;
    private String name;
    private String description;

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }
}