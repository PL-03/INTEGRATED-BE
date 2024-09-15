package com.pl03.kanban.kanban_entities;

import io.viascom.nanoid.NanoId;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table(name = "board", schema = "kanban_entities")
public class Board {
    @Id
    @Column(name = "boardId", nullable = false, length = 10)
    private String boardId;

    @Size(max = 120)
    @NotNull
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Size(max = 36)
    @NotNull
    @Column(name = "oid", nullable = false, length = 36)
    private String oid;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskV3> tasks;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatusV3> statusV3s;

    @Column(nullable = false, updatable = false, insertable = false)
    private Timestamp createdOn;

    @Column(nullable = false, updatable = false, insertable = false)
    private Timestamp updatedOn;

    @PrePersist
    public void prePersist() {
        generateUniqueId();
    }

    public void generateUniqueId() {
        this.boardId = NanoId.generate(10);
    }
}