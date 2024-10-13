package com.pl03.kanban.dtos;

import lombok.Data;

@Data
public class CollaboratorRequest {
    private String email;
    private String access_right;
}
