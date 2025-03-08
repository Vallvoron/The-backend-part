package com.example.SkippingLessonsJavaProject.repositories;

import com.example.SkippingLessonsJavaProject.entitys.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByLogin(String login);
    List<User> findAll();
}
