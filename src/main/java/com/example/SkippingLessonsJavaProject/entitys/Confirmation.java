package com.example.SkippingLessonsJavaProject.entitys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;
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


    @Lob
    @Column(nullable = false)
    private String filePath;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "skipping_request_id", nullable = false)
    private SkippingRequest skippingRequest;
}
