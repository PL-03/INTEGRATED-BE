package com.pl03.kanban.kanban_entities.repositories;

import com.pl03.kanban.kanban_entities.FileStorage;
import com.pl03.kanban.kanban_entities.TaskV3;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileStorageRepository extends JpaRepository<FileStorage,Long> {
    Optional<FileStorage> findByName(String fileName);
    List<FileStorage> findByTaskId(int taskId);
    Optional<FileStorage> findByNameAndTask_Id(String name, int taskId);


}
