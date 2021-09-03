package com.arangodb.springframework.debug.repository;

import com.arangodb.springframework.debug.repository.entity.User;
import com.arangodb.springframework.repository.ArangoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends ArangoRepository<User, String> {
    boolean existsByLoginEmailIgnoreCase(String loginEmail);
}