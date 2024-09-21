package com.pl03.kanban.kanban_entities;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    @Id
    @Column(name = "oid", nullable = false, length = 36)
    private String oid;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, length = 50)
    private String email;

    @Column(name = "created_on", nullable = false, updatable = false, insertable = false)
    private Timestamp createdOn;

    @Column(name = "updated_on", nullable = false, insertable = false)
    private Timestamp updatedOn;

}

