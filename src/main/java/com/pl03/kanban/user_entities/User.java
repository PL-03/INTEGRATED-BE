package com.pl03.kanban.user_entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.antlr.v4.runtime.misc.NotNull;
import org.hibernate.annotations.ColumnDefault;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", schema = "user_entities")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "oid", nullable = false, length = 36)
    private String oid;

    @NonNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @NotNull
    @Column(name = "email", nullable = false, length = 50)
    private String email;

    @JsonIgnore
    @NotNull
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @NotNull
    @ColumnDefault("'STUDENT'")
    @Lob
    @Column(name = "role", nullable = false)
    private String role;
}
