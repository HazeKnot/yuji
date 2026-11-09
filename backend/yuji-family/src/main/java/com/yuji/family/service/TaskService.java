package com.yuji.family.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.auth.entity.User;
import com.yuji.auth.mapper.UserMapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.family.dto.TaskAssignReq;
import com.yuji.family.dto.TaskTemplateVO;
import com.yuji.family.dto.TaskVO;
import com.yuji.family.entity.Family;
import com.yuji.family.entity.FamilyTask;
import com.yuji.family.entity.TaskTemplate;
import com.yuji.family.entity.TaskTemplateItem;
import com.yuji.family.mapper.FamilyTaskMapper;
import com.yuji.family.mapper.TaskTemplateItemMapper;
import com.yuji.family.mapper.TaskTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务包下发与打卡。
 *
 * <p>权限：只有妈妈能下发；家属只能推进"派给自己"的任务；越权一律 40400/40300。
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final List<String> STATUSES = List.of(
            FamilyTask.PENDING, FamilyTask.DOING, FamilyTask.DONE);

    /** 状态排序用常量 SQL（不含用户输入，无注入面） */
    private static final String ORDER_SQL =
            "ORDER BY FIELD(status,'DOING','PENDING','DONE'), due_date IS NULL, due_date";

    private final FamilyTaskMapper taskMapper;
    private final TaskTemplateMapper templateMapper;
    private final TaskTemplateItemMapper itemMapper;
    private final FamilyService familyService;
    private final UserMapper userMapper;

    /** 按模板批量生成任务，指派给某个成员 */
    @Transactional
    public List<TaskVO> assign(Long momUserId, TaskAssignReq req) {
        Family family = familyService.requireOwnedFamily(momUserId);

        TaskTemplate template = templateMapper.selectOne(new LambdaQueryWrapper<TaskTemplate>()
                .eq(TaskTemplate::getCode, req.templateCode()));
        if (template == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务包不存在");
        }
        requireAssignee(family.getId(), momUserId, req.assigneeUserId());

        List<TaskTemplateItem> items = itemMapper.selectList(new LambdaQueryWrapper<TaskTemplateItem>()
                .eq(TaskTemplateItem::getTemplateCode, template.getCode())
                .orderByAsc(TaskTemplateItem::getSortNo));
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该任务包暂无内容");
        }

        LocalDate today = LocalDate.now();
        List<FamilyTask> created = new ArrayList<>(items.size());
        for (TaskTemplateItem item : items) {
            FamilyTask t = new FamilyTask();
            t.setFamilyId(family.getId());
            t.setCreatorUserId(momUserId);
            t.setAssigneeUserId(req.assigneeUserId());
            t.setTemplateCode(template.getCode());
            t.setTitle(item.getTitle());
            t.setDetail(item.getDetail());
            t.setDueDate(today.plusDays(item.getDueOffsetDays() == null ? 0 : item.getDueOffsetDays()));
            t.setStatus(FamilyTask.PENDING);
            taskMapper.insert(t);
            created.add(t);
        }
        return toVOs(created, momUserId);
    }

    /** 妈妈看全家，家属只看派给自己的 */
    public List<TaskVO> list(Long userId) {
        FamilyService.Membership membership = familyService.requireFamily(userId);
        LambdaQueryWrapper<FamilyTask> w = new LambdaQueryWrapper<FamilyTask>()
                .eq(FamilyTask::getFamilyId, membership.family().getId())
                .last(ORDER_SQL);
        if (!membership.isOwner()) {
            w.eq(FamilyTask::getAssigneeUserId, userId);
        }
        return toVOs(taskMapper.selectList(w), userId);
    }

    @Transactional
    public TaskVO updateStatus(Long userId, Long taskId, String rawStatus) {
        String status = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务状态只能是 PENDING / DOING / DONE");
        }
        FamilyService.Membership membership = familyService.requireFamily(userId);
        FamilyTask t = taskMapper.selectById(taskId);
        if (t == null || !t.getFamilyId().equals(membership.family().getId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        if (!membership.isOwner() && !t.getAssigneeUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能更新分配给自己的任务");
        }

        FamilyTask update = new FamilyTask();
        update.setId(t.getId());
        update.setStatus(status);
        update.setCompletedAt(FamilyTask.DONE.equals(status) ? LocalDateTime.now() : null);
        taskMapper.updateById(update);
        return toVOs(List.of(taskMapper.selectById(t.getId())), userId).get(0);
    }

    /** 任务包清单：一次查模板 + 明细，避免 N+1 */
    public List<TaskTemplateVO> templates() {
        List<TaskTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<TaskTemplate>().orderByAsc(TaskTemplate::getId));
        if (templates.isEmpty()) {
            return List.of();
        }
        Map<String, List<String>> itemsByCode = itemMapper.selectList(
                        new LambdaQueryWrapper<TaskTemplateItem>()
                                .orderByAsc(TaskTemplateItem::getSortNo))
                .stream()
                .collect(Collectors.groupingBy(TaskTemplateItem::getTemplateCode,
                        Collectors.mapping(TaskTemplateItem::getTitle, Collectors.toList())));
        return templates.stream()
                .map(t -> new TaskTemplateVO(t.getCode(), t.getStage(), t.getName(), t.getDescription(),
                        itemsByCode.getOrDefault(t.getCode(), List.of())))
                .toList();
    }

    // ---------- 内部 ----------

    /** 执行人必须是本家庭成员或妈妈本人 */
    private void requireAssignee(Long familyId, Long momUserId, Long assigneeUserId) {
        if (assigneeUserId.equals(momUserId)) {
            return;
        }
        boolean inFamily = familyService.memberVOs(familyId).stream()
                .anyMatch(m -> m.userId().equals(assigneeUserId));
        if (!inFamily) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "执行人不在这个家庭里，请先邀请对方加入");
        }
    }

    private List<TaskVO> toVOs(List<FamilyTask> tasks, Long meId) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userMapper.selectBatchIds(
                        tasks.stream().map(FamilyTask::getAssigneeUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        LocalDate today = LocalDate.now();
        return tasks.stream().map(t -> {
            User assignee = users.get(t.getAssigneeUserId());
            boolean overdue = !FamilyTask.DONE.equals(t.getStatus())
                    && t.getDueDate() != null && t.getDueDate().isBefore(today);
            return new TaskVO(t.getId(), t.getTitle(), t.getDetail(), t.getDueDate(), t.getStatus(),
                    t.getAssigneeUserId(),
                    assignee == null || assignee.getNickname() == null ? "家人" : assignee.getNickname(),
                    t.getAssigneeUserId().equals(meId), t.getTemplateCode(), t.getCompletedAt(), overdue);
        }).toList();
    }
}
