package com.yuji.wellness.consult.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.wellness.consult.dto.ConsultDtos;
import com.yuji.wellness.consult.entity.ConsultMessage;
import com.yuji.wellness.consult.entity.ConsultSession;
import com.yuji.wellness.consult.entity.ConsultTopic;
import com.yuji.wellness.consult.mapper.ConsultMessageMapper;
import com.yuji.wellness.consult.mapper.ConsultSessionMapper;
import com.yuji.wellness.consult.mapper.ConsultTopicMapper;
import com.yuji.wellness.mental.entity.RiskTicket;
import com.yuji.wellness.mental.mapper.RiskTicketMapper;
import com.yuji.wellness.partner.dto.PartnerDtos;
import com.yuji.wellness.partner.service.PartnerService;
import com.yuji.wellness.support.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 私密健康咨询：知识卡驱动的分诊式问答。
 *
 * <p>刻意不做的事：不下诊断、不推荐处方药、不承诺疗效。
 * 输出的价值在于"这件事该不该看医生、看哪个科、去之前准备什么"。
 */
@Service
@RequiredArgsConstructor
public class ConsultService {

    public static final String DISCLAIMER = "以下为健康教育与就诊准备信息，不构成诊断或用药建议。如症状持续或加重，请到正规医疗机构面诊。";

    /** 这些词一旦出现，优先提示就医，而不是继续在家观察。 */
    private static final List<String> URGENT_WORDS = List.of(
            "大出血", "出血不止", "晕倒", "昏迷", "高热", "39度", "剧烈腹痛", "疼得直不起腰",
            "胸痛", "呼吸困难", "绝经后出血", "同房后出血", "硬块变大");

    private final ConsultTopicMapper topicMapper;
    private final ConsultSessionMapper sessionMapper;
    private final ConsultMessageMapper messageMapper;
    private final RiskTicketMapper ticketMapper;
    private final PartnerService partnerService;
    private final AiConsultClient aiClient;

    public List<ConsultDtos.TopicVO> topics() {
        return topicMapper.selectList(new LambdaQueryWrapper<ConsultTopic>()
                        .orderByAsc(ConsultTopic::getSortNo))
                .stream().map(this::toTopicVO).toList();
    }

    public List<ConsultDtos.SessionVO> mySessions(Long userId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<ConsultSession>()
                        .eq(ConsultSession::getUserId, userId)
                        .isNull(ConsultSession::getDeletedAt)
                        .orderByDesc(ConsultSession::getUpdatedAt))
                .stream().map(this::toSessionVO).toList();
    }

    @Transactional
    public ConsultDtos.SessionDetailVO create(Long userId, ConsultDtos.SessionReq req) {
        ConsultTopic topic = requireTopic(req.topicCode());
        ConsultSession session = new ConsultSession();
        session.setUserId(userId);
        session.setTopicCode(topic.getCode());
        session.setTitle(topic.getTitle());
        session.setRiskLevel(RiskLevel.GREEN.name());
        session.setStatus("OPEN");
        sessionMapper.insert(session);

        insertMessage(session.getId(), "ASSISTANT", opening(topic));
        if (req.question() != null && !req.question().isBlank()) {
            insertMessage(session.getId(), "USER", req.question().trim());
            reply(userId, session, topic, req.question().trim());
        }
        return detail(userId, session.getId());
    }

    public ConsultDtos.SessionDetailVO detail(Long userId, Long id) {
        ConsultSession session = requireSession(userId, id);
        List<ConsultDtos.MessageVO> messages = messageMapper.selectList(
                        new LambdaQueryWrapper<ConsultMessage>()
                                .eq(ConsultMessage::getSessionId, id)
                                .orderByAsc(ConsultMessage::getId))
                .stream().map(this::toMessageVO).toList();
        ConsultTopic topic = topicMapper.selectOne(new LambdaQueryWrapper<ConsultTopic>()
                .eq(ConsultTopic::getCode, session.getTopicCode()));
        return new ConsultDtos.SessionDetailVO(toSessionVO(session), messages,
                topic == null ? null : toTopicDetail(topic), DISCLAIMER);
    }

    /** 返回 [用户消息, 助手回复]，前端直接追加到当前会话。 */
    @Transactional
    public List<ConsultDtos.MessageVO> send(Long userId, Long id, String content) {
        ConsultSession session = requireSession(userId, id);
        if ("CLOSED".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "这个会话已经结束，可以重新发起一次咨询");
        }
        ConsultTopic topic = topicMapper.selectOne(new LambdaQueryWrapper<ConsultTopic>()
                .eq(ConsultTopic::getCode, session.getTopicCode()));
        ConsultMessage userMessage = insertMessage(id, "USER", content.trim());
        ConsultMessage replyMessage = reply(userId, session, topic, content.trim());
        return List.of(toMessageVO(userMessage), toMessageVO(replyMessage));
    }

    @Transactional
    public ConsultDtos.ReferralVO referral(Long userId, Long id) {
        ConsultSession session = requireSession(userId, id);
        if ("REFERRED".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "已经提交过转介，机构会在工作时间内联系你");
        }
        RiskLevel level = RiskLevel.of(session.getRiskLevel());
        if (level == RiskLevel.GREEN) {
            level = RiskLevel.YELLOW;
        }
        RiskTicket ticket = new RiskTicket();
        ticket.setUserId(userId);
        ticket.setSource("CONSULT");
        ticket.setLevel(level.name());
        ticket.setSummary("私密咨询转介：" + session.getTitle());
        ticket.setStatus("OPEN");
        ticketMapper.insert(ticket);

        session.setStatus("REFERRED");
        session.setRiskLevel(level.name());
        sessionMapper.updateById(session);
        insertMessage(id, "SYSTEM", "已为你提交转介工单，合作机构会在工作时间内联系。紧急情况请直接拨打 120 或前往急诊。");

        List<PartnerDtos.OrgBriefVO> partners = partnerService.activeBrief(3);
        String message = level == RiskLevel.RED
                ? "你描述的情况建议尽快到线下就诊。可以先联系附近的妇幼保健院或妇科急诊。"
                : "已提交转介，你也可以先从下方合作机构里选择一家预约。";
        return new ConsultDtos.ReferralVO(ticket.getId(), level.name(), level.label(), message, partners);
    }

    // ---------- 回复生成 ----------

    private ConsultMessage reply(Long userId, ConsultSession session, ConsultTopic sessionTopic, String content) {
        ConsultTopic matched = bestMatch(content, sessionTopic);
        boolean urgent = containsAny(content, URGENT_WORDS);
        String structured = matched == null
                ? genericReply(content)
                : topicReply(matched, urgent);

        String aiPrefix = aiClient.reply(content, matched == null ? session.getTitle() : matched.getTitle())
                .map(text -> text + "\n\n")
                .orElse("");

        String finalText = aiPrefix + structured;
        RiskLevel level = urgent ? RiskLevel.RED : RiskLevel.of(session.getRiskLevel());
        if (urgent) {
            session.setRiskLevel(RiskLevel.RED.name());
            sessionMapper.updateById(session);
        }
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        return insertMessage(session.getId(), "ASSISTANT", finalText);
    }

    private String topicReply(ConsultTopic topic, boolean urgent) {
        StringBuilder sb = new StringBuilder();
        if (urgent) {
            sb.append("你描述的情况里有需要尽快就医的信号，先别继续在家观察。\n\n");
        }
        sb.append("这类困扰很常见，不需要觉得羞耻。\n\n");
        sb.append("【可以先这样理解】\n").append(topic.getSummary()).append("\n\n");
        sb.append("【可以先做】\n").append(topic.getAdvice()).append("\n\n");
        sb.append("【需要尽快就医的信号】\n").append(topic.getRedFlags()).append("\n\n");
        sb.append("【日常护理】\n").append(topic.getCareTips()).append("\n\n");
        sb.append("【建议就诊科室】\n").append(topic.getDepartment()).append("\n\n");
        sb.append("【内容来源】\n").append(topic.getSource()).append("\n\n");
        sb.append("如果愿意，可以再补充：这种情况持续多久了？有没有伴随发热、明显疼痛或异常出血？");
        return sb.toString();
    }

    private String genericReply(String content) {
        return "我在。先谢谢你愿意说出来，身体的问题不需要羞耻。\n\n"
                + "为了不误导你，我不能只凭一句话判断原因。可以补充三点：\n"
                + "1. 症状持续多久了，是第一次还是反复出现；\n"
                + "2. 有没有伴随发热、明显疼痛、异常出血或异味；\n"
                + "3. 最近有没有换洗液、用药、作息或避孕方式的变化。\n\n"
                + "你也可以直接选一个更接近的主题，我会给你对应的「可以先做什么、什么时候该就医、挂哪个科」。\n\n"
                + "如果出现大出血、晕厥、剧烈腹痛、胸痛或呼吸困难，请直接就医。";
    }

    private ConsultTopic bestMatch(String content, ConsultTopic sessionTopic) {
        List<ConsultTopic> all = topicMapper.selectList(new LambdaQueryWrapper<ConsultTopic>()
                .orderByAsc(ConsultTopic::getSortNo));
        ConsultTopic best = null;
        int bestScore = 0;
        for (ConsultTopic t : all) {
            int score = 0;
            for (String kw : splitKeywords(t.getKeywords())) {
                if (!kw.isBlank() && content.contains(kw)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = t;
            }
        }
        return best != null ? best : sessionTopic;
    }

    private static boolean containsAny(String text, List<String> words) {
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitKeywords(String keywords) {
        if (keywords == null) {
            return List.of();
        }
        return java.util.Arrays.stream(keywords.split("、")).map(String::trim).toList();
    }

    private String opening(ConsultTopic topic) {
        return "你好，我是育己的健康助手。这里不需要真名，聊天也不会出现在社区里。\n\n"
                + "你选的是「" + topic.getTitle() + "」。\n" + topic.getSummary() + "\n\n"
                + "你可以直接描述现在的困扰，比如持续多久、有没有伴随发热/疼痛/异常出血。"
                + "我会告诉你哪些情况可以先观察、哪些需要尽快就医、该挂哪个科 —— 但我不能替代医生做诊断。";
    }

    // ---------- 内部 ----------

    private ConsultSession requireSession(Long userId, Long id) {
        ConsultSession session = id == null ? null : sessionMapper.selectById(id);
        if (session == null || session.getDeletedAt() != null || !userId.equals(session.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "咨询会话不存在");
        }
        return session;
    }

    private ConsultTopic requireTopic(String code) {
        ConsultTopic topic = code == null ? null : topicMapper.selectOne(
                new LambdaQueryWrapper<ConsultTopic>().eq(ConsultTopic::getCode, code.trim().toUpperCase()));
        if (topic == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的咨询主题");
        }
        return topic;
    }

    private ConsultMessage insertMessage(Long sessionId, String role, String content) {
        ConsultMessage message = new ConsultMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        messageMapper.insert(message);
        return message;
    }

    private ConsultDtos.TopicVO toTopicVO(ConsultTopic t) {
        return new ConsultDtos.TopicVO(t.getId(), t.getCode(), t.getTitle(), t.getCategory(),
                categoryLabel(t.getCategory()), t.getSummary(), t.getDepartment());
    }

    private ConsultDtos.TopicDetailVO toTopicDetail(ConsultTopic t) {
        return new ConsultDtos.TopicDetailVO(t.getId(), t.getCode(), t.getTitle(), t.getCategory(),
                categoryLabel(t.getCategory()), t.getSummary(), t.getAdvice(), t.getRedFlags(),
                t.getCareTips(), t.getDepartment(), t.getSource());
    }

    private ConsultDtos.SessionVO toSessionVO(ConsultSession s) {
        RiskLevel level = RiskLevel.of(s.getRiskLevel());
        return new ConsultDtos.SessionVO(s.getId(), s.getTitle(), s.getTopicCode(),
                level.name(), level.label(), s.getStatus(), s.getCreatedAt());
    }

    private ConsultDtos.MessageVO toMessageVO(ConsultMessage m) {
        return new ConsultDtos.MessageVO(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt());
    }

    static String categoryLabel(String category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case "CYCLE" -> "月经与周期";
            case "INTIMATE" -> "私密困扰";
            case "BREAST" -> "乳房健康";
            case "PELVIC" -> "盆底与产后";
            case "SCREEN" -> "筛查报告";
            case "MOOD" -> "情绪与睡眠";
            default -> category;
        };
    }

    /** 会话列表按最近更新排序，保证最近会话在第一条。 */
    public void touch(ConsultSession session) {
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    /** 供测试与小工具使用：保证消息按 id 稳定排序。 */
    static List<ConsultMessage> sorted(List<ConsultMessage> messages) {
        List<ConsultMessage> copy = new ArrayList<>(messages);
        copy.sort(Comparator.comparing(ConsultMessage::getId));
        return copy;
    }
}
