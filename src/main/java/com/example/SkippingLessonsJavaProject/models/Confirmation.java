package com.example.SkippingLessonsJavaProject.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "confirmations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Confirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String fileType;

    @Lob
    @Column(nullable = false)
    private byte[] data;

    @ManyToOne
    @JoinColumn(name = "skipping_request_id", nullable = false)
    private SkippingRequest skippingRequest;
}


