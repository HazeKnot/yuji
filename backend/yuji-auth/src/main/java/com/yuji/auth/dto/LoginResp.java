package com.yuji.auth.dto;

public record LoginResp(String token, UserVO user) {

    public record UserVO(Long id, String phone, String nickname, String role) {
    }
}
