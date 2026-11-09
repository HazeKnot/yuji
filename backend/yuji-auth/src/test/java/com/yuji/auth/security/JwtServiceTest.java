package com.yuji.auth.security;

import com.yuji.common.security.LoginUser;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 签发/解析测试。
 * 覆盖：正常往返、跨实例验证（无状态）、换密钥必失败、过期必失败、脏数据不崩。
 */
class JwtServiceTest {

    private static final String SECRET = "yuji-unit-test-secret-0123456789abcdef0123456789";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = newService(SECRET, 7);
    }

    @Test
    @DisplayName("签发后可解析回用户 ID 与角色（标准三段式 JWT）")
    void shouldRoundTripToken() {
        String token = jwtService.issue(42L, "MOM");

        assertThat(token).as("JWT 应为 header.payload.signature 三段式").contains(".");
        assertThat(token.split("\\.")).hasSize(3);

        LoginUser user = jwtService.parse(token);
        assertThat(user.userId()).isEqualTo(42L);
        assertThat(user.role()).isEqualTo("MOM");
        assertThat(user.isMom()).isTrue();
    }

    @Test
    @DisplayName("无状态：同一密钥的另一实例可解析该 token（多实例部署前提）")
    void shouldBeStatelessAcrossInstances() {
        String token = jwtService.issue(7L, "FAMILY");

        LoginUser user = newService(SECRET, 7).parse(token);
        assertThat(user.userId()).isEqualTo(7L);
        assertThat(user.role()).isEqualTo("FAMILY");
        assertThat(user.isMom()).isFalse();
    }

    @Test
    @DisplayName("换密钥后验签失败，不允许越权解析")
    void shouldRejectTokenSignedByOtherSecret() {
        String token = jwtService.issue(1L, "MOM");
        JwtService other = newService("another-secret-0123456789abcdef0123456789xyz", 7);

        assertThatThrownBy(() -> other.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("过期的 token 解析抛 JwtException（过滤器据此返回 401）")
    void shouldRejectExpiredToken() {
        JwtService expiring = newService(SECRET, -1);

        String token = expiring.issue(1L, "MOM");

        assertThatThrownBy(() -> expiring.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("乱码/空串不会被当成合法身份")
    void shouldRejectGarbageToken() {
        assertThatThrownBy(() -> jwtService.parse("not-a-jwt")).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwtService.parse("a.b.c")).isInstanceOf(JwtException.class);
        // 空串在参数校验层就被拒绝（IllegalArgumentException，不是 JwtException）；
        // JwtAuthFilter 捕获取的是 Exception，两种都能拦住，不会产生非法登录态
        assertThatThrownBy(() -> jwtService.parse("")).isInstanceOf(IllegalArgumentException.class);
    }

    private static JwtService newService(String secret, int expireDays) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expireDays", expireDays);
        return service;
    }
}
