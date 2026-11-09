package com.yuji.wellness.care.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.wellness.care.dto.CareDtos;
import com.yuji.wellness.care.entity.CarePlan;
import com.yuji.wellness.care.entity.CareTask;
import com.yuji.wellness.care.entity.CareTaskTemplate;
import com.yuji.wellness.care.mapper.CarePlanMapper;
import com.yuji.wellness.care.mapper.CareTaskMapper;
import com.yuji.wellness.care.mapper.CareTaskTemplateMapper;
import com.yuji.wellness.support.CareCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 月子调理：按分娩日生成 42 天任务清单。
 *
 * <p>任务来自审核过的模板（饮食/康复/作息/喂养/情绪/复查），用户只做"打卡"，
 * 平台不根据症状给出新的医疗处置。
 */
@Service
@RequiredArgsConstructor
public class CareService {

    public static final String DISCLAIMER = "月子任务是日常照护提醒，不替代医生查房与产后复查。出现发热、出血增多、伤口异常或情绪持续低落，请及时就医。";

    private final CarePlanMapper planMapper;
    private final CareTaskMapper taskMapper;
    private final CareTaskTemplateMapper templateMapper;

    @Transactional
    public CareDtos.PlanVO createOrReplace(Long userId, CareDtos.PlanReq req) {
        LocalDate today = LocalDate.now();
        if (req.deliveryDate().isAfter(today)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分娩日期不能晚于今天");
        }
        String mode = req.deliveryMode() == null ? "NATURAL" : req.deliveryMode().trim().toUpperCase();
        if (!List.of("NATURAL", "CESAREAN").contains(mode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分娩方式只能是 NATURAL 或 CESAREAN");
        }

        CarePlan plan = planMapper.selectOne(new LambdaQueryWrapper<CarePlan>()
                .eq(CarePlan::getUserId, userId)
                .eq(CarePlan::getStatus, "ACTIVE")
                .isNull(CarePlan::getDeletedAt)
                .orderByDesc(CarePlan::getId)
                .last("LIMIT 1"));
        if (plan == null) {
            plan = new CarePlan();
            plan.setUserId(userId);
            plan.setStatus("ACTIVE");
            plan.setDeliveryDate(req.deliveryDate());
            plan.setDeliveryMode(mode);
            planMapper.insert(plan);
        } else {
            plan.setDeliveryDate(req.deliveryDate());
            plan.setDeliveryMode(mode);
            planMapper.updateById(plan);
            taskMapper.delete(new LambdaQueryWrapper<CareTask>().eq(CareTask::getPlanId, plan.getId()));
        }

        List<CareTaskTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<CareTaskTemplate>()
                        .orderByAsc(CareTaskTemplate::getDayOffset)
                        .orderByAsc(CareTaskTemplate::getSortNo));
        for (CareTaskTemplate t : templates) {
            CareTask task = new CareTask();
            task.setPlanId(plan.getId());
            task.setUserId(userId);
            task.setDayIndex(t.getDayOffset());
            task.setDueDate(req.deliveryDate().plusDays(t.getDayOffset() - 1L));
            task.setCategory(t.getCategory());
            task.setTitle(t.getTitle());
            task.setDetail(t.getDetail());
            task.setStatus("PENDING");
            task.setSortNo(t.getSortNo());
            taskMapper.insert(task);
        }
        return current(userId);
    }

    public CareDtos.PlanVO current(Long userId) {
        CarePlan plan = activePlan(userId);
        if (plan == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        int dayIndex = (int) (ChronoUnit.DAYS.between(plan.getDeliveryDate(), today) + 1);
        List<CareTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<CareTask>()
                .eq(CareTask::getPlanId, plan.getId())
                .orderByAsc(CareTask::getDayIndex)
                .orderByAsc(CareTask::getSortNo));

        List<CareDtos.TaskVO> todayTasks = tasks.stream()
                .filter(t -> today.equals(t.getDueDate()))
                .map(this::toTaskVO).toList();
        if (todayTasks.isEmpty()) {
            todayTasks = tasks.stream()
                    .filter(t -> "PENDING".equals(t.getStatus()) && !t.getDueDate().isBefore(today))
                    .limit(3)
                    .map(this::toTaskVO).toList();
        }
        List<CareDtos.TaskVO> upcoming = tasks.stream()
                .filter(t -> "PENDING".equals(t.getStatus()) && t.getDueDate().isAfter(today))
                .limit(5)
                .map(this::toTaskVO).toList();

        long done = tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();
        int progress = tasks.isEmpty() ? 0 : (int) Math.round(done * 100.0 / tasks.size());

        return new CareDtos.PlanVO(plan.getId(), plan.getDeliveryDate(), plan.getDeliveryMode(),
                "CESAREAN".equals(plan.getDeliveryMode()) ? "剖宫产" : "顺产",
                dayIndex, stageLabel(dayIndex), progress, todayTasks, upcoming,
                groupWeeks(tasks), DISCLAIMER);
    }

    @Transactional
    public CareDtos.TaskVO markTask(Long userId, Long taskId, String status) {
        String s = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("PENDING", "DONE", "SKIPPED").contains(s)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的任务状态");
        }
        CareTask task = taskMapper.selectById(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        task.setStatus(s);
        taskMapper.updateById(task);
        return toTaskVO(task);
    }

    public List<CareDtos.TaskVO> todayTasks(Long userId) {
        CareDtos.PlanVO plan = current(userId);
        return plan == null ? List.of() : plan.today();
    }

    public Integer dayIndex(Long userId) {
        CarePlan plan = activePlan(userId);
        if (plan == null) {
            return null;
        }
        return (int) (ChronoUnit.DAYS.between(plan.getDeliveryDate(), LocalDate.now()) + 1);
    }

    public CarePlan activePlan(Long userId) {
        return planMapper.selectOne(new LambdaQueryWrapper<CarePlan>()
                .eq(CarePlan::getUserId, userId)
                .eq(CarePlan::getStatus, "ACTIVE")
                .isNull(CarePlan::getDeletedAt)
                .orderByDesc(CarePlan::getId)
                .last("LIMIT 1"));
    }

    static String stageLabel(int dayIndex) {
        if (dayIndex <= 0) {
            return "待产";
        }
        if (dayIndex <= 7) {
            return "产后第 1 周 · 排恶露与开奶";
        }
        if (dayIndex <= 21) {
            return "产后第 2-3 周 · 伤口与作息";
        }
        if (dayIndex <= 42) {
            return "产后第 4-6 周 · 恢复与复查";
        }
        return "已过产褥期 · 继续保持";
    }

    private List<CareDtos.WeekVO> groupWeeks(List<CareTask> tasks) {
        Map<Integer, List<CareDtos.TaskVO>> grouped = new LinkedHashMap<>();
        for (CareTask task : tasks) {
            int week = (task.getDayIndex() - 1) / 7 + 1;
            grouped.computeIfAbsent(week, k -> new ArrayList<>()).add(toTaskVO(task));
        }
        List<CareDtos.WeekVO> weeks = new ArrayList<>();
        for (Map.Entry<Integer, List<CareDtos.TaskVO>> entry : grouped.entrySet()) {
            int week = entry.getKey();
            String label = "第 " + week + " 周 · 产后第 " + ((week - 1) * 7 + 1) + "-" + Math.min(week * 7, 42) + " 天";
            weeks.add(new CareDtos.WeekVO(week, label, entry.getValue()));
        }
        return weeks;
    }

    private CareDtos.TaskVO toTaskVO(CareTask task) {
        return new CareDtos.TaskVO(task.getId(), task.getDayIndex(), task.getDueDate(),
                task.getCategory(), CareCategory.labelOf(task.getCategory()),
                task.getTitle(), task.getDetail(), task.getStatus());
    }
}
