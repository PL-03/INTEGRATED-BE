package com.pl03.kanban.repositories;

import com.pl03.kanban.dtos.GetAllTaskDto;
import com.pl03.kanban.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    @Query("SELECT new com.pl03.kanban.dtos.GetAllTaskDto(t.id, t.title, t.assignees, t.status) FROM Task t")
    List<GetAllTaskDto> findAllTasks();

    Task findTaskById(int id);
}
