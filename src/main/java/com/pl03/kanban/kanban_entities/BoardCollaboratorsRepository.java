package com.pl03.kanban.kanban_entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface BoardCollaboratorsRepository extends JpaRepository<BoardCollaborators, BoardCollaboratorsId> {
}


