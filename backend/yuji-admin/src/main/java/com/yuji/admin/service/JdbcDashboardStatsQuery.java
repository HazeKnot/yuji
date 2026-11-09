package com.yuji.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计 SQL 集中在这里：一眼能看出"看板上的数字是怎么数出来的"。
 *
 * <p>所有查询都带 {@code deleted_at IS NULL}，与业务表的软删约定保持一致。
 */
@Repository
@RequiredArgsConstructor
public class JdbcDashboardStatsQuery implements DashboardStatsQuery {

    private static final String ACTIVE_MOM =
            "SELECT COUNT(*) FROM `user` WHERE role = 'MOM' AND status = 'ACTIVE' AND deleted_at IS NULL";

    private final JdbcTemplate jdbc;

    @Override
    public long countMoms() {
        return one(ACTIVE_MOM);
    }

    @Override
    public long countFamilies() {
        return one("SELECT COUNT(*) FROM family");
    }

    @Override
    public long countActiveMembers() {
        return one("SELECT COUNT(*) FROM family_member WHERE status = 'ACTIVE'");
    }

    @Override
    public long countProfiles() {
        return one("SELECT COUNT(*) FROM profile WHERE deleted_at IS NULL");
    }

    @Override
    public long countRecords() {
        return one("SELECT COUNT(*) FROM health_record WHERE deleted_at IS NULL");
    }

    @Override
    public long countReports() {
        return one("SELECT COUNT(*) FROM report WHERE deleted_at IS NULL");
    }

    @Override
    public long countTasks() {
        return one("SELECT COUNT(*) FROM family_task");
    }

    @Override
    public long countDoneTasks() {
        return one("SELECT COUNT(*) FROM family_task WHERE status = 'DONE'");
    }

    @Override
    public long countCheckups() {
        return one("SELECT COUNT(*) FROM checkup_item WHERE deleted_at IS NULL");
    }

    @Override
    public long countDoneCheckups() {
        return one("SELECT COUNT(*) FROM checkup_item WHERE status = 'DONE' AND deleted_at IS NULL");
    }

    @Override
    public List<LocalDate> lmpDates() {
        return jdbc.query("SELECT lmp_date FROM profile WHERE deleted_at IS NULL AND lmp_date IS NOT NULL",
                (rs, i) -> rs.getDate(1).toLocalDate());
    }

    @Override
    public List<DailyCount> recordsSince(LocalDate since) {
        return jdbc.query("""
                        SELECT DATE(measured_at) AS d, COUNT(*) AS c
                        FROM health_record
                        WHERE deleted_at IS NULL AND measured_at >= ?
                        GROUP BY DATE(measured_at)
                        """,
                (rs, i) -> new DailyCount(rs.getDate("d").toLocalDate(), rs.getInt("c")),
                since.atStartOfDay());
    }

    private long one(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }
}
