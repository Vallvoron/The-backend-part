package com.example.SkippingLessonsJavaProject.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserSingInRequest {

    @Schema(description = "Логин пользователя", example = "ivanovivan@example.com")
    @Email(message = "email адрес должен быть в формате user@example.com")
    @NotBlank(message = "Логин не должен быть пустым")
    private String login;

    @Schema(description = "Пароль пользователя", example = "IvanovIvan")
    @NotBlank(message = "Пароль не должен быть пустым")
    private String password;
}
