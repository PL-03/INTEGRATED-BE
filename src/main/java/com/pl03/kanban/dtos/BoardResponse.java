package com.pl03.kanban.dtos;

import lombok.Builder;
import lombok.Data;

@Data
public class BoardResponse {
    private String id;
    private String name;
    private OwnerResponse owner;

    @Data
    public static class OwnerResponse {
        private String oid;
        private String name;
    }
}
