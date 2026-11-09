package com.yuji.wellness.profile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.auth.entity.User;
import com.yuji.auth.mapper.UserMapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.profile.dto.ProfileVO;
import com.yuji.profile.service.ProfileService;
import com.yuji.wellness.care.dto.CareDtos;
import com.yuji.wellness.care.service.CareService;
import com.yuji.wellness.cycle.dto.CycleDtos;
import com.yuji.wellness.cycle.service.CycleService;
import com.yuji.wellness.profile.dto.WellnessDtos;
import com.yuji.wellness.profile.entity.WellnessProfile;
import com.yuji.wellness.profile.mapper.WellnessProfileMapper;
import com.yuji.wellness.support.LifeStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * 全周期档案与首页聚合。
 *
 * <p>关键取舍：不强制所有女性用户填写孕产信息。孕产档案 profile 与 wellness_profile 并存，
 * 首页以"至少有一份档案"为可用条件，避免把经期用户挡在门外。
 */
@Service
@RequiredArgsConstructor
public class WellnessService {

    private final WellnessProfileMapper profileMapper;
    private final ProfileService pregnancyProfileService;
    private final UserMapper userMapper;
    private final CycleService cycleService;
    private final CareService careService;

    /**
     * 支持部分更新：只传宝宝生日时不会把已有的阶段/周期信息清空。
     */
    @Transactional
    public WellnessDtos.ProfileVO saveProfile(Long userId, WellnessDtos.ProfileReq req) {
        LifeStage stage = req.lifeStage() == null || req.lifeStage().isBlank()
                ? null : LifeStage.of(req.lifeStage());
        if (req.nickname() != null && !req.nickname().isBlank()) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setNickname(req.nickname().trim());
                userMapper.updateById(user);
            }
        }
        LocalDate today = LocalDate.now();
        if (req.lastPeriodDate() != null && req.lastPeriodDate().isAfter(today)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "末次月经日期不能晚于今天");
        }
        if (req.babyBirthDate() != null && req.babyBirthDate().isAfter(today)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "宝宝生日不能晚于今天");
        }
        if (req.deliveryDate() != null && req.deliveryDate().isAfter(today)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分娩日期不能晚于今天");
        }
        String mode = req.deliveryMode();
        if (mode != null && !mode.isBlank()) {
            mode = mode.trim().toUpperCase();
            if (!List.of("NATURAL", "CESAREAN").contains(mode)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "分娩方式只能是 NATURAL 或 CESAREAN");
            }
        }

        WellnessProfile profile = find(userId);
        if (profile == null) {
            profile = new WellnessProfile();
            profile.setUserId(userId);
            profile.setLifeStage(stage == null ? LifeStage.CYCLE.name() : stage.name());
            profileMapper.insert(profile);
        } else if (stage != null) {
            profile.setLifeStage(stage.name());
        }
        if (req.lastPeriodDate() != null) {
            profile.setLastPeriodDate(req.lastPeriodDate());
        }
        profile.setCycleLength(req.cycleLength() != null ? req.cycleLength() : safe(profile.getCycleLength(), 28));
        profile.setPeriodLength(req.periodLength() != null ? req.periodLength() : safe(profile.getPeriodLength(), 5));
        if (req.babyBirthDate() != null) {
            profile.setBabyBirthDate(req.babyBirthDate());
        }
        if (req.deliveryDate() != null) {
            profile.setDeliveryDate(req.deliveryDate());
        }
        if (mode != null) {
            profile.setDeliveryMode(mode);
        }
        if (req.concerns() != null) {
            profile.setConcerns(String.join(",", req.concerns()));
        }
        profileMapper.updateById(profile);
        return toVO(profile);
    }

    public WellnessProfile find(Long userId) {
        return profileMapper.selectOne(new LambdaQueryWrapper<WellnessProfile>()
                .eq(WellnessProfile::getUserId, userId)
                .isNull(WellnessProfile::getDeletedAt));
    }

    public WellnessDtos.OverviewVO overview(Long userId) {
        WellnessProfile profile = find(userId);
        ProfileVO pregnancy = pregnancyOf(userId);
        User user = userMapper.selectById(userId);

        boolean hasProfile = profile != null || pregnancy != null;
        LifeStage stage;
        if (profile != null) {
            stage = LifeStage.of(profile.getLifeStage());
        } else if (pregnancy != null) {
            stage = Boolean.TRUE.equals(pregnancy.postpartum()) ? LifeStage.POSTPARTUM : LifeStage.PREGNANT;
        } else {
            stage = LifeStage.CYCLE;
        }

        CycleDtos.SummaryVO cycle = hasProfile ? cycleService.summary(userId) : null;
        CareDtos.PlanVO care = hasProfile ? careService.current(userId) : null;

        Integer babyMonths = null;
        LocalDate babyBirth = profile == null ? null : profile.getBabyBirthDate();
        if (babyBirth != null) {
            babyMonths = (int) ChronoUnit.MONTHS.between(babyBirth, LocalDate.now());
        }

        String nickname = user == null || user.getNickname() == null || user.getNickname().isBlank()
                ? "姐妹" : user.getNickname();

        return new WellnessDtos.OverviewVO(
                hasProfile,
                nickname,
                stage.name(),
                stage.label(),
                cycle == null ? null : cycle.cycleDay(),
                cycle == null ? null : cycle.phase(),
                cycle == null ? null : cycle.phaseLabel(),
                cycle == null ? null : cycle.phaseHint(),
                cycle == null ? null : cycle.nextPeriodDate(),
                cycle == null ? null : cycle.daysToNextPeriod(),
                cycle == null ? null : cycle.avgCycleLength(),
                cycle == null ? null : cycle.avgPeriodLength(),
                pregnancy != null && !Boolean.TRUE.equals(pregnancy.postpartum()),
                pregnancy == null ? null : pregnancy.weeks(),
                pregnancy == null ? null : pregnancy.days(),
                pregnancy == null ? null : pregnancy.daysToEdd(),
                pregnancy == null ? null : pregnancy.stage(),
                pregnancy != null && Boolean.TRUE.equals(pregnancy.postpartum()),
                babyMonths,
                care == null ? null : care.dayIndex(),
                care == null ? null : care.stageLabel(),
                care == null ? List.of() : care.today(),
                todayTip(stage, cycle, pregnancy, care, babyMonths)
        );
    }

    public Integer babyAgeMonths(Long userId) {
        WellnessProfile profile = find(userId);
        if (profile == null || profile.getBabyBirthDate() == null) {
            return null;
        }
        return (int) ChronoUnit.MONTHS.between(profile.getBabyBirthDate(), LocalDate.now());
    }

    // ---------- 内部 ----------

    private ProfileVO pregnancyOf(Long userId) {
        try {
            return pregnancyProfileService.getMine(userId);
        } catch (BusinessException e) {
            // 40401 未建档是正常分支；其它业务异常也不应拖垮首页
            return null;
        }
    }

    private WellnessDtos.ProfileVO toVO(WellnessProfile p) {
        LifeStage stage = LifeStage.of(p.getLifeStage());
        return new WellnessDtos.ProfileVO(stage.name(), stage.label(), p.getLastPeriodDate(),
                p.getCycleLength(), p.getPeriodLength(), p.getBabyBirthDate(), p.getDeliveryDate(),
                p.getDeliveryMode(), split(p.getConcerns()));
    }

    private static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static int safe(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static String todayTip(LifeStage stage, CycleDtos.SummaryVO cycle, ProfileVO pregnancy,
                                   CareDtos.PlanVO care, Integer babyMonths) {
        if (care != null && care.dayIndex() != null && care.dayIndex() >= 1 && care.dayIndex() <= 42) {
            return "产后第 " + care.dayIndex() + " 天：" + (care.today().isEmpty()
                    ? "今天没有硬性任务，先照顾好自己的睡眠。" : care.today().get(0).title());
        }
        if (pregnancy != null && !Boolean.TRUE.equals(pregnancy.postpartum())) {
            return "孕 " + pregnancy.weeks() + " 周 + " + pregnancy.days() + " 天，今天只做三件事就够了。";
        }
        if (cycle != null && cycle.cycleDay() != null) {
            return "周期第 " + cycle.cycleDay() + " 天 · " + cycle.phaseLabel()
                    + "：" + cycle.phaseHint();
        }
        if (babyMonths != null) {
            return "宝宝 " + babyMonths + " 月龄，今天可以看一节 5 分钟的育儿课。";
        }
        return switch (stage) {
            case PREPARE -> "备孕阶段先把叶酸、作息和体检安排好，不必把每一天都变成任务。";
            case MENOPAUSE -> "潮热和失眠可以被评估和帮助，先记录一周，再去门诊聊。";
            default -> "还没有周期记录。记下这次月经第一天，就能开始预测与饮食规划。";
        };
    }
}
