package com.yuji.auth.service;

import com.yuji.auth.dto.LoginReq;
import com.yuji.auth.dto.LoginResp;
import com.yuji.auth.dto.RegisterReq;
import com.yuji.auth.entity.User;
import com.yuji.auth.mapper.UserMapper;
import com.yuji.auth.security.JwtService;
import com.yuji.common.BusinessException;
import com.yuji.common.security.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注册/登录服务单元测试。
 * 密码编码器用真实 BCrypt（不 mock），顺带验证"入库的是哈希而非明文"这条安全契约。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, passwordEncoder, jwtService);
    }

    // ---------- 注册 ----------

    @Test
    @DisplayName("注册成功：密码以 BCrypt 哈希入库，默认昵称为准妈妈，角色为 MOM，返回 token")
    void shouldRegisterWithHashedPassword() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(jwtService.issue(any(), anyString())).thenReturn("token-abc");

        LoginResp resp = authService.register(new RegisterReq("13800138000", "secret123", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getPhone()).isEqualTo("13800138000");
        assertThat(saved.getPasswordHash())
                .as("不得存明文密码")
                .isNotEqualTo("secret123")
                .startsWith("$2");
        assertThat(passwordEncoder.matches("secret123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getNickname()).isEqualTo("准妈妈");
        assertThat(saved.getRole()).isEqualTo(Roles.MOM);
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");

        assertThat(resp.token()).isEqualTo("token-abc");
        assertThat(resp.user().phone()).isEqualTo("13800138000");
        assertThat(resp.user().role()).isEqualTo(Roles.MOM);
    }

    @Test
    @DisplayName("注册时填写昵称则保留，空白昵称回退为默认值")
    void shouldKeepGivenNicknameOrFallback() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(jwtService.issue(any(), anyString())).thenReturn("t");

        authService.register(new RegisterReq("13800138001", "secret123", "小林"));
        authService.register(new RegisterReq("13800138002", "secret123", "   "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, org.mockito.Mockito.times(2)).insert(captor.capture());

        assertThat(captor.getAllValues().get(0).getNickname()).isEqualTo("小林");
        assertThat(captor.getAllValues().get(1).getNickname()).isEqualTo("准妈妈");
    }

    @Test
    @DisplayName("手机号已注册返回 40900，且不写库、不发 token")
    void shouldRejectDuplicatePhone() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(new RegisterReq("13800138000", "secret123", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该手机号已注册")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40900);

        verify(userMapper, never()).insert(any(User.class));
        verify(jwtService, never()).issue(any(), anyString());
    }

    // ---------- 登录 ----------

    @Test
    @DisplayName("登录成功：返回 token 与用户信息")
    void shouldLoginWithCorrectPassword() {
        User user = user(5L, "13800138000", "secret123", "ACTIVE");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(jwtService.issue(5L, Roles.MOM)).thenReturn("token-xyz");

        LoginResp resp = authService.login(new LoginReq("13800138000", "secret123"));

        assertThat(resp.token()).isEqualTo("token-xyz");
        assertThat(resp.user().id()).isEqualTo(5L);
        assertThat(resp.user().nickname()).isEqualTo("小林");
    }

    @Test
    @DisplayName("密码错误返回 40100")
    void shouldRejectWrongPassword() {
        when(userMapper.selectOne(any())).thenReturn(user(5L, "13800138000", "secret123", "ACTIVE"));

        assertThatThrownBy(() -> authService.login(new LoginReq("13800138000", "wrong-pass")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("手机号或密码不正确")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40100);

        verify(jwtService, never()).issue(any(), anyString());
    }

    @Test
    @DisplayName("账号不存在也返回 40100（不泄露手机号是否注册过）")
    void shouldRejectUnknownAccount() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login(new LoginReq("13900139000", "secret123")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("手机号或密码不正确")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40100);
    }

    @Test
    @DisplayName("账号被禁用返回 40300")
    void shouldRejectInactiveAccount() {
        when(userMapper.selectOne(any())).thenReturn(user(5L, "13800138000", "secret123", "DISABLED"));

        assertThatThrownBy(() -> authService.login(new LoginReq("13800138000", "secret123")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号不可用，请联系客服")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40300);
    }

    private User user(Long id, String phone, String rawPassword, String status) {
        User u = new User();
        u.setId(id);
        u.setPhone(phone);
        u.setNickname("小林");
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRole(Roles.MOM);
        u.setStatus(status);
        return u;
    }
}
