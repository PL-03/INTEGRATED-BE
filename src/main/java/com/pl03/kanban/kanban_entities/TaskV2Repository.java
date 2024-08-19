package com.pl03.kanban.kanban_entities;

import com.pl03.kanban.kanban_entities.Status;
import com.pl03.kanban.kanban_entities.TaskV2;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskV2Repository extends JpaRepository<TaskV2, Integer> {
    List<TaskV2> findByStatus(Status status);
    List<TaskV2> findByStatusIn(List<Status> statuses, Sort sort);
    List<TaskV2> findByStatusIn(List<Status> statuses);
}