package com.pl03.kanban.kanban_entities.Repositories;

import com.pl03.kanban.kanban_entities.StatusV3;
import com.pl03.kanban.kanban_entities.TaskV3;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskV3Repository extends JpaRepository<TaskV3, Integer> {
    List<TaskV3> findByStatusV3(StatusV3 statusV3);
//    List<TaskV3> findByStatusV3In(List<StatusV3> statusV3s, Sort sort);


    Optional<TaskV3> findByIdAndBoardId(int taskId, String id); //fetch single task
    List<TaskV3> findByBoardId(String id); //fetch all tasks in a board

    List<TaskV3> findByBoardId(String boardId, Sort sort); //fetch all tasks in a board (sorted)

    List<TaskV3> findByStatusV3InAndBoardId(List<StatusV3> statusV3s, String boardId, Sort sort); //fetch all tasks in a board (sorted, status filtered)
    List<TaskV3> findByStatusV3InAndBoardId(List<StatusV3> statusV3s, String boardId); //fetch all tasks in a board (status filtered no sort)
}