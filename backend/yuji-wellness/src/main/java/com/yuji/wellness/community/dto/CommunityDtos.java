package com.yuji.wellness.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class CommunityDtos {

    private CommunityDtos() {
    }

    public record PageVO<T>(
            List<T> records,
            long total,
            long page,
            long size,
            boolean hasMore
    ) {
    }

    public record PostReq(
            @NotBlank String category,
            @NotBlank @Size(min = 4, max = 80) String title,
            @NotBlank @Size(min = 10, max = 2000) String content,
            Boolean anonymous
    ) {
    }

    public record PostVO(
            Long id,
            String category,
            String categoryLabel,
            String title,
            String content,
            String authorName,
            Boolean anonymous,
            Integer likeCount,
            Integer commentCount,
            Boolean liked,
            String status,
            String timeText,
            LocalDateTime createdAt
    ) {
    }

    public record CommentReq(
            @NotBlank @Size(max = 1000) String content,
            Boolean anonymous
    ) {
    }

    public record CommentVO(
            Long id,
            String authorName,
            String content,
            String timeText,
            LocalDateTime createdAt
    ) {
    }

    public record LikeVO(
            Boolean liked,
            Integer likeCount
    ) {
    }
}
