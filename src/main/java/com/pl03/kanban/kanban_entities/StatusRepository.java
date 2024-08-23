package com.pl03.kanban.kanban_entities;

import com.pl03.kanban.kanban_entities.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusRepository extends JpaRepository<Status, Integer> {
    List<Status> findByNameIn(List<String> names);

    List<Status> findByNameIgnoreCaseAndIdNot(String name, int id);
}