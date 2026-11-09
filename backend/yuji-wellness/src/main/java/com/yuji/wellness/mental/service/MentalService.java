package com.yuji.wellness.mental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.wellness.mental.dto.MentalDtos;
import com.yuji.wellness.mental.entity.EmotionCheckin;
import com.yuji.wellness.mental.entity.RiskTicket;
import com.yuji.wellness.mental.entity.ScaleResult;
import com.yuji.wellness.mental.mapper.EmotionCheckinMapper;
import com.yuji.wellness.mental.mapper.RiskTicketMapper;
import com.yuji.wellness.mental.mapper.ScaleResultMapper;
import com.yuji.wellness.partner.dto.PartnerDtos;
import com.yuji.wellness.support.AiModerationClient;
import com.yuji.wellness.support.RiskAlertService;
import com.yuji.wellness.support.RiskLevel;
import com.yuji.wellness.support.TimeText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 产后心理康复：情绪打卡 + EPDS 筛查 + 分级工单。
 *
 * <p>黄档进人工陪伴，红档转介；绝不输出"你得了抑郁症"这类诊断表述。
 */
@Service
@RequiredArgsConstructor
public class MentalService {

    public static final String DISCLAIMER = "量表是筛查工具，不是诊断。结果受最近一周状态影响，不能替代医生的临床访谈。";

    public static final String HOTLINE = "全国心理援助热线 12356；如有立即危险，请拨打 120 或前往急诊。";

    private static final List<String> TICKET_STATUS = List.of("OPEN", "FOLLOWING", "CLOSED");

    private final EmotionCheckinMapper checkinMapper;
    private final ScaleResultMapper scaleMapper;
    private final RiskTicketMapper ticketMapper;
    private final AiModerationClient moderation;
    private final RiskAlertService riskAlert;

    @Transactional
    public MentalDtos.CheckinVO checkin(Long userId, MentalDtos.CheckinReq req) {
        EmotionCheckin row = new EmotionCheckin();
        row.setUserId(userId);
        row.setMood(req.mood());
        row.setTags(join(req.tags()));
        row.setNote(req.note());
        checkinMapper.insert(row);

        // AI 风控：打卡备注里的消极表达 → 消极人选预警（危机表达直接红档）
        if (req.note() != null && !req.note().isBlank()) {
            AiModerationClient.ScanResult scan = moderation.scan(req.note(), "checkin");
            if (scan.isAlert()) {
                riskAlert.alert(userId, "EMOTION", "RED",
                        "AI 风控：情绪打卡出现危机表达「" + String.join("、", scan.tags()) + "」，请立即人工联系");
            } else if (scan.isWatch()) {
                riskAlert.alert(userId, "EMOTION", "YELLOW",
                        "AI 风控：情绪打卡出现消极表达「" + String.join("、", scan.tags()) + "」，建议安排一次陪伴访谈");
            }
        }
        return toCheckinVO(row);
    }

    public MentalDtos.OverviewVO overview(Long userId) {
        List<MentalDtos.CheckinVO> checkins = checkinMapper.selectList(
                        new LambdaQueryWrapper<EmotionCheckin>()
                                .eq(EmotionCheckin::getUserId, userId)
                                .ge(EmotionCheckin::getCreatedAt, LocalDateTime.now().minusDays(14))
                                .orderByDesc(EmotionCheckin::getCreatedAt))
                .stream().map(this::toCheckinVO).toList();
        List<MentalDtos.ScaleVO> scales = scaleMapper.selectList(
                        new LambdaQueryWrapper<ScaleResult>()
                                .eq(ScaleResult::getUserId, userId)
                                .orderByDesc(ScaleResult::getCreatedAt)
                                .last("LIMIT 10"))
                .stream().map(this::toScaleVO).toList();
        List<MentalDtos.TicketVO> tickets = ticketMapper.selectList(
                        new LambdaQueryWrapper<RiskTicket>()
                                .eq(RiskTicket::getUserId, userId)
                                .orderByDesc(RiskTicket::getCreatedAt)
                                .last("LIMIT 10"))
                .stream().map(this::toTicketVO).toList();
        return new MentalDtos.OverviewVO(checkins, scales, tickets, tip(checkins), DISCLAIMER);
    }

    @Transactional
    public MentalDtos.EpdsResultVO submitEpds(Long userId, MentalDtos.EpdsReq req) {
        EpdsScorer.Outcome outcome;
        try {
            outcome = EpdsScorer.score(req.answers());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, e.getMessage());
        }

        ScaleResult result = new ScaleResult();
        result.setUserId(userId);
        result.setScaleType("EPDS");
        result.setScore(outcome.score());
        result.setLevel(outcome.level().name());
        result.setAnswers(req.answers().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        scaleMapper.insert(result);

        Long ticketId = null;
        if (outcome.level() != RiskLevel.GREEN) {
            RiskTicket ticket = new RiskTicket();
            ticket.setUserId(userId);
            ticket.setSource("EPDS");
            ticket.setLevel(outcome.level().name());
            ticket.setSummary("EPDS 总分 " + outcome.score() + " 分"
                    + (outcome.selfHarmIdea() ? "，第 10 题提示存在自伤想法" : ""));
            ticket.setStatus("OPEN");
            ticketMapper.insert(ticket);
            ticketId = ticket.getId();
        }

        return new MentalDtos.EpdsResultVO(outcome.score(), outcome.level().name(),
                outcome.level().label(), interpretation(outcome.level()),
                suggestions(outcome.level()), outcome.selfHarmIdea() ? HOTLINE : null,
                ticketId, DISCLAIMER);
    }

    public List<MentalDtos.TicketVO> myTickets(Long userId) {
        return ticketMapper.selectList(new LambdaQueryWrapper<RiskTicket>()
                        .eq(RiskTicket::getUserId, userId)
                        .orderByDesc(RiskTicket::getCreatedAt))
                .stream().map(this::toTicketVO).toList();
    }

    // ---------- 机构端 ----------

    public List<MentalDtos.TicketVO> adminTickets(String status, String level) {
        return adminTickets(status, level, null);
    }

    /**
     * 机构端工单查询：支持限定会员范围与告警来源（COMMUNITY/EMOTION = AI 风控告警）。
     */
    public List<MentalDtos.TicketVO> adminTickets(String status, String level, java.util.Collection<Long> userIds) {
        LambdaQueryWrapper<RiskTicket> wrapper = new LambdaQueryWrapper<RiskTicket>()
                .orderByDesc(RiskTicket::getCreatedAt);
        if (userIds != null) {
            if (userIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(RiskTicket::getUserId, userIds);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(RiskTicket::getStatus, status.trim().toUpperCase());
        }
        if (level != null && !level.isBlank()) {
            wrapper.eq(RiskTicket::getLevel, level.trim().toUpperCase());
        }
        return ticketMapper.selectList(wrapper).stream().map(this::toTicketVO).toList();
    }

    @Transactional
    public MentalDtos.TicketVO updateTicket(Long id, PartnerDtos.TicketStatusReq req) {
        String status = req.status().trim().toUpperCase();
        if (!TICKET_STATUS.contains(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的工单状态");
        }
        RiskTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        ticket.setStatus(status);
        if (req.handlerNote() != null) {
            ticket.setHandlerNote(req.handlerNote());
        }
        if ("CLOSED".equals(status)) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        ticketMapper.updateById(ticket);
        return toTicketVO(ticket);
    }

    // ---------- 内部 ----------

    static String interpretation(RiskLevel level) {
        return switch (level) {
            case GREEN -> "本次筛查没有提示明显的高风险信号。情绪有起伏是正常的，继续保留记录和休息的习惯即可。";
            case YELLOW -> "本次筛查提示可能存在一定程度的情绪困扰。建议安排一次专业访谈，或让信任的人陪你聊聊最近的压力。";
            case RED -> "本次筛查提示风险较高。建议尽快联系产科、精神心理科或当地妇幼保健机构做进一步评估。";
        };
    }

    static List<String> suggestions(RiskLevel level) {
        return switch (level) {
            case GREEN -> List.of(
                    "每天留 10 分钟只属于自己，不处理任何人的需求",
                    "把夜里的一次喂奶或家务明确交给家人",
                    "连续记录情绪两周，观察睡眠与情绪的关联");
            case YELLOW -> List.of(
                    "这周内约一次产科或心理门诊，聊聊最近的睡眠和情绪",
                    "把「我现在最需要的帮助」写成一句话，发给伴侣或家人",
                    "减少不必要的访客与家务，优先保证连续 4 小时的睡眠");
            case RED -> List.of(
                    "尽快到精神心理科或产科做一次面对面评估",
                    "让家人知道你的状态，请他们陪同就诊",
                    "暂时不要独自承担夜间照护，把宝宝交给可信任的人",
                    "如出现伤害自己的想法，立即联系热线或前往急诊");
        };
    }

    private static String tip(List<MentalDtos.CheckinVO> checkins) {
        if (checkins.size() < 2) {
            return "记录不需要连续打卡，想起来的时候写一句就好。";
        }
        int low = (int) checkins.stream().filter(c -> c.mood() <= 2).count();
        if (low >= 3) {
            return "最近两周有 " + low + " 次情绪偏低。可以试试 EPDS 筛查，或约一次专业访谈——这不是矫情。";
        }
        return "最近两周记录了 " + checkins.size() + " 次，继续留意睡眠和精力变化。";
    }

    private MentalDtos.CheckinVO toCheckinVO(EmotionCheckin row) {
        return new MentalDtos.CheckinVO(row.getId(), row.getMood(), moodLabel(row.getMood()),
                split(row.getTags()), row.getNote(), TimeText.of(row.getCreatedAt()), row.getCreatedAt());
    }

    private MentalDtos.ScaleVO toScaleVO(ScaleResult row) {
        RiskLevel level = RiskLevel.of(row.getLevel());
        return new MentalDtos.ScaleVO(row.getId(), row.getScaleType(), row.getScore(),
                level.name(), level.label(), row.getCreatedAt());
    }

    private MentalDtos.TicketVO toTicketVO(RiskTicket row) {
        RiskLevel level = RiskLevel.of(row.getLevel());
        return new MentalDtos.TicketVO(row.getId(), row.getSource(), level.name(), level.label(),
                row.getSummary(), row.getStatus(), statusLabel(row.getStatus()),
                row.getHandlerNote(), TimeText.of(row.getCreatedAt()), row.getCreatedAt());
    }

    static String moodLabel(Integer mood) {
        if (mood == null) {
            return "";
        }
        return switch (mood) {
            case 1 -> "很糟";
            case 2 -> "有点低";
            case 3 -> "还行";
            case 4 -> "不错";
            default -> "很好";
        };
    }

    public static String statusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "OPEN" -> "待跟进";
            case "FOLLOWING" -> "跟进中";
            case "CLOSED" -> "已关闭";
            default -> status;
        };
    }

    private static String join(List<String> list) {
        return list == null || list.isEmpty() ? null : String.join(",", list);
    }

    private static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
