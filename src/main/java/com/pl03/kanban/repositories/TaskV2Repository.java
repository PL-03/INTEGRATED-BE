package com.pl03.kanban.repositories;

import com.pl03.kanban.entities.Status;
import com.pl03.kanban.entities.TaskV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskV2Repository extends JpaRepository<TaskV2, Integer> {
    List<TaskV2> findByStatus(Status status);
}