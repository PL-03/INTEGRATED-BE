package com.pl03.kanban.kanban_entities.repositories;

import com.pl03.kanban.kanban_entities.BoardCollaborators;
import com.pl03.kanban.kanban_entities.BoardCollaboratorsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface BoardCollaboratorsRepository extends JpaRepository<BoardCollaborators, BoardCollaboratorsId> {

    List<BoardCollaborators> findByBoardId(String boardId);

    Optional<BoardCollaborators> findByBoardIdAndUserOid(String boardId, String userOid);

    boolean existsByBoardIdAndUserOid(String boardId, String userOid);
    boolean existsByBoardIdAndUserOidAndAccessRightNot(String boardId, String userOid, BoardCollaborators.AccessRight accessRight);


}


