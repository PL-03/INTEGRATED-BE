package com.pl03.kanban.kanban_entities;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskV2Repository extends JpaRepository<TaskV2, Integer> {
    List<TaskV2> findByStatus(Status status);
    List<TaskV2> findByStatusIn(List<Status> statuses, Sort sort);


    Optional<TaskV2> findByIdAndBoardBoardId(int taskId, String boardId); //fetch single task
    List<TaskV2> findByBoardBoardId(String boardId); //fetch all tasks in a board

    List<TaskV2> findByBoardBoardId(String boardId, Sort sort); //fetch all tasks in a board (sorted)

    List<TaskV2> findByStatusInAndBoardBoardId(List<Status> statuses, String boardId, Sort sort); //fetch all tasks in a board (sorted, status filtered)
    List<TaskV2> findByStatusInAndBoardBoardId(List<Status> statuses, String boardId); //fetch all tasks in a board (status filtered no sort)
}