package com.pl03.kanban.kanban_entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface BoardCollaboratorsRepository extends JpaRepository<BoardCollaborators, BoardCollaboratorsId> {

    List<BoardCollaborators> findByBoardId(String boardId);

    Optional<BoardCollaborators> findByBoardIdAndUserOid(String boardId, String userOid);

    boolean existsByBoardIdAndUserOid(String boardId, String userOid);

//    boolean existsByBoardIdAndUserOid(String boardId, Long userId);
}

