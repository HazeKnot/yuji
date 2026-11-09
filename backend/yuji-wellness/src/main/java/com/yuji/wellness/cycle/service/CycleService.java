package com.yuji.wellness.cycle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.wellness.cycle.dto.CycleDtos;
import com.yuji.wellness.cycle.entity.CycleRecord;
import com.yuji.wellness.cycle.mapper.CycleRecordMapper;
import com.yuji.wellness.profile.entity.WellnessProfile;
import com.yuji.wellness.profile.mapper.WellnessProfileMapper;
import com.yuji.wellness.support.CyclePhase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 周期记录与预测。
 *
 * <p>做的是"基于自己历史记录的日历估算"，不是排卵检测，也不能用于避孕。
 * 预测逻辑是纯函数，便于单测与答辩时解释口径。
 */
@Service
@RequiredArgsConstructor
public class CycleService {

    public static final String DISCLAIMER = "周期预测基于你记录的历史数据推算，仅供参考，不能用于避孕或判断排卵。";

    private static final int MIN_CYCLE = 15;
    private static final int MAX_CYCLE = 60;
    private static final int STALE_DAYS = 15;

    private final CycleRecordMapper recordMapper;
    private final WellnessProfileMapper profileMapper;

    @Transactional
    public CycleDtos.RecordVO add(Long userId, CycleDtos.RecordReq req) {
        LocalDate today = LocalDate.now();
        if (req.startDate().isAfter(today)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始日期不能晚于今天");
        }
        if (req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
        Long exists = recordMapper.selectCount(new LambdaQueryWrapper<CycleRecord>()
                .eq(CycleRecord::getUserId, userId)
                .eq(CycleRecord::getStartDate, req.startDate())
                .isNull(CycleRecord::getDeletedAt));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "这一天已经记录过了");
        }

        CycleRecord row = new CycleRecord();
        row.setUserId(userId);
        row.setStartDate(req.startDate());
        row.setEndDate(req.endDate());
        row.setFlow(flowOf(req.flow()));
        row.setSymptoms(join(req.symptoms()));
        row.setMood(req.mood());
        row.setNote(req.note());
        recordMapper.insert(row);

        // 同步最近一次月经到全周期档案，月事餐饮与首页用它兜底
        WellnessProfile profile = findProfile(userId);
        if (profile != null && (profile.getLastPeriodDate() == null
                || req.startDate().isAfter(profile.getLastPeriodDate()))) {
            profile.setLastPeriodDate(req.startDate());
            profileMapper.updateById(profile);
        }
        return toVO(row);
    }

    public List<CycleDtos.RecordVO> list(Long userId) {
        return records(userId).stream().map(this::toVO).toList();
    }

    public void delete(Long userId, Long id) {
        CycleRecord row = recordMapper.selectById(id);
        if (row == null || row.getDeletedAt() != null || !userId.equals(row.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "记录不存在");
        }
        row.setDeletedAt(java.time.LocalDateTime.now());
        recordMapper.updateById(row);
    }

    /** 首页/餐饮都要用；没有记录返回 null，不抛异常。 */
    public CycleDtos.SummaryVO summary(Long userId) {
        List<CycleRecord> rows = records(userId);
        WellnessProfile profile = findProfile(userId);
        LocalDate fallback = profile == null ? null : profile.getLastPeriodDate();
        int defaultCycle = profile == null ? 28 : safe(profile.getCycleLength(), 28);
        int defaultPeriod = profile == null ? 5 : safe(profile.getPeriodLength(), 5);
        return buildSummary(rows, fallback, defaultCycle, defaultPeriod, LocalDate.now());
    }

    // ---------- 纯函数（可直接单测） ----------

    public static CycleDtos.SummaryVO buildSummary(List<CycleRecord> rows,
                                                   LocalDate fallbackStart,
                                                   int defaultCycle,
                                                   int defaultPeriod,
                                                   LocalDate today) {
        List<LocalDate> starts = new ArrayList<>();
        for (CycleRecord r : rows) {
            if (r.getStartDate() != null) {
                starts.add(r.getStartDate());
            }
        }
        starts.sort(LocalDate::compareTo);

        LocalDate lastStart = starts.isEmpty() ? fallbackStart : starts.get(starts.size() - 1);
        int avgCycle = averageCycle(starts, defaultCycle);
        int avgPeriod = averagePeriod(rows, defaultPeriod);

        Integer cycleDay = null;
        if (lastStart != null && !lastStart.isAfter(today)) {
            long days = ChronoUnit.DAYS.between(lastStart, today) + 1;
            if (days <= avgCycle + STALE_DAYS) {
                cycleDay = (int) days;
            }
        }

        String phase = cycleDay == null ? null : phaseOf(cycleDay, avgCycle, avgPeriod).name();
        String phaseLabel = phase == null ? null : CyclePhase.valueOf(phase).label();
        String phaseHint = phase == null ? null : CyclePhase.valueOf(phase).hint();

        LocalDate nextPeriod = null;
        Long daysToNext = null;
        // 记录过期太久时不再硬推下一次：宁可让用户补记，也不给一个看似精确的假日期
        if (lastStart != null && cycleDay != null) {
            nextPeriod = lastStart.plusDays(avgCycle);
            while (nextPeriod.isBefore(today)) {
                nextPeriod = nextPeriod.plusDays(avgCycle);
            }
            daysToNext = ChronoUnit.DAYS.between(today, nextPeriod);
        }

        List<CycleDtos.PredictionVO> predictions = new ArrayList<>();
        if (lastStart != null && cycleDay != null) {
            for (int i = 1; i <= 3; i++) {
                LocalDate s = lastStart.plusDays((long) avgCycle * i);
                while (s.isBefore(today)) {
                    s = s.plusDays(avgCycle);
                }
                predictions.add(new CycleDtos.PredictionVO(s, s.plusDays(avgPeriod - 1L), null));
            }
        }

        return new CycleDtos.SummaryVO(lastStart, cycleDay, phase, phaseLabel, phaseHint,
                avgCycle, avgPeriod, nextPeriod, daysToNext, predictions,
                rows.stream().map(CycleService::toVOPublic).toList(),
                DISCLAIMER);
    }

    public static CyclePhase phaseOf(int cycleDay, int cycleLength, int periodLength) {
        if (cycleDay <= periodLength) {
            return CyclePhase.MENSTRUAL;
        }
        int ovulationStart = Math.max(periodLength + 1, cycleLength - 15);
        int ovulationEnd = Math.max(ovulationStart, cycleLength - 11);
        if (cycleDay < ovulationStart) {
            return CyclePhase.FOLLICULAR;
        }
        if (cycleDay <= ovulationEnd) {
            return CyclePhase.OVULATION;
        }
        return CyclePhase.LUTEAL;
    }

    static int averageCycle(List<LocalDate> sortedStarts, int fallback) {
        if (sortedStarts.size() < 2) {
            return fallback;
        }
        long sum = 0;
        int n = 0;
        for (int i = 1; i < sortedStarts.size(); i++) {
            long diff = ChronoUnit.DAYS.between(sortedStarts.get(i - 1), sortedStarts.get(i));
            if (diff >= MIN_CYCLE && diff <= MAX_CYCLE) {
                sum += diff;
                n++;
            }
        }
        return n == 0 ? fallback : (int) Math.round((double) sum / n);
    }

    static int averagePeriod(List<CycleRecord> rows, int fallback) {
        long sum = 0;
        int n = 0;
        for (CycleRecord r : rows) {
            if (r.getStartDate() != null && r.getEndDate() != null) {
                long d = ChronoUnit.DAYS.between(r.getStartDate(), r.getEndDate()) + 1;
                if (d >= 1 && d <= 15) {
                    sum += d;
                    n++;
                }
            }
        }
        return n == 0 ? fallback : (int) Math.round((double) sum / n);
    }

    // ---------- 内部 ----------

    private List<CycleRecord> records(Long userId) {
        return recordMapper.selectList(new LambdaQueryWrapper<CycleRecord>()
                .eq(CycleRecord::getUserId, userId)
                .isNull(CycleRecord::getDeletedAt)
                .orderByDesc(CycleRecord::getStartDate));
    }

    private WellnessProfile findProfile(Long userId) {
        return profileMapper.selectOne(new LambdaQueryWrapper<WellnessProfile>()
                .eq(WellnessProfile::getUserId, userId)
                .isNull(WellnessProfile::getDeletedAt));
    }

    private CycleDtos.RecordVO toVO(CycleRecord row) {
        return toVOPublic(row);
    }

    static CycleDtos.RecordVO toVOPublic(CycleRecord row) {
        Integer periodDays = null;
        if (row.getEndDate() != null) {
            periodDays = (int) (ChronoUnit.DAYS.between(row.getStartDate(), row.getEndDate()) + 1);
        }
        return new CycleDtos.RecordVO(row.getId(), row.getStartDate(), row.getEndDate(), periodDays,
                row.getFlow(), flowLabel(row.getFlow()), split(row.getSymptoms()), row.getMood(),
                row.getNote(), row.getCreatedAt());
    }

    private static String flowOf(String flow) {
        if (flow == null || flow.isBlank()) {
            return "NORMAL";
        }
        String v = flow.trim().toUpperCase();
        if (!List.of("LIGHT", "NORMAL", "HEAVY").contains(v)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "经量只能是 LIGHT/NORMAL/HEAVY");
        }
        return v;
    }

    static String flowLabel(String flow) {
        if (flow == null) {
            return "";
        }
        return switch (flow) {
            case "LIGHT" -> "少量";
            case "HEAVY" -> "较多";
            default -> "中等";
        };
    }

    private static String join(List<String> list) {
        return list == null || list.isEmpty() ? null : String.join(",", list);
    }

    static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static int safe(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
