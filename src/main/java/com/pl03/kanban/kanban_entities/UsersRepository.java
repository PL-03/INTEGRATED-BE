package com.pl03.kanban.kanban_entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, String> {
    Optional<Users> findByOid(String oid);
    Users findByUsername(String username);
}


