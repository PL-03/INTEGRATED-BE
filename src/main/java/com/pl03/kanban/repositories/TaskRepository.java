package com.pl03.kanban.repositories;

import com.pl03.kanban.dtos.AllTaskDto;
import com.pl03.kanban.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    @Query("SELECT new com.pl03.kanban.dtos.AllTaskDto(t.id, t.taskTitle, t.taskAssignees, t.taskStatus) FROM Task t")
    List<AllTaskDto> findAllTasks();
}
