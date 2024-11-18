package com.pl03.kanban.dtos;

import lombok.Data;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class FileStorageDto implements Serializable {
    private Long id;
    private String name;
    private String type;
    private long size;
    private Timestamp addedOn;
}

