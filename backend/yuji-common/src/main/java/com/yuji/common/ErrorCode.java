package com.yuji.common;

import lombok.Getter;

/**
 * 业务错误码（答辩可讲：错误码分段管理）。
 */
@Getter
public enum ErrorCode {

    PARAM_ERROR(40000, "参数不正确"),
    UNAUTHORIZED(40100, "请先登录"),
    FORBIDDEN(40300, "没有权限执行此操作"),
    NOT_FOUND(40400, "资源不存在"),
    NO_PROFILE(40401, "尚未建档"),
    CONFLICT(40900, "数据冲突"),
    BUSINESS_ERROR(50000, "业务处理失败");

    private final int code;
    private final String defaultMsg;

    ErrorCode(int code, String defaultMsg) {
        this.code = code;
        this.defaultMsg = defaultMsg;
    }
}
