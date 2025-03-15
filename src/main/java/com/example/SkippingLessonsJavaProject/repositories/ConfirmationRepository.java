package com.example.SkippingLessonsJavaProject.repositories;

import com.example.SkippingLessonsJavaProject.entitys.Confirmation;
import com.example.SkippingLessonsJavaProject.entitys.SkippingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConfirmationRepository  extends JpaRepository<Confirmation, UUID> {
    List<Confirmation> findBySkippingRequest(SkippingRequest skippingRequest);
    boolean existsByFilenameAndSkippingRequest(String filename, SkippingRequest skippingRequest);
}
