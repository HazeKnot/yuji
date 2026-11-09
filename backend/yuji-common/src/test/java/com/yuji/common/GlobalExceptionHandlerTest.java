package com.yuji.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 全局异常处理器测试。
 * 关键点：对外只返回错误码与安全文案，绝不把堆栈/SQL/内部字段泄露给前端。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("业务异常：原样返回业务错误码与提示")
    void shouldMapBusinessException() {
        Result<Void> r = handler.handleBusiness(new BusinessException(ErrorCode.CONFLICT, "该手机号已注册"));

        assertThat(r.getCode()).isEqualTo(40900);
        assertThat(r.getMsg()).isEqualTo("该手机号已注册");
        assertThat(r.getData()).isNull();
    }

    @Test
    @DisplayName("参数校验失败：拼出「字段 + 原因」，便于前端定位")
    void shouldJoinFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("req", "phone", "手机号格式不正确"),
                new FieldError("req", "password", "密码长度需在 6–20 位之间")));
        MethodArgumentNotValidException e = new MethodArgumentNotValidException(null, bindingResult);

        Result<Void> r = handler.handleValid(e);

        assertThat(r.getCode()).isEqualTo(40000);
        assertThat(r.getMsg()).contains("phone 手机号格式不正确").contains("password");
    }

    @Test
    @DisplayName("未知异常：统一 50000，且响应里不包含内部细节（安全管理要求）")
    void shouldNotLeakInternalDetail() {
        Result<Void> r = handler.handleOther(
                new IllegalStateException("jdbc:mysql://10.0.0.8:3306/yuji password=yuji123"));

        assertThat(r.getCode()).isEqualTo(50000);
        assertThat(r.getMsg()).isEqualTo("系统开小差了，请稍后再试")
                .doesNotContain("jdbc")
                .doesNotContain("password")
                .doesNotContain("10.0.0.8");
        assertThat(r.getData()).isNull();
    }
}
