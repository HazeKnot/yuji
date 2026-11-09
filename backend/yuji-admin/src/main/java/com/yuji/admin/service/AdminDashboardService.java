package com.yuji.admin.service;

import com.yuji.admin.dto.DashboardVO;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.common.security.LoginUser;
import com.yuji.common.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 机构看板：只做聚合，不做判读。
 *
 * <p>权限在服务层判定（不依赖前端隐藏入口）：只有 ORG / ADMIN 能读。
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    /** 近 7 天（含今天） */
    private static final int RECENT_DAYS = 7;

    private final DashboardStatsQuery query;

    public DashboardVO overview(LoginUser me) {
        if (me == null || (!Roles.ORG.equals(me.role()) && !Roles.ADMIN.equals(me.role()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该接口仅限机构账号访问");
        }

        LocalDate today = LocalDate.now();
        int early = 0;
        int mid = 0;
        int late = 0;
        int unknown = 0;
        for (LocalDate lmp : query.lmpDates()) {
            if (lmp == null || lmp.isAfter(today)) {
                unknown++;
                continue;
            }
            long weeks = ChronoUnit.DAYS.between(lmp, today) / 7;
            if (weeks < 14) {
                early++;
            } else if (weeks < 28) {
                mid++;
            } else {
                late++;
            }
        }

        long taskTotal = query.countTasks();
        long taskDone = query.countDoneTasks();
        long checkupTotal = query.countCheckups();
        long checkupDone = query.countDoneCheckups();

        return new DashboardVO(
                toInt(query.countMoms()),
                toInt(query.countFamilies()),
                toInt(query.countActiveMembers()),
                toInt(query.countProfiles()),
                toInt(query.countRecords()),
                toInt(query.countReports()),
                toInt(taskTotal),
                toInt(taskDone),
                rate(taskDone, taskTotal),
                toInt(checkupTotal),
                toInt(checkupDone),
                rate(checkupDone, checkupTotal),
                List.of(
                        new DashboardVO.WeekBucket("孕早期", early),
                        new DashboardVO.WeekBucket("孕中期", mid),
                        new DashboardVO.WeekBucket("孕晚期", late),
                        new DashboardVO.WeekBucket("未建档", unknown)),
                recent(today),
                DashboardVO.NOTE);
    }

    /** 近 7 天按日期补零：前端图表的横轴必须是固定 7 格，不能缺天 */
    private List<DashboardVO.DayCount> recent(LocalDate today) {
        LocalDate from = today.minusDays(RECENT_DAYS - 1L);
        Map<LocalDate, Integer> counts = query.recordsSince(from).stream()
                .collect(Collectors.toMap(DashboardStatsQuery.DailyCount::date,
                        DashboardStatsQuery.DailyCount::count, Integer::sum));

        List<DashboardVO.DayCount> result = new ArrayList<>(RECENT_DAYS);
        for (int i = 0; i < RECENT_DAYS; i++) {
            LocalDate day = from.plusDays(i);
            result.add(new DashboardVO.DayCount(day.toString(),
                    counts.getOrDefault(day, 0)));
        }
        return result;
    }

    private static int rate(long done, long total) {
        return total <= 0 ? 0 : (int) Math.round(done * 100.0 / total);
    }

    private static int toInt(long value) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
    }

    /** 供其他服务复用（避免各处重复写角色判断） */
    public static boolean isOrg(LoginUser me) {
        return me != null && (Roles.ORG.equals(me.role()) || Roles.ADMIN.equals(me.role()));
    }
}
