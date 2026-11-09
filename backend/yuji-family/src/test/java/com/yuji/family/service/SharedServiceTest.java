package com.yuji.family.service;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.family.TableInfoInitializer;
import com.yuji.family.dto.SharedSummaryVO;
import com.yuji.family.entity.Family;
import com.yuji.family.entity.FamilyMember;
import com.yuji.family.entity.FamilyTask;
import com.yuji.family.mapper.FamilyTaskMapper;
import com.yuji.profile.dto.HealthRecordVO;
import com.yuji.profile.dto.ProfileVO;
import com.yuji.profile.dto.TrendVO;
import com.yuji.profile.entity.CheckupItem;
import com.yuji.profile.mapper.CheckupItemMapper;
import com.yuji.profile.service.HealthRecordService;
import com.yuji.profile.service.ProfileService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 家属共享视图：默认只给状态摘要，指标必须逐项授权；情绪/量表没有入口。
 */
@ExtendWith(MockitoExtension.class)
class SharedServiceTest {

    private static final Long MOM = 1L;
    private static final Long MEMBER = 9L;
    private static final Long FAMILY_ID = 10L;

    @Mock
    private FamilyService familyService;
    @Mock
    private ProfileService profileService;
    @Mock
    private HealthRecordService healthRecordService;
    @Mock
    private CheckupItemMapper checkupItemMapper;
    @Mock
    private FamilyTaskMapper taskMapper;

    @InjectMocks
    private SharedService service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoInitializer.init(CheckupItem.class, FamilyTask.class);
    }

    @Test
    @DisplayName("妈妈本人调家属视图：40300（避免自己看自己的共享页）")
    void shouldRejectOwner() {
        when(familyService.requireFamily(MOM))
                .thenReturn(new FamilyService.Membership(family(), FamilyRole.OWNER, null));

        assertCode(() -> service.summary(MOM), ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("摘要：孕周/产检进度/任务完成率 + 隐私声明，且不含任何情绪字段")
    void shouldBuildSummary() {
        stubMember("WEIGHT");
        when(profileService.getMine(MOM)).thenReturn(profile());
        when(checkupItemMapper.selectCount(any())).thenReturn(10L, 2L);
        CheckupItem next = new CheckupItem();
        next.setName("糖耐量试验");
        next.setDueDate(LocalDate.now().plusDays(6));
        when(checkupItemMapper.selectOne(any())).thenReturn(next);
        when(taskMapper.selectCount(any())).thenReturn(4L, 1L);

        SharedSummaryVO vo = service.summary(MEMBER);

        assertThat(vo.weeks()).isEqualTo(28);
        assertThat(vo.stage()).isEqualTo("孕晚期");
        assertThat(vo.checkupTotal()).isEqualTo(10);
        assertThat(vo.checkupDone()).isEqualTo(2);
        assertThat(vo.nextCheckupName()).isEqualTo("糖耐量试验");
        assertThat(vo.taskTotal()).isEqualTo(4);
        assertThat(vo.taskDone()).isEqualTo(1);
        assertThat(vo.visibleFields()).containsExactly("WEIGHT");
        assertThat(vo.privacyNote()).contains("情绪记录与量表得分不对家属开放");
    }

    @Test
    @DisplayName("妈妈还没建档：摘要不 500，孕周字段留空")
    void shouldTolerateMissingProfile() {
        stubMember("");
        when(familyService.nicknameOf(MOM)).thenReturn("小满");
        when(profileService.getMine(MOM))
                .thenThrow(new BusinessException(ErrorCode.NO_PROFILE));
        when(checkupItemMapper.selectCount(any())).thenReturn(0L);
        when(checkupItemMapper.selectOne(any())).thenReturn(null);
        when(taskMapper.selectCount(any())).thenReturn(0L);

        SharedSummaryVO vo = service.summary(MEMBER);

        assertThat(vo.weeks()).isNull();
        assertThat(vo.stage()).isNull();
        assertThat(vo.nextCheckupName()).isNull();
        assertThat(vo.checkupTotal()).isZero();
    }

    @Test
    @DisplayName("未授权的指标：趋势接口 40300，提示可请她授权")
    void shouldRejectUnauthorizedMetric() {
        stubMember("BLOOD_PRESSURE");

        assertThatThrownBy(() -> service.trend(MEMBER, "WEIGHT", 30))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("体重还没有对你开放");
        verify(healthRecordService, org.mockito.Mockito.never())
                .trend(anyLong(), any(), any());
    }

    @Test
    @DisplayName("已授权：趋势读到的是妈妈的数据（ownerId 而非当前用户）")
    void shouldReadOwnersTrend() {
        stubMember("WEIGHT");
        TrendVO trend = new TrendVO("WEIGHT", "体重", "kg", List.of(), null, null, null, TrendVO.DISCLAIMER);
        when(healthRecordService.trend(MOM, "WEIGHT", 30)).thenReturn(trend);

        assertThat(service.trend(MEMBER, "weight", 30)).isSameAs(trend);
        verify(healthRecordService).trend(MOM, "WEIGHT", 30);
    }

    @Test
    @DisplayName("记录列表：不指定类型时只汇总已授权的那几类，并按时间倒序")
    void shouldMergeAuthorizedRecords() {
        stubMember("WEIGHT,BLOOD_PRESSURE");
        HealthRecordVO older = record("WEIGHT", LocalDateTime.now().minusDays(2));
        HealthRecordVO newer = record("BLOOD_PRESSURE", LocalDateTime.now().minusHours(1));
        when(healthRecordService.listMine(MOM, "WEIGHT", null, null)).thenReturn(List.of(older));
        when(healthRecordService.listMine(MOM, "BLOOD_PRESSURE", null, null)).thenReturn(List.of(newer));

        List<HealthRecordVO> list = service.records(MEMBER, null);

        assertThat(list).containsExactly(newer, older);
    }

    @Test
    @DisplayName("一个字段都没授权时，列表接口明确拒绝而不是返回空数组")
    void shouldRejectWhenNothingShared() {
        stubMember("");

        assertCode(() -> service.records(MEMBER, null), ErrorCode.FORBIDDEN);
    }

    // ---------- 夹具 ----------

    private void stubMember(String fields) {
        FamilyMember m = new FamilyMember();
        m.setId(1L);
        m.setFamilyId(FAMILY_ID);
        m.setUserId(MEMBER);
        m.setVisibleFields(fields);
        m.setStatus("ACTIVE");
        when(familyService.requireFamily(MEMBER))
                .thenReturn(new FamilyService.Membership(family(), FamilyRole.MEMBER, m));
    }

    private void assertCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(expected.getCode());
    }

    private static Family family() {
        Family f = new Family();
        f.setId(FAMILY_ID);
        f.setOwnerUserId(MOM);
        return f;
    }

    private static ProfileVO profile() {
        return new ProfileVO(1L, "小满", LocalDate.now().minusDays(196),
                LocalDate.now().plusDays(84), List.of(), 28, 0, "孕晚期", 84L, false);
    }

    private static HealthRecordVO record(String type, LocalDateTime at) {
        return new HealthRecordVO(1L, type, type, BigDecimal.TEN, null, "kg", at, null);
    }
}
