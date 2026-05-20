package com.example.trekkingapp.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmail(String email);

    boolean existsByGoogleId(String googleId);

    boolean existsByEmail(String email);
}
