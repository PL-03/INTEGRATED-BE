package com.pl03.kanban.kanban_entities.repositories;

import com.pl03.kanban.kanban_entities.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, String> {
    boolean existsById(String id);

}
