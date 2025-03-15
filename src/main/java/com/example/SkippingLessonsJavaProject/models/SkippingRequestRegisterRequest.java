package com.example.SkippingLessonsJavaProject.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SkippingRequestRegisterRequest {

    @Schema(description = "Дата начала действия пропуска", example = "2024-10-27")
    @NotNull(message = "Дата начала не должна быть пустой")
    private LocalDate startDate;

    @Schema(description = "Дата конца действия пропуска", example = "2024-10-28")
    @NotNull(message = "Дата конца не должна быть пустой")
    private LocalDate endDate;

    @Schema(description = "Причина пропуска", example = "Потому что")
    private String reason;

    @Schema(description = "Пары, которые были пропущены (если был пропущен временной промежуток, то оставьте это поле пустым)", example = "[1, 2, 3]")
    private List<Integer> lessons;
}