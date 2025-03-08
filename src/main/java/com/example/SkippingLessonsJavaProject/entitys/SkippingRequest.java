package com.example.SkippingLessonsJavaProject.entitys;

import com.example.SkippingLessonsJavaProject.models.SkippingRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "skipping_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkippingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private String reason;

    private List<Integer> lessons;

    @Enumerated(EnumType.STRING)
    private SkippingRequestStatus status = SkippingRequestStatus.PENDING;

    @OneToMany(mappedBy = "skippingRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Confirmation> confirmations = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "approver_id")
    private User approver;

    private String rejectionReason;

}
