package com.example.SkippingLessonsJavaProject.repositories;

import com.example.SkippingLessonsJavaProject.models.UsersForRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UsersForRegisterRepository extends JpaRepository<UsersForRegister, UUID> {
    Optional<UsersForRegister> findByLogin(String login);
    void deleteByLogin(String login);
}
