package com.pl03.kanban.kanban_entities.repositories;

import com.pl03.kanban.kanban_entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, String> {
    Optional<Users> findByOid(String oid);
    Optional<Users> findByEmail(String email);
//    Users findByUsername(String username);
}


