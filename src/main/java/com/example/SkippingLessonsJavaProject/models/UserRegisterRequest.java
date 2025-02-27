package com.example.SkippingLessonsJavaProject.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequest {

    @Schema(description = "Логин пользователя", example = "ivanovivan@example.com")
    @Size(min = 5, message = "Логин должен содержать больше 5 символов")
    @Email(message = "email адрес должен быть в формате user@example.com")
    @NotBlank(message = "Логин не должен быть пустым")
    private String login;

    @Schema(description = "Пароль пользователя", example = "IvanovIvan")
    @Size(min = 5, message = "Пароль должен содержать больше 5 символов")
    @NotBlank(message = "Пароль не должен быть пустым")
    private String password;

    @Schema(description = "Имя пользователя", example = "Иванов Иван Иванович")
    @NotBlank(message = "Имя не должно быть пустым")
    private String name;

    @Schema(description = "Номер телефона пользователя", example = "+79519091589")
    @Size(min = 7, message = "Номер должен содержать больше 7 символов")
    @NotBlank(message = "Номер не должен быть пустым")
    private String phone;

    @Schema(description = "Роль пользователя", example = "СТУДЕНТ")
    private UserRole role;
}
