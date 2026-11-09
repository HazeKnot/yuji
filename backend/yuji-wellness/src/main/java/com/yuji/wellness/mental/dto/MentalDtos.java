package com.yuji.wellness.mental.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class MentalDtos {

    private MentalDtos() {
    }

    public record CheckinReq(
            @NotNull @Min(1) @Max(5) Integer mood,
            @Size(max = 6) List<String> tags,
            @Size(max = 300) String note
    ) {
    }

    public record CheckinVO(
            Long id,
            Integer mood,
            String moodLabel,
            List<String> tags,
            String note,
            String timeText,
            LocalDateTime createdAt
    ) {
    }

    public record EpdsReq(
            @NotNull @Size(min = 10, max = 10) List<@Min(0) @Max(3) Integer> answers
    ) {
    }

    public record EpdsResultVO(
            Integer score,
            String level,
            String levelLabel,
            String interpretation,
            List<String> suggestions,
            String safetyNote,
            Long ticketId,
            String disclaimer
    ) {
    }

    public record ScaleVO(
            Long id,
            String scaleType,
            Integer score,
            String level,
            String levelLabel,
            LocalDateTime createdAt
    ) {
    }

    public record TicketVO(
            Long id,
            String source,
            String level,
            String levelLabel,
            String summary,
            String status,
            String statusLabel,
            String handlerNote,
            String timeText,
            LocalDateTime createdAt
    ) {
    }

    public record OverviewVO(
            List<CheckinVO> recentCheckins,
            List<ScaleVO> scales,
            List<TicketVO> tickets,
            String tip,
            String disclaimer
    ) {
    }
}
