package com.pl03.kanban.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class BoardResponse {
    private String id; // Changed from 'boardId' to 'id'
    private String name;
    private OwnerResponse owner;
    private Visibility visibility;

    public enum Visibility {
        PRIVATE, PUBLIC
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OwnerResponse {
        private String oid;
        private String name;
    }
}
