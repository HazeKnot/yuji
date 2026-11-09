package com.yuji.admin.service;

import com.yuji.admin.dto.DashboardVO;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.common.security.LoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 看板的三个易错点：权限、除零、近 7 天补零。
 */
@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    private static final LoginUser ORG = new LoginUser(100L, "ORG");

    @Mock
    private DashboardStatsQuery query;

    @InjectMocks
    private AdminDashboardService service;

    @Test
    @DisplayName("妈妈账号读看板：40300（权限在服务层，不靠前端藏入口）")
    void shouldRejectMom() {
        assertThatThrownBy(() -> service.overview(new LoginUser(1L, "MOM")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());

        assertThatThrownBy(() -> service.overview(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("机构账号");
    }

    @Test
    @DisplayName("空库：所有比率为 0 而不是除零，近 7 天仍返回 7 个点")
    void shouldBeZeroSafeOnEmptyDatabase() {
        when(query.lmpDates()).thenReturn(List.of());

        DashboardVO vo = service.overview(ORG);

        assertThat(vo.momCount()).isZero();
        assertThat(vo.taskRate()).isZero();
        assertThat(vo.checkupRate()).isZero();
        assertThat(vo.weekBuckets()).extracting(DashboardVO.WeekBucket::label)
                .containsExactly("孕早期", "孕中期", "孕晚期", "未建档");
        assertThat(vo.recentRecords()).hasSize(7).allSatisfy(d -> assertThat(d.count()).isZero());
        assertThat(vo.note()).contains("不含任何个人明细");
    }

    @Test
    @DisplayName("正常汇总：完成率四舍五入；家庭与成员数分别统计")
    void shouldAggregateCounts() {
        stubCounts(3, 2, 2, 3, 10, 1, 8, 3, 20, 5);
        when(query.lmpDates()).thenReturn(List.of());

        DashboardVO vo = service.overview(ORG);

        assertThat(vo.momCount()).isEqualTo(3);
        assertThat(vo.familyCount()).isEqualTo(2);
        assertThat(vo.memberCount()).isEqualTo(2);
        assertThat(vo.profileCount()).isEqualTo(3);
        assertThat(vo.recordCount()).isEqualTo(10);
        assertThat(vo.reportCount()).isEqualTo(1);
        assertThat(vo.taskTotal()).isEqualTo(8);
        assertThat(vo.taskDone()).isEqualTo(3);
        assertThat(vo.taskRate()).isEqualTo(38);
        assertThat(vo.checkupRate()).isEqualTo(25);
    }

    @Test
    @DisplayName("孕周分桶：早/中/晚按 lmp 天数换算，空值与未来日期归入未建档")
    void shouldBucketByWeeks() {
        stubCounts(1, 0, 0, 5, 0, 0, 0, 0, 0, 0);
        LocalDate today = LocalDate.now();
        // List.of 不允许 null，这里用 Arrays.asList 模拟"有档案但没填 lmp"的历史数据
        when(query.lmpDates()).thenReturn(Arrays.asList(
                today.minusDays(70),   // 10 周
                today.minusDays(140),  // 20 周
                today.minusDays(210),  // 30 周
                null,
                today.plusDays(3)));

        DashboardVO vo = service.overview(ORG);

        assertThat(vo.weekBuckets()).extracting(DashboardVO.WeekBucket::count)
                .containsExactly(1, 1, 1, 2);
    }

    @Test
    @DisplayName("近 7 天：缺数据的日期补 0，同一天多条合并")
    void shouldFillMissingDays() {
        stubCounts(1, 0, 0, 1, 5, 0, 0, 0, 0, 0);
        LocalDate today = LocalDate.now();
        when(query.lmpDates()).thenReturn(List.of());
        when(query.recordsSince(today.minusDays(6))).thenReturn(List.of(
                new DashboardStatsQuery.DailyCount(today, 4),
                new DashboardStatsQuery.DailyCount(today.minusDays(3), 2),
                new DashboardStatsQuery.DailyCount(today.minusDays(3), 1)));

        List<DashboardVO.DayCount> recent = service.overview(ORG).recentRecords();

        assertThat(recent).hasSize(7);
        assertThat(recent.get(6).date()).isEqualTo(today.toString());
        assertThat(recent.get(6).count()).isEqualTo(4);
        assertThat(recent.get(3).count()).isEqualTo(3);
        assertThat(recent.get(0).count()).isZero();
    }

    // ---------- 夹具 ----------

    private void stubCounts(long moms, long families, long members, long profiles,
                            long records, long reports, long tasks, long doneTasks,
                            long checkups, long doneCheckups) {
        when(query.countMoms()).thenReturn(moms);
        when(query.countFamilies()).thenReturn(families);
        when(query.countActiveMembers()).thenReturn(members);
        when(query.countProfiles()).thenReturn(profiles);
        when(query.countRecords()).thenReturn(records);
        when(query.countReports()).thenReturn(reports);
        when(query.countTasks()).thenReturn(tasks);
        when(query.countDoneTasks()).thenReturn(doneTasks);
        when(query.countCheckups()).thenReturn(checkups);
        when(query.countDoneCheckups()).thenReturn(doneCheckups);
    }
}
