package com.pl03.kanban.kanban_entities;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, String> {
//    Board findByOid(String oid);

}
