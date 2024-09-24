package com.pl03.kanban.kanban_entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusV3Repository extends JpaRepository<StatusV3, Integer> {
    List<StatusV3> findByNameInAndBoardBoardId(List<String> names, String boardId);
//    List<StatusV3> findByNameIgnoreCaseAndIdNot(String name, int id);
    List<StatusV3> findByBoardBoardId(String boardId);
    Optional<StatusV3> findByIdAndBoardBoardId(int id, String boardId);

    boolean existsByNameIgnoreCaseAndBoardBoardId(String name, String board_boardId);
}