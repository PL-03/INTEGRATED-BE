package com.pl03.kanban.user_entities;

import com.pl03.kanban.kanban_entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUsername(String username);
    User findByOid(String oid);
    Optional<User> findByEmail(String email);
}
