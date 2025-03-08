package com.example.SkippingLessonsJavaProject.entitys;

import com.example.SkippingLessonsJavaProject.models.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Логин не должен быть пустым")
    @Email(message = "email адрес должен быть в формате user@example.com")
    private String login;//почта

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.ПОЛЬЗОВАТЕЛЬ;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String name;

}
