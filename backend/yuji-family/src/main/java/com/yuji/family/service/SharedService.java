package com.yuji.family.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.family.dto.SharedSummaryVO;
import com.yuji.family.entity.FamilyTask;
import com.yuji.family.mapper.FamilyTaskMapper;
import com.yuji.profile.dto.HealthRecordVO;
import com.yuji.profile.dto.ProfileVO;
import com.yuji.profile.dto.TrendVO;
import com.yuji.profile.entity.CheckupItem;
import com.yuji.profile.mapper.CheckupItemMapper;
import com.yuji.profile.service.HealthRecordService;
import com.yuji.profile.service.MetricType;
import com.yuji.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 家属视角的只读数据。
 *
 * <p>默认可见：状态摘要（孕周 / 产检进度 / 任务完成率）。
 * 需要逐项授权才可见：指标四类。情绪与量表得分没有任何入口。
 */
@Service
@RequiredArgsConstructor
public class SharedService {

    private final FamilyService familyService;
    private final ProfileService profileService;
    private final HealthRecordService healthRecordService;
    private final CheckupItemMapper checkupItemMapper;
    private final FamilyTaskMapper taskMapper;

    public SharedSummaryVO summary(Long userId) {
        FamilyService.Membership ms = requireMember(userId);
        Long ownerId = ms.ownerUserId();

        ProfileVO p = null;
        try {
            p = profileService.getMine(ownerId);
        } catch (BusinessException e) {
            // 妈妈还没建档：摘要是空壳，但不该让家属页 500
            if (e.getCode() != ErrorCode.NO_PROFILE.getCode()) {
                throw e;
            }
        }

        long checkupTotal = count(checkupItemMapper.selectCount(new LambdaQueryWrapper<CheckupItem>()
                .eq(CheckupItem::getUserId, ownerId)));
        long checkupDone = count(checkupItemMapper.selectCount(new LambdaQueryWrapper<CheckupItem>()
                .eq(CheckupItem::getUserId, ownerId)
                .eq(CheckupItem::getStatus, "DONE")));
        CheckupItem next = checkupItemMapper.selectOne(new LambdaQueryWrapper<CheckupItem>()
                .eq(CheckupItem::getUserId, ownerId)
                .eq(CheckupItem::getStatus, "PLANNED")
                .orderByAsc(CheckupItem::getDueDate)
                .last("LIMIT 1"));

        long taskTotal = count(taskMapper.selectCount(new LambdaQueryWrapper<FamilyTask>()
                .eq(FamilyTask::getFamilyId, ms.family().getId())
                .eq(FamilyTask::getAssigneeUserId, userId)));
        long taskDone = count(taskMapper.selectCount(new LambdaQueryWrapper<FamilyTask>()
                .eq(FamilyTask::getFamilyId, ms.family().getId())
                .eq(FamilyTask::getAssigneeUserId, userId)
                .eq(FamilyTask::getStatus, FamilyTask.DONE)));

        return new SharedSummaryVO(
                p == null ? familyService.nicknameOf(ownerId) : p.nickname(),
                p == null ? null : p.weeks(),
                p == null ? null : p.days(),
                p == null ? null : p.stage(),
                p == null ? null : p.edd(),
                p == null ? null : p.daysToEdd(),
                Math.toIntExact(checkupTotal),
                Math.toIntExact(checkupDone),
                next == null ? null : next.getName(),
                next == null ? null : next.getDueDate(),
                Math.toIntExact(taskTotal),
                Math.toIntExact(taskDone),
                FamilyService.splitFields(ms.member() == null ? "" : ms.member().getVisibleFields()),
                SharedSummaryVO.PRIVACY_NOTE);
    }

    public TrendVO trend(Long userId, String typeCode, Integer days) {
        FamilyService.Membership ms = requireMember(userId);
        MetricType type = MetricType.parse(typeCode);
        requireVisible(ms, type);
        return healthRecordService.trend(ms.ownerUserId(), type.name(), days);
    }

    public List<HealthRecordVO> records(Long userId, String typeCode) {
        FamilyService.Membership ms = requireMember(userId);
        if (typeCode == null || typeCode.isBlank()) {
            // 不指定类型时，只返回已授权的那几类
            List<String> visible = FamilyService.splitFields(
                    ms.member() == null ? "" : ms.member().getVisibleFields());
            if (visible.isEmpty()) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "她还没有授权任何指标给你");
            }
            return visible.stream()
                    .flatMap(code -> healthRecordService
                            .listMine(ms.ownerUserId(), code, null, null).stream())
                    .sorted((a, b) -> b.measuredAt().compareTo(a.measuredAt()))
                    .toList();
        }
        MetricType type = MetricType.parse(typeCode);
        requireVisible(ms, type);
        return healthRecordService.listMine(ms.ownerUserId(), type.name(), null, null);
    }

    private void requireVisible(FamilyService.Membership ms, MetricType type) {
        List<String> visible = FamilyService.splitFields(
                ms.member() == null ? "" : ms.member().getVisibleFields());
        if (!visible.contains(type.name())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    type.label() + "还没有对你开放，可以请她授权");
        }
    }

    private FamilyService.Membership requireMember(Long userId) {
        FamilyService.Membership ms = familyService.requireFamily(userId);
        if (ms.isOwner()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "这是家属视图；你自己的数据在「记录」页");
        }
        return ms;
    }

    private static long count(Long v) {
        return v == null ? 0L : v;
    }
}
