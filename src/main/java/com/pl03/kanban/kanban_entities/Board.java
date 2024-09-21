package com.pl03.kanban.kanban_entities;

import io.viascom.nanoid.NanoId;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    @ManyToOne
    @JoinColumn(name = "oid", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user; // Link to Users entity through oid (foreign key)

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskV3> tasks;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatusV3> statusV3s;

    @Column(name = "createdOn", nullable = false, updatable = false, insertable = false)
    private Timestamp createdOn;

    @Column(name = "updatedOn", nullable = false, insertable = false)
    private Timestamp updatedOn;

    @PrePersist
    public void prePersist() {
        generateUniqueId();
    }

    public void generateUniqueId() {
        this.boardId = NanoId.generate(10);
    }
}