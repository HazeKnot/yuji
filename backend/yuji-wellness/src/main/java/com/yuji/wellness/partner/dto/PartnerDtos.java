package com.yuji.wellness.partner.dto;

import com.yuji.wellness.mental.dto.MentalDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PartnerDtos {

    private PartnerDtos() {
    }

    public record ServiceVO(
            Long id,
            String name,
            String priceText,
            String description
    ) {
    }

    public record OrgVO(
            Long id,
            String name,
            String type,
            String typeLabel,
            String city,
            String district,
            String address,
            String intro,
            String status,
            Integer serviceCount,
            Long leadCount
    ) {
    }

    public record OrgBriefVO(
            Long id,
            String name,
            String type,
            String typeLabel,
            String city,
            String district,
            String address
    ) {
    }

    public record OrgDetailVO(
            OrgVO org,
            List<ServiceVO> services
    ) {
    }

    public record LeadReq(
            @NotBlank @Size(max = 40) String name,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "请填写 11 位手机号") String phone,
            @NotBlank @Size(max = 200) String need,
            LocalDate preferredDate
    ) {
    }

    public record LeadVO(
            Long id,
            Long orgId,
            String orgName,
            String name,
            String phone,
            String need,
            LocalDate preferredDate,
            String status,
            String statusLabel,
            String note,
            String timeText,
            LocalDateTime createdAt
    ) {
    }

    public record ServiceReq(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 60) String priceText,
            @Size(max = 300) String description
    ) {
    }

    public record OrgReq(
            @NotBlank @Size(max = 100) String name,
            @NotBlank String type,
            @NotBlank @Size(max = 40) String city,
            @Size(max = 40) String district,
            @Size(max = 200) String address,
            @Size(max = 40) String contactName,
            @Size(max = 30) String contactPhone,
            @Size(max = 500) String intro,
            String status,
            List<ServiceReq> services
    ) {
    }

    public record LeadStatusReq(
            @NotBlank String status,
            @Size(max = 300) String note
    ) {
    }

    public record TicketStatusReq(
            @NotBlank String status,
            @Size(max = 500) String handlerNote
    ) {
    }

    public record PostStatusReq(
            @NotBlank String status
    ) {
    }

    public record MemberVO(
            Long userId,
            String nickname,
            String phone,
            long leadCount,
            String lastLeadStatus,
            String lastLeadStatusLabel,
            String lastTicketLevel,
            String lastTicketLevelLabel,
            String lastTicketSummary,
            String joinedText,
            LocalDateTime joinedAt
    ) {
    }

    public record MemberDetailVO(
            MemberVO member,
            List<LeadVO> leads,
            List<MentalDtos.TicketVO> tickets
    ) {
    }

    public record AdminOverviewVO(
            long userCount,
            long cycleRecords,
            long posts,
            long pendingPosts,
            long openTickets,
            long redTickets,
            long leads,
            long newLeads,
            long partnerOrgs,
            long checkins,
            long scaleResults
    ) {
    }
}
