package com.yuji.common;

import com.yuji.common.security.LoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 统一返回体与错误码契约测试。
 * 前端依赖这些约定（code=0 成功、40401 跳建档引导），改错会直接导致联调失败。
 */
class ResultTest {

    @Test
    @DisplayName("ok() 的 code 恒为 0、msg 为 ok，并原样返回数据")
    void okShouldCarryDataAndZeroCode() {
        Result<String> r = Result.ok("x");
        assertThat(r.getCode()).isZero();
        assertThat(r.getMsg()).isEqualTo("ok");
        assertThat(r.getData()).isEqualTo("x");
    }

    @Test
    @DisplayName("ok() 无参时 data 为 null")
    void okWithoutArgShouldHaveNullData() {
        Result<Void> r = Result.ok();
        assertThat(r.getCode()).isZero();
        assertThat(r.getData()).isNull();
    }

    @Test
    @DisplayName("fail() 保留传入的 code 与 msg，且 data 为空")
    void failShouldKeepCodeAndMsg() {
        Result<Object> r = Result.fail(ErrorCode.NO_PROFILE.getCode(), "尚未建档");
        assertThat(r.getCode()).isEqualTo(40401);
        assertThat(r.getMsg()).isEqualTo("尚未建档");
        assertThat(r.getData()).isNull();
    }

    @Test
    @DisplayName("BusinessException 携带错误码与消息")
    void businessExceptionShouldCarryCode() {
        BusinessException e = new BusinessException(ErrorCode.CONFLICT, "该手机号已注册");
        assertThat(e.getCode()).isEqualTo(40900);
        assertThat(e).hasMessage("该手机号已注册");

        BusinessException byDefault = new BusinessException(ErrorCode.NO_PROFILE);
        assertThat(byDefault.getCode()).isEqualTo(40401);
        assertThat(byDefault).hasMessage("尚未建档");
    }

    @Test
    @DisplayName("也可直接指定数值错误码（用于三方回调等场景）")
    void shouldSupportCustomCode() {
        BusinessException e = new BusinessException(40001, "自定义码");
        assertThat(e.getCode()).isEqualTo(40001);
        assertThat(e).hasMessage("自定义码");
    }

    @Test
    @DisplayName("错误码分段不重复：参数/鉴权/资源/冲突/业务各自独立区段")
    void errorCodesShouldBeUniqueAndSegmented() {
        Set<Integer> codes = new HashSet<>();
        for (ErrorCode ec : ErrorCode.values()) {
            assertThat(codes.add(ec.getCode()))
                    .as("错误码 %s 重复", ec.name())
                    .isTrue();
        }
        // 关键错误码必须稳定，前端与文档都依赖它们
        assertThat(ErrorCode.PARAM_ERROR.getCode()).isEqualTo(40000);
        assertThat(ErrorCode.UNAUTHORIZED.getCode()).isEqualTo(40100);
        assertThat(ErrorCode.FORBIDDEN.getCode()).isEqualTo(40300);
        assertThat(ErrorCode.NOT_FOUND.getCode()).isEqualTo(40400);
        assertThat(ErrorCode.NO_PROFILE.getCode()).isEqualTo(40401);
        assertThat(ErrorCode.CONFLICT.getCode()).isEqualTo(40900);
        assertThat(ErrorCode.BUSINESS_ERROR.getCode()).isEqualTo(50000);
    }

    @Test
    @DisplayName("LoginUser 可判断是否为妈妈角色")
    void loginUserShouldTellMomRole() {
        assertThat(new LoginUser(1L, "MOM").isMom()).isTrue();
        assertThat(new LoginUser(2L, "FAMILY").isMom()).isFalse();
    }

    @Test
    @DisplayName("BusinessException 是运行时异常，可直接被全局处理器捕获")
    void businessExceptionShouldBeRuntimeException() {
        assertThatThrownBy(() -> {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "bad");
        }).isInstanceOf(RuntimeException.class);
    }
}
