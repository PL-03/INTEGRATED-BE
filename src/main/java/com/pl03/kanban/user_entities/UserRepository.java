package com.pl03.kanban.user_entities;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUsername(String username);
    User findByOid(String oid);
}
