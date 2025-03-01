package com.example.SkippingLessonsJavaProject.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UserChangeRoleRequest {

    @Schema(description = "Логин пользователя на изменение роли", example = "ivanovivan@example.com")
    @Size(min = 5, message = "Логин должен содержать больше 5 символов")
    @Email(message = "email адрес должен быть в формате user@example.com")
    @NotBlank(message = "Логин не должен быть пустым")
    private String login;

    @Schema(description = "Роль пользователя", example = "СТУДЕНТ")
    private UserRole newRole;
}
