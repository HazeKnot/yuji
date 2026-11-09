package com.yuji.profile.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuji.common.BusinessException;
import com.yuji.profile.dto.ProfileCreateReq;
import com.yuji.profile.dto.ProfileVO;
import com.yuji.profile.entity.Profile;
import com.yuji.profile.mapper.ProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 建档服务单元测试。
 * 重点：lmp/edd 二选一换算、建档后自动生成产检计划、重复建档与未建档的错误码契约。
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final Long USER_ID = 9L;

    @Mock
    private ProfileMapper profileMapper;

    @Mock
    private CheckupService checkupService;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        // ObjectMapper 用真实实例：顺带验证 concerns 的 JSON 序列化/反序列化
        profileService = new ProfileService(profileMapper, checkupService, new ObjectMapper());
    }

    @Test
    @DisplayName("只填末次月经：预产期 = lmp + 280 天，并自动生成全孕期产检计划")
    void shouldCreateProfileFromLmpAndGenerateCheckups() {
        LocalDate lmp = LocalDate.now().minusDays(196);       // 孕 28 周整
        when(profileMapper.selectOne(any())).thenReturn(null);

        ProfileVO vo = profileService.create(USER_ID,
                new ProfileCreateReq(lmp, null, "小林", List.of("产检提醒", "情绪状态")));

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileMapper).insert(captor.capture());
        Profile saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getLmpDate()).isEqualTo(lmp);
        assertThat(saved.getEdd()).isEqualTo(lmp.plusDays(280));
        assertThat(saved.getConsentVersion()).as("合规：记录用户同意的隐私政策版本").isEqualTo("v0.1");

        verify(checkupService).generateForUser(USER_ID, lmp);

        assertThat(vo.weeks()).isEqualTo(28);
        assertThat(vo.days()).isZero();
        assertThat(vo.stage()).isEqualTo("孕晚期");
        assertThat(vo.daysToEdd()).isEqualTo(84);
        assertThat(vo.postpartum()).isFalse();
        assertThat(vo.concerns()).containsExactly("产检提醒", "情绪状态");
    }

    @Test
    @DisplayName("只填预产期：反推末次月经 = edd - 280 天，concerns 缺省为空数组")
    void shouldDeriveLmpWhenOnlyEddProvided() {
        LocalDate edd = LocalDate.now().plusDays(84);
        when(profileMapper.selectOne(any())).thenReturn(null);

        ProfileVO vo = profileService.create(USER_ID,
                new ProfileCreateReq(null, edd, null, null));

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileMapper).insert(captor.capture());
        Profile saved = captor.getValue();

        assertThat(saved.getLmpDate()).isEqualTo(edd.minusDays(280));
        assertThat(saved.getEdd()).isEqualTo(edd);
        verify(checkupService).generateForUser(USER_ID, edd.minusDays(280));

        assertThat(vo.nickname()).isNull();
        assertThat(vo.concerns()).isEmpty();
        assertThat(vo.weeks()).isEqualTo(28);
    }

    @Test
    @DisplayName("重复建档返回 40900，且不会重复生成产检计划")
    void shouldRejectDuplicateProfile() {
        Profile existing = new Profile();
        existing.setId(1L);
        existing.setUserId(USER_ID);
        existing.setLmpDate(LocalDate.now().minusDays(100));
        when(profileMapper.selectOne(any())).thenReturn(existing);

        assertThatThrownBy(() -> profileService.create(USER_ID,
                new ProfileCreateReq(LocalDate.now().minusDays(100), null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("你已经建过档了")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40900);

        verify(profileMapper, never()).insert(any(Profile.class));
        verify(checkupService, never()).generateForUser(any(), any());
    }

    @Test
    @DisplayName("lmp 与 edd 都没填：返回 40000，不写库")
    void shouldRejectEmptyDates() {
        when(profileMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> profileService.create(USER_ID,
                new ProfileCreateReq(null, null, "小林", List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请填写末次月经日期或预产期")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40000);

        verify(profileMapper, never()).insert(any(Profile.class));
    }

    @Test
    @DisplayName("未建档查询返回 40401（前端据此跳建档引导）")
    void shouldReturnNoProfileCodeWhenMissing() {
        when(profileMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> profileService.getMine(USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("尚未建档")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40401);
    }

    @Test
    @DisplayName("关注点字段为空（历史数据 / 刚好是空白串）：返回空列表而不是 null")
    void shouldReturnEmptyConcernsWhenBlank() {
        Profile p = new Profile();
        p.setId(1L);
        p.setUserId(USER_ID);
        p.setLmpDate(LocalDate.now().minusDays(30));
        p.setEdd(p.getLmpDate().plusDays(280));
        p.setConcerns("   ");
        when(profileMapper.selectOne(any())).thenReturn(p);

        assertThat(profileService.getMine(USER_ID).concerns()).isEmpty();

        p.setConcerns(null);
        assertThat(profileService.getMine(USER_ID).concerns()).isEmpty();
    }

    @Test
    @DisplayName("已建档查询：返回孕周、阶段与关注点列表")
    void shouldReturnProfileSummary() {
        Profile p = new Profile();
        p.setId(1L);
        p.setUserId(USER_ID);
        p.setNickname("小林");
        p.setLmpDate(LocalDate.now().minusDays(60));            // 孕 8 周 4 天
        p.setEdd(p.getLmpDate().plusDays(280));
        p.setConcerns("[\"产检提醒\"]");
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileVO vo = profileService.getMine(USER_ID);

        assertThat(vo.nickname()).isEqualTo("小林");
        assertThat(vo.weeks()).isEqualTo(8);
        assertThat(vo.days()).isEqualTo(4);
        assertThat(vo.stage()).isEqualTo("孕早期");
        assertThat(vo.concerns()).containsExactly("产检提醒");
    }
}
