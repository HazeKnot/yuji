package com.yuji.wellness.care.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class CareDtos {

    private CareDtos() {
    }

    public record PlanReq(
            @NotNull LocalDate deliveryDate,
            @NotBlank String deliveryMode
    ) {
    }

    public record TaskVO(
            Long id,
            Integer dayIndex,
            LocalDate dueDate,
            String category,
            String categoryLabel,
            String title,
            String detail,
            String status
    ) {
    }

    public record WeekVO(
            Integer weekNo,
            String label,
            List<TaskVO> tasks
    ) {
    }

    public record PlanVO(
            Long id,
            LocalDate deliveryDate,
            String deliveryMode,
            String deliveryModeLabel,
            Integer dayIndex,
            String stageLabel,
            Integer progress,
            List<TaskVO> today,
            List<TaskVO> upcoming,
            List<WeekVO> weeks,
            String disclaimer
    ) {
    }

    public record StatusReq(
            @NotBlank String status
    ) {
    }

    public record NoteReq(
            @Size(max = 300) String note
    ) {
    }
}
