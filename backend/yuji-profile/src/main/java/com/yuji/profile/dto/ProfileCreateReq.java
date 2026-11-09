package com.yuji.profile.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 建档请求：lmpDate 与 edd 二选一（服务层校验），均填时以 lmpDate 为准。
 */
public record ProfileCreateReq(
        LocalDate lmpDate,

        LocalDate edd,

        @Size(max = 20, message = "昵称最长 20 字")
        String nickname,

        List<String> concerns
) {
}
