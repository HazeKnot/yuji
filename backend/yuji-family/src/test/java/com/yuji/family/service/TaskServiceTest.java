package com.yuji.family.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.auth.entity.User;
import com.yuji.auth.mapper.UserMapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.family.TableInfoInitializer;
import com.yuji.family.dto.MemberVO;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 任务包下发与打卡：重点测"谁能改哪条任务"，而不是 CRUD 本身。
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long MOM = 1L;
    private static final Long FAMILY_ID = 10L;
    private static final Long MEMBER_USER = 9L;

    @Mock
    private FamilyTaskMapper taskMapper;
    @Mock
    private TaskTemplateMapper templateMapper;
    @Mock
    private TaskTemplateItemMapper itemMapper;
    @Mock
    private FamilyService familyService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private TaskService service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoInitializer.init(FamilyTask.class, TaskTemplate.class, TaskTemplateItem.class);
    }

    @Test
    @DisplayName("下发任务包：按明细逐条生成，截止日 = 今天 + 偏移天数")
    void shouldAssignFromTemplate() {
        when(familyService.requireOwnedFamily(MOM)).thenReturn(family());
        when(templateMapper.selectOne(any())).thenReturn(template());
        when(familyService.memberVOs(FAMILY_ID)).thenReturn(List.of(memberVO(MEMBER_USER)));
        when(itemMapper.selectList(any())).thenReturn(List.of(
                item("整理待产包", 7), item("确认路线", 5)));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(MEMBER_USER, "阿哲")));

        List<TaskVO> created = service.assign(MOM, new TaskAssignReq("LATE_PREGNANCY", MEMBER_USER));

        assertThat(created).hasSize(2);
        ArgumentCaptor<FamilyTask> saved = ArgumentCaptor.forClass(FamilyTask.class);
        verify(taskMapper, times(2)).insert(saved.capture());
        assertThat(saved.getAllValues())
                .allSatisfy(t -> {
                    assertThat(t.getStatus()).isEqualTo(FamilyTask.PENDING);
                    assertThat(t.getCreatorUserId()).isEqualTo(MOM);
                    assertThat(t.getAssigneeUserId()).isEqualTo(MEMBER_USER);
                    assertThat(t.getTemplateCode()).isEqualTo("LATE_PREGNANCY");
                });
        assertThat(saved.getAllValues().get(0).getDueDate()).isEqualTo(LocalDate.now().plusDays(7));
        assertThat(saved.getAllValues().get(1).getDueDate()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(created.get(0).mine()).isFalse();
    }

    @Test
    @DisplayName("任务包不存在：40000")
    void shouldRejectUnknownTemplate() {
        when(familyService.requireOwnedFamily(MOM)).thenReturn(family());
        when(templateMapper.selectOne(any())).thenReturn(null);

        assertCode(() -> service.assign(MOM, new TaskAssignReq("NOPE", MEMBER_USER)),
                ErrorCode.PARAM_ERROR);
        verify(taskMapper, never()).insert(any(FamilyTask.class));
    }

    @Test
    @DisplayName("执行人不在家庭里：40000（防把任务派给任意用户）")
    void shouldRejectForeignAssignee() {
        when(familyService.requireOwnedFamily(MOM)).thenReturn(family());
        when(templateMapper.selectOne(any())).thenReturn(template());
        when(familyService.memberVOs(FAMILY_ID)).thenReturn(List.of(memberVO(8L)));

        assertCode(() -> service.assign(MOM, new TaskAssignReq("LATE_PREGNANCY", MEMBER_USER)),
                ErrorCode.PARAM_ERROR);
        verify(taskMapper, never()).insert(any(FamilyTask.class));
    }

    @Test
    @DisplayName("家属只能改派给自己的任务：改别人的 40300")
    void shouldRejectUpdatingOthersTask() {
        when(familyService.requireFamily(MEMBER_USER))
                .thenReturn(new FamilyService.Membership(family(), FamilyRole.MEMBER, null));
        FamilyTask other = task(5L, 8L, FamilyTask.PENDING);
        when(taskMapper.selectById(5L)).thenReturn(other);

        assertCode(() -> service.updateStatus(MEMBER_USER, 5L, "DONE"), ErrorCode.FORBIDDEN);
        verify(taskMapper, never()).updateById(any(FamilyTask.class));
    }

    @Test
    @DisplayName("任务不属于本家庭：40400（连存在性都不透露）")
    void shouldRejectForeignTask() {
        FamilyTask foreign = task(5L, MEMBER_USER, FamilyTask.PENDING);
        foreign.setFamilyId(999L);
        when(familyService.requireFamily(MEMBER_USER))
                .thenReturn(new FamilyService.Membership(family(), FamilyRole.MEMBER, null));
        when(taskMapper.selectById(5L)).thenReturn(foreign);

        assertCode(() -> service.updateStatus(MEMBER_USER, 5L, "DONE"), ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("非法状态：40000（不做静默兜底）")
    void shouldRejectIllegalStatus() {
        assertCode(() -> service.updateStatus(MEMBER_USER, 5L, "WHATEVER"), ErrorCode.PARAM_ERROR);
        verify(taskMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("打卡完成：写入 completedAt；妈妈可改任意任务")
    void shouldCompleteTask() {
        FamilyTask pending = task(5L, MEMBER_USER, FamilyTask.PENDING);
        FamilyTask done = task(5L, MEMBER_USER, FamilyTask.DONE);
        done.setCompletedAt(LocalDateTime.now());
        when(familyService.requireFamily(MOM))
                .thenReturn(new FamilyService.Membership(family(), FamilyRole.OWNER, null));
        when(taskMapper.selectById(5L)).thenReturn(pending, done);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(MEMBER_USER, "阿哲")));

        TaskVO vo = service.updateStatus(MOM, 5L, "done");

        ArgumentCaptor<FamilyTask> updated = ArgumentCaptor.forClass(FamilyTask.class);
        verify(taskMapper).updateById(updated.capture());
        assertThat(updated.getValue().getStatus()).isEqualTo(FamilyTask.DONE);
        assertThat(updated.getValue().getCompletedAt()).isNotNull();
        assertThat(vo.status()).isEqualTo(FamilyTask.DONE);
    }

    @Test
    @DisplayName("列表：家属的查询条件带 assignee_user_id，妈妈不带（看全家）")
    void shouldScopeListByRole() {
        when(familyService.requireFamily(MEMBER_USER))
                .thenReturn(new FamilyService.Membership(family(), FamilyRole.MEMBER, null));
        when(taskMapper.selectList(any())).thenReturn(List.of());

        service.list(MEMBER_USER);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<FamilyTask>> memberWrapper =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(taskMapper).selectList(memberWrapper.capture());
        assertThat(memberWrapper.getValue().getSqlSegment()).contains("assignee_user_id");

        when(familyService.requireFamily(MOM))
                .thenReturn(new FamilyService.Membership(family(), FamilyRole.OWNER, null));
        service.list(MOM);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<FamilyTask>> allWrappers =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(taskMapper, times(2)).selectList(allWrappers.capture());
        assertThat(allWrappers.getAllValues().get(1).getSqlSegment())
                .doesNotContain("assignee_user_id");
    }

    @Test
    @DisplayName("模板清单：明细按模板聚合，缺失明细不报错")
    void shouldAggregateTemplates() {
        when(templateMapper.selectList(any())).thenReturn(List.of(template(), template2()));
        when(itemMapper.selectList(any())).thenReturn(List.of(item("整理待产包", 7)));

        List<TaskTemplateVO> list = service.templates();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).items()).containsExactly("整理待产包");
        assertThat(list.get(1).items()).isEmpty();
    }

    @Test
    @DisplayName("妈妈可以把任务派给自己（不查成员表）")
    void shouldAllowAssignToSelf() {
        when(familyService.requireOwnedFamily(MOM)).thenReturn(family());
        when(templateMapper.selectOne(any())).thenReturn(template());
        when(itemMapper.selectList(any())).thenReturn(List.of(item("整理待产包", 3)));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(MOM, "小满")));

        List<TaskVO> created = service.assign(MOM, new TaskAssignReq("LATE_PREGNANCY", MOM));

        assertThat(created).hasSize(1);
        assertThat(created.get(0).mine()).isTrue();
        verify(familyService, never()).memberVOs(any());
    }

    @Test
    @DisplayName("模板表为空：返回空清单而不是 null")
    void shouldReturnEmptyTemplates() {
        when(templateMapper.selectList(any())).thenReturn(List.of());

        assertThat(service.templates()).isEmpty();
        verify(itemMapper, never()).selectList(any());
    }

    // ---------- 夹具 ----------

    private void assertCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(expected.getCode());
    }

    private static Family family() {
        Family f = new Family();
        f.setId(FAMILY_ID);
        f.setOwnerUserId(MOM);
        return f;
    }

    private static TaskTemplate template() {
        TaskTemplate t = new TaskTemplate();
        t.setCode("LATE_PREGNANCY");
        t.setStage("孕晚期");
        t.setName("待产准备");
        return t;
    }

    private static TaskTemplate template2() {
        TaskTemplate t = new TaskTemplate();
        t.setCode("CHECKUP_COMPANY");
        t.setStage("产检");
        t.setName("产检陪同");
        return t;
    }

    private static TaskTemplateItem item(String title, int offset) {
        TaskTemplateItem i = new TaskTemplateItem();
        i.setTemplateCode("LATE_PREGNANCY");
        i.setTitle(title);
        i.setDueOffsetDays(offset);
        return i;
    }

    private static FamilyTask task(Long id, Long assignee, String status) {
        FamilyTask t = new FamilyTask();
        t.setId(id);
        t.setFamilyId(FAMILY_ID);
        t.setAssigneeUserId(assignee);
        t.setTitle("整理待产包");
        t.setStatus(status);
        t.setDueDate(LocalDate.now().plusDays(3));
        return t;
    }

    private static MemberVO memberVO(Long userId) {
        return new MemberVO(1L, userId, "阿哲", "配偶", List.of(), LocalDateTime.now());
    }

    private static User user(Long id, String nickname) {
        User u = new User();
        u.setId(id);
        u.setNickname(nickname);
        return u;
    }
}
