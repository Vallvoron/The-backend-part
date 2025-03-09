package com.example.SkippingLessonsJavaProject.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ConfirmationRegisterRequest {

    @Schema(description = "ID пропуска для прикрепления файлов", example = "2024-10-27")
    @NotNull(message = "Дата начала не должна быть пустой")
    private UUID requestId;
}