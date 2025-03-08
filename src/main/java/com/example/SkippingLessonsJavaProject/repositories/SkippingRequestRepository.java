package com.example.SkippingLessonsJavaProject.repositories;

import com.example.SkippingLessonsJavaProject.entitys.Confirmation;
import com.example.SkippingLessonsJavaProject.entitys.SkippingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SkippingRequestRepository extends JpaRepository<SkippingRequest, UUID> {

}
