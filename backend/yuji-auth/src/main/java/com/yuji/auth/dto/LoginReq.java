package com.yuji.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginReq(
        @NotBlank String phone,
        @NotBlank String password
) {
}
