package com.pl03.kanban.kanban_entities.Repositories;

import com.pl03.kanban.kanban_entities.FileStorage;
import com.pl03.kanban.kanban_entities.TaskV3;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileStorageRepository extends JpaRepository<FileStorage,Long> {
    Optional<FileStorage> findByName(String fileName);
    List<FileStorage> findByTask(TaskV3 task);
}
