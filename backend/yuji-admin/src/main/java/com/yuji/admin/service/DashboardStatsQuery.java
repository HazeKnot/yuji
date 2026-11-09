package com.yuji.admin.service;

import java.time.LocalDate;
import java.util.List;

/**
 * 看板数据来源。抽成接口的目的：统计口径（SQL）与业务组装（率、分桶、补零）分开，
 * 后者可以纯单测，不需要数据库。
 */
public interface DashboardStatsQuery {

    long countMoms();

    long countFamilies();

    long countActiveMembers();

    long countProfiles();

    long countRecords();

    long countReports();

    long countTasks();

    long countDoneTasks();

    long countCheckups();

    long countDoneCheckups();

    /** 已建档用户的末次月经，用于按孕周分桶 */
    List<LocalDate> lmpDates();

    /** 从 since（含）开始的每日记录条数 */
    List<DailyCount> recordsSince(LocalDate since);

    record DailyCount(LocalDate date, int count) {
    }
}
