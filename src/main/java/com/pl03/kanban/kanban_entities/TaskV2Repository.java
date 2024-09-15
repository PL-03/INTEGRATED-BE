package com.pl03.kanban.kanban_entities;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskV2Repository extends JpaRepository<TaskV3, Integer> {
    List<TaskV3> findByStatus(StatusV3 statusV3);
    List<TaskV3> findByStatusIn(List<StatusV3> statusV3s, Sort sort);


    Optional<TaskV3> findByIdAndBoardBoardId(int taskId, String boardId); //fetch single task
    List<TaskV3> findByBoardBoardId(String boardId); //fetch all tasks in a board

    List<TaskV3> findByBoardBoardId(String boardId, Sort sort); //fetch all tasks in a board (sorted)

    List<TaskV3> findByStatusInAndBoardBoardId(List<StatusV3> statusV3s, String boardId, Sort sort); //fetch all tasks in a board (sorted, status filtered)
    List<TaskV3> findByStatusInAndBoardBoardId(List<StatusV3> statusV3s, String boardId); //fetch all tasks in a board (status filtered no sort)
}