package com.yuji.profile.service;

import com.yuji.common.BusinessException;
import com.yuji.profile.dto.HealthRecordReq;
import com.yuji.profile.dto.HealthRecordVO;
import com.yuji.profile.dto.TrendVO;
import com.yuji.profile.entity.HealthRecord;
import com.yuji.profile.mapper.HealthRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 指标记录服务测试：范围校验、血压交叉校验、趋势按天聚合、越权删除。
 */
@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private HealthRecordMapper mapper;

    @InjectMocks
    private HealthRecordService service;

    // ---------- 录入 ----------

    @Test
    @DisplayName("体重录入：入库值与单位回填正确")
    void shouldRecordWeight() {
        HealthRecordVO vo = service.record(USER_ID, req("WEIGHT", "60.5", null, null, "  晨起空腹 "));

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(mapper).insert(captor.capture());
        HealthRecord saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getType()).isEqualTo("WEIGHT");
        assertThat(saved.getPrimaryValue()).isEqualByComparingTo("60.5");
        assertThat(saved.getSecondaryValue()).isNull();
        assertThat(saved.getNote()).as("备注两端空白应清理").isEqualTo("晨起空腹");
        assertThat(saved.getMeasuredAt()).isNotNull();

        assertThat(vo.unit()).isEqualTo("kg");
        assertThat(vo.typeLabel()).isEqualTo("体重");
    }

    @Test
    @DisplayName("体重超出 20–250kg：40000 且不落库")
    void shouldRejectWeightOutOfRange() {
        assertThatThrownBy(() -> service.record(USER_ID, req("WEIGHT", "5", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("体重 应在 20–250 kg 之间")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40000);

        verify(mapper, never()).insert(any(HealthRecord.class));
    }

    @Test
    @DisplayName("未知类型：40000（不能 NPE，也不能 500）")
    void shouldRejectUnknownType() {
        assertThatThrownBy(() -> service.record(USER_ID, req("HEIGHT", "170", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的指标类型");

        assertThatThrownBy(() -> service.record(USER_ID, req(null, "170", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请指定指标类型");
    }

    @Test
    @DisplayName("血压：收缩压必须高于舒张压")
    void shouldRejectInvertedBloodPressure() {
        assertThatThrownBy(() -> service.record(USER_ID, req("BLOOD_PRESSURE", "100", "120", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("收缩压应高于舒张压，请检查录入");

        assertThatThrownBy(() -> service.record(USER_ID, req("BLOOD_PRESSURE", "120", "120", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("收缩压应高于舒张压，请检查录入");
    }

    @Test
    @DisplayName("血压：缺舒张压或舒张压超范围都拒绝")
    void shouldValidateSecondValue() {
        assertThatThrownBy(() -> service.record(USER_ID, req("BLOOD_PRESSURE", "120", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("需要同时填写两个数值");

        assertThatThrownBy(() -> service.record(USER_ID, req("BLOOD_PRESSURE", "150", "20", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("舒张压");

        // 合法：120/80 应通过
        service.record(USER_ID, req("BLOOD_PRESSURE", "120", "80", null, null));
        verify(mapper).insert(any(HealthRecord.class));
    }

    @Test
    @DisplayName("非血压指标携带第二个数值：拒绝（避免前端串数据）")
    void shouldRejectUnexpectedSecondValue() {
        assertThatThrownBy(() -> service.record(USER_ID, req("BLOOD_GLUCOSE", "5.2", "3.1", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("血糖只需要一个数值");
    }

    @Test
    @DisplayName("血糖范围 1–30 mmol/L")
    void shouldValidateGlucoseRange() {
        assertThatThrownBy(() -> service.record(USER_ID, req("BLOOD_GLUCOSE", "0.5", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mmol/L");

        service.record(USER_ID, req("BLOOD_GLUCOSE", "5.2", null, null, null));
    }

    @Test
    @DisplayName("测量时间不能是未来（允许 5 分钟时钟漂移）")
    void shouldRejectFutureMeasuredAt() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);

        assertThatThrownBy(() -> service.record(USER_ID, req("WEIGHT", "60", null, tomorrow, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("测量时间不能晚于当前时间");

        // 快 2 分钟的手机时间应被容忍
        service.record(USER_ID, req("WEIGHT", "60", null, LocalDateTime.now().plusMinutes(2), null));
    }

    // ---------- 趋势 ----------

    @Test
    @DisplayName("趋势：同日多条取最后一次，按日期升序，并给出摘要与参考区间")
    void shouldAggregateTrendByDay() {
        LocalDate today = LocalDate.now();
        when(mapper.selectList(any())).thenReturn(List.of(
                row("WEIGHT", "60.0", null, today.minusDays(2).atTime(8, 0)),
                row("WEIGHT", "61.0", null, today.minusDays(1).atTime(8, 0)),
                row("WEIGHT", "61.5", null, today.minusDays(1).atTime(20, 0)),
                row("WEIGHT", "62.0", null, today.atTime(7, 30))));

        TrendVO trend = service.trend(USER_ID, "WEIGHT", 7);

        assertThat(trend.points()).hasSize(3);
        assertThat(trend.points()).extracting(TrendVO.Point::date)
                .containsExactly(today.minusDays(2).toString(), today.minusDays(1).toString(), today.toString());
        assertThat(trend.points().get(1).value())
                .as("同一天两条取后写的 61.5")
                .isEqualByComparingTo("61.5");

        assertThat(trend.summary().count()).isEqualTo(3);
        assertThat(trend.summary().latest()).isEqualByComparingTo("62.0");
        assertThat(trend.summary().min()).isEqualByComparingTo("60.0");
        assertThat(trend.summary().max()).isEqualByComparingTo("62.0");
        assertThat(trend.summary().avg()).isEqualByComparingTo("61.17");
        assertThat(trend.disclaimer()).contains("不构成医学判读");
    }

    @Test
    @DisplayName("趋势：无数据时返回空点集与 null 摘要（前端显示占位）")
    void shouldReturnEmptyTrend() {
        when(mapper.selectList(any())).thenReturn(List.of());

        TrendVO trend = service.trend(USER_ID, "BLOOD_PRESSURE", 30);

        assertThat(trend.points()).isEmpty();
        assertThat(trend.summary()).isNull();
        assertThat(trend.refMin()).isEqualTo(90d);
        assertThat(trend.refMax()).isEqualTo(140d);
        assertThat(trend.unit()).isEqualTo("mmHg");
    }

    @Test
    @DisplayName("趋势天数越界：40000（防止一次拉全量）")
    void shouldRejectIllegalDays() {
        assertThatThrownBy(() -> service.trend(USER_ID, "WEIGHT", 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1–365");
        assertThatThrownBy(() -> service.trend(USER_ID, "WEIGHT", 366))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1–365");
    }

    // ---------- 列表 / 删除 ----------

    @Test
    @DisplayName("列表：历史脏数据（未知类型）不抛错，label 回落为原始码")
    void shouldTolerateLegacyUnknownType() {
        when(mapper.selectList(any())).thenReturn(List.of(
                row("WEIGHT", "60", null, LocalDateTime.now().minusDays(1)),
                row("HEIGHT", "170", null, LocalDateTime.now().minusDays(2))));

        List<HealthRecordVO> list = service.listMine(USER_ID, null, null, null);

        assertThat(list).hasSize(2);
        assertThat(list.get(1).typeLabel()).isEqualTo("HEIGHT");
        assertThat(list.get(1).unit()).isEmpty();
    }

    @Test
    @DisplayName("删除：越权（他人的记录）返回 40400 且不删")
    void shouldRejectDeletingOthersRecord() {
        HealthRecord others = row("WEIGHT", "60", null, LocalDateTime.now());
        others.setId(9L);
        others.setUserId(8L);
        when(mapper.selectById(9L)).thenReturn(others);

        assertThatThrownBy(() -> service.delete(USER_ID, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("记录不存在");

        verify(mapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("删除：自己的记录可删")
    void shouldDeleteOwnRecord() {
        HealthRecord mine = row("WEIGHT", "60", null, LocalDateTime.now());
        mine.setId(9L);
        mine.setUserId(USER_ID);
        when(mapper.selectById(9L)).thenReturn(mine);

        service.delete(USER_ID, 9L);

        verify(mapper).deleteById(9L);
    }

    // ---------- 夹具 ----------

    private static HealthRecordReq req(String type, String value, String value2,
                                       LocalDateTime measuredAt, String note) {
        return new HealthRecordReq(type,
                value == null ? null : new BigDecimal(value),
                value2 == null ? null : new BigDecimal(value2),
                measuredAt, note);
    }

    private static HealthRecord row(String type, String value, String value2, LocalDateTime at) {
        HealthRecord r = new HealthRecord();
        r.setUserId(USER_ID);
        r.setType(type);
        r.setPrimaryValue(new BigDecimal(value));
        r.setSecondaryValue(value2 == null ? null : new BigDecimal(value2));
        r.setMeasuredAt(at);
        return r;
    }
}
