package com.pl03.kanban.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class CollaboratorResponse {
    private String oid;
    private String name;
    private String email;

//    @JsonProperty("access_right")
    private String accessRight;

//    @JsonProperty("added_on")
    private Timestamp addedOn;
}

