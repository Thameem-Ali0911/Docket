package com.docket.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.docket.entity.User;

/**
 * Data access for the users table.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
