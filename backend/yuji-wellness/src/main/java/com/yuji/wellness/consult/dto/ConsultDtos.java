package com.yuji.wellness.consult.dto;

import com.yuji.wellness.partner.dto.PartnerDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class ConsultDtos {

    private ConsultDtos() {
    }

    public record TopicVO(
            Long id,
            String code,
            String title,
            String category,
            String categoryLabel,
            String summary,
            String department
    ) {
    }

    public record TopicDetailVO(
            Long id,
            String code,
            String title,
            String category,
            String categoryLabel,
            String summary,
            String advice,
            String redFlags,
            String careTips,
            String department,
            String source
    ) {
    }

    public record SessionReq(
            @NotBlank String topicCode,
            @Size(max = 500) String question
    ) {
    }

    public record SessionVO(
            Long id,
            String title,
            String topicCode,
            String riskLevel,
            String riskLabel,
            String status,
            LocalDateTime createdAt
    ) {
    }

    public record MessageReq(
            @NotBlank @Size(max = 1000) String content
    ) {
    }

    public record MessageVO(
            Long id,
            String role,
            String content,
            LocalDateTime createdAt
    ) {
    }

    public record SessionDetailVO(
            SessionVO session,
            List<MessageVO> messages,
            TopicDetailVO topic,
            String disclaimer
    ) {
    }

    public record ReferralVO(
            Long ticketId,
            String level,
            String levelLabel,
            String message,
            List<PartnerDtos.OrgBriefVO> partners
    ) {
    }
}
