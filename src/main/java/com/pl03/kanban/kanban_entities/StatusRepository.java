package com.pl03.kanban.kanban_entities;

import com.pl03.kanban.kanban_entities.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusRepository extends JpaRepository<Status, Integer> {
    List<Status> findByNameInAndBoardBoardId(List<String> names, String boardId);
    List<Status> findByNameIgnoreCaseAndIdNot(String name, int id);
    List<Status> findByBoardBoardId(String boardId);
    Optional<Status> findByIdAndBoardBoardId(int id, String boardId);

    boolean existsByNameIgnoreCaseAndBoardBoardId(String name, String board_boardId);
}