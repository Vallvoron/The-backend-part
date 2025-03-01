package com.example.SkippingLessonsJavaProject;

import com.example.SkippingLessonsJavaProject.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByLogin(String login);
    List<User> findAll();
}
