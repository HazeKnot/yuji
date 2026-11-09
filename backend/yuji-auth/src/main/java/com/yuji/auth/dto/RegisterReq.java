package com.yuji.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterReq(
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        @NotBlank @Size(min = 6, max = 20, message = "密码长度需在 6–20 位之间")
        String password,

        @Size(max = 20, message = "昵称最长 20 字")
        String nickname
) {
}
