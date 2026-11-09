package com.yuji.profile.service;

import com.yuji.common.BusinessException;
import com.yuji.profile.dto.CheckupVO;
import com.yuji.profile.entity.CheckupItem;
import com.yuji.profile.entity.CheckupTemplate;
import com.yuji.profile.mapper.CheckupItemMapper;
import com.yuji.profile.mapper.CheckupTemplateMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 产检服务单元测试（Mockito 隔离数据库）。
 * 覆盖：按模板批量生成计划日、日历排序与逾期标记、状态流转与越权/非法值拦截。
 */
@ExtendWith(MockitoExtension.class)
class CheckupServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private CheckupItemMapper itemMapper;

    @Mock
    private CheckupTemplateMapper templateMapper;

    @InjectMocks
    private CheckupService checkupService;

    // ---------- 生成产检计划 ----------

    @Test
    @DisplayName("建档生成计划：每个模板生成一项，建议日期 = 末次月经 + 孕周*7 天")
    void shouldGenerateOneItemPerTemplate() {
        LocalDate lmp = LocalDate.of(2026, 4, 20);
        when(templateMapper.selectList(any())).thenReturn(List.of(
                template("NT", "NT 超声", 12, "孕 11–13+6 周"),
                template("OGTT", "糖耐量试验", 26, "孕 24–28 周")));

        checkupService.generateForUser(USER_ID, lmp);

        ArgumentCaptor<CheckupItem> captor = ArgumentCaptor.forClass(CheckupItem.class);
        verify(itemMapper, times(2)).insert(captor.capture());
        List<CheckupItem> saved = captor.getAllValues();

        assertThat(saved).extracting(CheckupItem::getCode).containsExactly("NT", "OGTT");
        assertThat(saved).allSatisfy(i -> {
            assertThat(i.getUserId()).isEqualTo(USER_ID);
            assertThat(i.getStatus()).isEqualTo(CheckupVO.PLANNED);
            assertThat(i.getDueDate()).isEqualTo(lmp.plusWeeks(i.getSuggestedWeek()));
        });
        assertThat(saved.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 7, 13));  // 4/20 + 12 周
        assertThat(saved.get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 10, 19)); // 4/20 + 26 周
    }

    @Test
    @DisplayName("模板表为空时不生成任何条目（不抛异常）")
    void shouldGenerateNothingWhenNoTemplate() {
        when(templateMapper.selectList(any())).thenReturn(List.of());

        checkupService.generateForUser(USER_ID, LocalDate.of(2026, 4, 20));

        verify(itemMapper, never()).insert(any(CheckupItem.class));
    }

    // ---------- 日历列表 ----------

    @Test
    @DisplayName("日历按建议孕周升序返回，且逾期标记只针对未完成的项")
    void shouldSortByWeekAndMarkOverdue() {
        LocalDate today = LocalDate.now();
        CheckupItem week26 = item(2L, USER_ID, "OGTT", 26, today.minusDays(3), CheckupVO.PLANNED);
        CheckupItem week06 = item(1L, USER_ID, "FIRST_VISIT", 6, today.minusDays(30), CheckupVO.DONE);
        CheckupItem week12 = item(3L, USER_ID, "NT", 12, today.plusDays(5), CheckupVO.PLANNED);

        when(itemMapper.selectList(any())).thenReturn(List.of(week26, week06, week12));
        when(templateMapper.selectOne(any())).thenReturn(template("X", "任意", 12, "窗口文案"));

        List<CheckupVO> list = checkupService.listMine(USER_ID);

        assertThat(list).extracting(CheckupVO::suggestedWeek).containsExactly(6, 12, 26);
        assertThat(list.get(0).overdue()).as("已完成项不算逾期").isFalse();
        assertThat(list.get(1).overdue()).as("建议日期在未来").isFalse();
        assertThat(list.get(2).overdue()).as("待办且已过期").isTrue();
        assertThat(list).allSatisfy(vo -> assertThat(vo.windowText()).isEqualTo("窗口文案"));
    }

    // ---------- 状态更新 ----------

    @Test
    @DisplayName("标记完成：状态置为 DONE 并落库，返回的 VO 不显示逾期")
    void shouldMarkItemDone() {
        LocalDate today = LocalDate.now();
        CheckupItem planned = item(5L, USER_ID, "NT", 12, today.minusDays(10), CheckupVO.PLANNED);
        when(itemMapper.selectById(5L)).thenReturn(planned);
        when(templateMapper.selectOne(any())).thenReturn(template("NT", "NT 超声", 12, "孕 11–13+6 周"));

        CheckupVO vo = checkupService.updateStatus(USER_ID, 5L, CheckupVO.DONE);

        assertThat(vo.status()).isEqualTo(CheckupVO.DONE);
        assertThat(vo.overdue()).isFalse();
        assertThat(planned.getStatus()).isEqualTo(CheckupVO.DONE);
        verify(itemMapper).updateById(planned);
    }

    @Test
    @DisplayName("非法状态值（含 null、任意字符串）返回 40000，且不查库")
    void shouldRejectIllegalStatus() {
        assertThatThrownBy(() -> checkupService.updateStatus(USER_ID, 1L, "WHATEVER"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("非法的产检状态")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40000);

        assertThatThrownBy(() -> checkupService.updateStatus(USER_ID, 1L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40000);

        assertThatThrownBy(() -> checkupService.updateStatus(USER_ID, 1L, ""))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40000);

        verify(itemMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("产检项不存在：返回 40400")
    void shouldRejectMissingItem() {
        when(itemMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> checkupService.updateStatus(USER_ID, 99L, CheckupVO.DONE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("产检项不存在")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40400);
    }

    @Test
    @DisplayName("越权：不能修改别人的产检项（返回 40400 且不落库）")
    void shouldRejectOtherUsersItem() {
        CheckupItem others = item(6L, 8L, "NT", 12, LocalDate.now(), CheckupVO.PLANNED);
        when(itemMapper.selectById(6L)).thenReturn(others);

        assertThatThrownBy(() -> checkupService.updateStatus(USER_ID, 6L, CheckupVO.DONE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40400);

        verify(itemMapper, never()).updateById(any(CheckupItem.class));
        assertThat(others.getStatus()).as("他人数据不应被改写").isEqualTo(CheckupVO.PLANNED);
    }

    // ---------- 测试夹具 ----------

    private static CheckupTemplate template(String code, String name, int week, String window) {
        CheckupTemplate t = new CheckupTemplate();
        t.setCode(code);
        t.setName(name);
        t.setSuggestedWeek(week);
        t.setWindowText(window);
        return t;
    }

    private static CheckupItem item(Long id, Long userId, String code, int week, LocalDate due, String status) {
        CheckupItem i = new CheckupItem();
        i.setId(id);
        i.setUserId(userId);
        i.setCode(code);
        i.setName(code + " 名称");
        i.setSuggestedWeek(week);
        i.setDueDate(due);
        i.setStatus(status);
        return i;
    }
}
