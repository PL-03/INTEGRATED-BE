package com.pl03.kanban.kanban_entities;

import io.viascom.nanoid.NanoId;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private List<TaskV2> tasks;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Status> statuses;

    @PrePersist
    public void prePersist() {
        if (boardId == null) {
            boardId = NanoId.generate(10);
        }
    }
}