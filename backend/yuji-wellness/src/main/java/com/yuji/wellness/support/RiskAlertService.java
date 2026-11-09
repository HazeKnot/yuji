package com.yuji.wellness.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.wellness.mental.entity.RiskTicket;
import com.yuji.wellness.mental.mapper.RiskTicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * AI 风控告警落单：社区/打卡里的消极信号统一在这里生成风险工单。
 *
 * <p>去重规则：同一用户同一来源，7 天内已有未关闭的工单时不再重复建单，
 * 避免用户连续发帖/打卡刷出一串告警。危机信号（RED）不去重，永远建单。
 */
@Component
@RequiredArgsConstructor
public class RiskAlertService {

    private final RiskTicketMapper ticketMapper;

    /**
     * @return 工单 id；被去重规则拦下时返回 null
     */
    public Long alert(Long userId, String source, String level, String summary) {
        boolean red = "RED".equals(level);
        if (!red) {
            long openSameSource = ticketMapper.selectCount(new LambdaQueryWrapper<RiskTicket>()
                    .eq(RiskTicket::getUserId, userId)
                    .eq(RiskTicket::getSource, source)
                    .ne(RiskTicket::getStatus, "CLOSED")
                    .ge(RiskTicket::getCreatedAt, LocalDateTime.now().minusDays(7)));
            if (openSameSource > 0) {
                return null;
            }
        }
        RiskTicket ticket = new RiskTicket();
        ticket.setUserId(userId);
        ticket.setSource(source);
        ticket.setLevel(level);
        ticket.setSummary(summary);
        ticket.setStatus("OPEN");
        ticketMapper.insert(ticket);
        return ticket.getId();
    }
}
