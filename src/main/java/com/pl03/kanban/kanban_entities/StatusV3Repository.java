package com.pl03.kanban.kanban_entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusV3Repository extends JpaRepository<StatusV3, Integer> {
    List<StatusV3> findByNameInAndBoardId(List<String> names, String id);
    List<StatusV3> findByBoardId(String id);
    Optional<StatusV3> findByIdAndBoardId(int id, String boardId);

    boolean existsByNameIgnoreCaseAndBoardId(String name, String boardId);
}
