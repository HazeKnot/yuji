package com.yuji.profile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.profile.dto.HealthRecordReq;
import com.yuji.profile.dto.HealthRecordVO;
import com.yuji.profile.dto.TrendVO;
import com.yuji.profile.entity.HealthRecord;
import com.yuji.profile.mapper.HealthRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 指标记录：录入校验 + 列表 + 趋势聚合。
 *
 * <p>校验分三层：Bean Validation（非空/长度）→ 本类（范围、血压交叉校验、时间不能是未来）→ DB（非空约束）。
 */
@Service
@RequiredArgsConstructor
public class HealthRecordService {

    /** 列表最多返回条数，避免一次拉全量 */
    private static final int MAX_LIST = 200;
    /** 趋势最长天数 */
    private static final int MAX_TREND_DAYS = 365;
    /** 允许的时钟漂移（用户手机时间可能快几分钟） */
    private static final int FUTURE_TOLERANCE_MINUTES = 5;

    private final HealthRecordMapper mapper;

    public HealthRecordVO record(Long userId, HealthRecordReq req) {
        MetricType type = MetricType.parse(req.type());
        BigDecimal value = req.value();
        if (value == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写" + type.label());
        }
        if (value.doubleValue() < type.min() || value.doubleValue() > type.max()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, rangeMessage(type));
        }

        BigDecimal second = req.value2();
        if (type.requiresSecond()) {
            if (second == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, type.label() + "需要同时填写两个数值（收缩压 + 舒张压）");
            }
            if (second.doubleValue() < type.secondMin() || second.doubleValue() > type.secondMax()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        String.format("%s（舒张压）应在 %s–%s %s 之间",
                                type.label(), trim(type.secondMin()), trim(type.secondMax()), type.unit()));
            }
            if (second.compareTo(value) >= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "收缩压应高于舒张压，请检查录入");
            }
        } else if (second != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, type.label() + "只需要一个数值");
        }

        LocalDateTime measuredAt = req.measuredAt() == null ? LocalDateTime.now() : req.measuredAt();
        if (measuredAt.isAfter(LocalDateTime.now().plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "测量时间不能晚于当前时间");
        }

        HealthRecord r = new HealthRecord();
        r.setUserId(userId);
        r.setType(type.name());
        r.setPrimaryValue(value);
        r.setSecondaryValue(second);
        r.setMeasuredAt(measuredAt);
        r.setNote(blankToNull(req.note()));
        mapper.insert(r);
        return toVO(r);
    }

    public List<HealthRecordVO> listMine(Long userId, String type, LocalDate from, LocalDate to) {
        LambdaQueryWrapper<HealthRecord> w = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId);
        if (type != null && !type.isBlank()) {
            w.eq(HealthRecord::getType, MetricType.parse(type).name());
        }
        if (from != null) {
            w.ge(HealthRecord::getMeasuredAt, from.atStartOfDay());
        }
        if (to != null) {
            w.le(HealthRecord::getMeasuredAt, to.atTime(LocalTime.MAX));
        }
        w.orderByDesc(HealthRecord::getMeasuredAt).last("LIMIT " + MAX_LIST);

        return mapper.selectList(w).stream().map(this::toVO).toList();
    }

    /**
     * 趋势：最近 days 天，按天聚合（同日多次取最后一次），返回升序点序列。
     */
    public TrendVO trend(Long userId, String typeCode, Integer days) {
        MetricType type = MetricType.parse(typeCode);
        int span = days == null ? 30 : days;
        if (span < 1 || span > MAX_TREND_DAYS) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "时间范围需在 1–" + MAX_TREND_DAYS + " 天之间");
        }
        LocalDateTime from = LocalDate.now().minusDays(span - 1L).atStartOfDay();

        List<HealthRecord> rows = mapper.selectList(new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .eq(HealthRecord::getType, type.name())
                .ge(HealthRecord::getMeasuredAt, from)
                .orderByAsc(HealthRecord::getMeasuredAt));

        // 同日多条：后写的覆盖前面的（保持首次出现的顺序，即时间升序）
        Map<LocalDate, HealthRecord> byDay = new LinkedHashMap<>();
        for (HealthRecord r : rows) {
            byDay.put(r.getMeasuredAt().toLocalDate(), r);
        }

        List<TrendVO.Point> points = new ArrayList<>();
        byDay.forEach((day, r) -> points.add(
                new TrendVO.Point(day.toString(), r.getPrimaryValue(), r.getSecondaryValue())));

        return new TrendVO(type.name(), type.label(), type.unit(), points,
                type.refMin(), type.refMax(), summary(points), TrendVO.DISCLAIMER);
    }

    public void delete(Long userId, Long id) {
        HealthRecord r = mapper.selectById(id);
        if (r == null || !r.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "记录不存在");
        }
        mapper.deleteById(id);
    }

    private TrendVO.Summary summary(List<TrendVO.Point> points) {
        if (points.isEmpty()) {
            return null;
        }
        BigDecimal min = null;
        BigDecimal max = null;
        BigDecimal sum = BigDecimal.ZERO;
        for (TrendVO.Point p : points) {
            min = min == null || p.value().compareTo(min) < 0 ? p.value() : min;
            max = max == null || p.value().compareTo(max) > 0 ? p.value() : max;
            sum = sum.add(p.value());
        }
        BigDecimal avg = sum.divide(BigDecimal.valueOf(points.size()), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return new TrendVO.Summary(points.get(points.size() - 1).value(), min, max, avg, points.size());
    }

    private HealthRecordVO toVO(HealthRecord r) {
        MetricType type = lookup(r.getType());
        String label = type == null ? r.getType() : type.label();
        String unit = type == null ? "" : type.unit();
        return new HealthRecordVO(r.getId(), r.getType(), label,
                r.getPrimaryValue(), r.getSecondaryValue(), unit, r.getMeasuredAt(), r.getNote());
    }

    /** 只读路径不做抛错：历史脏数据不应让整个列表 500 */
    private static MetricType lookup(String code) {
        for (MetricType t : MetricType.values()) {
            if (t.name().equals(code)) {
                return t;
            }
        }
        return null;
    }

    private static String rangeMessage(MetricType type) {
        return String.format("%s 应在 %s–%s %s 之间",
                type.label(), trim(type.min()), trim(type.max()), type.unit());
    }

    private static String trim(double v) {
        return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
