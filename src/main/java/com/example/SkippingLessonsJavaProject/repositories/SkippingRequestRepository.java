package com.example.SkippingLessonsJavaProject.repositories;

import com.example.SkippingLessonsJavaProject.entitys.Confirmation;
import com.example.SkippingLessonsJavaProject.entitys.SkippingRequest;
import com.example.SkippingLessonsJavaProject.entitys.User;
import com.example.SkippingLessonsJavaProject.models.SkippingRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkippingRequestRepository extends JpaRepository<SkippingRequest, UUID> {
    List<SkippingRequest> findAll();
    List<SkippingRequest> findByStatus(SkippingRequestStatus status);
    List<SkippingRequest> findByStudent(User student);
}
