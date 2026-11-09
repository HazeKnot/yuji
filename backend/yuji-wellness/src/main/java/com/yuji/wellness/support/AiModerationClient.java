package com.yuji.wellness.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI 社区风控客户端：把帖子/评论/打卡文本交给 ai-service 定级。
 *
 * <p>级别语义（与 /api/v1/moderate/scan 约定）：
 * ALERT 危机信号 → 待审核 + 红色工单；BLOCK 违规导流 → 待审核；
 * WATCH 消极情绪 → 正常发布 + 黄色预警；NONE 干净 → 正常发布。
 *
 * <p>AI 不可用时降级为本地关键词兜底（community 的 REVIEW_WORDS 一致），链路永不失败。
 */
@Component
public class AiModerationClient {

    /** 本地兜底词表：AI 服务失联时用，语义与 ai-service 词典保持一致 */
    static final List<String> CRISIS_FALLBACK = List.of(
            "不想活", "自杀", "自残", "想死", "伤害自己", "解脱", "消失", "跳楼", "割腕");
    static final List<String> BLOCK_FALLBACK = List.of(
            "加微信", "加qq", "代购", "私聊卖", "药流", "打胎药", "刷单", "兼职日结",
            "扫码进群", "低价代做", "回收奶粉", "出售处方");
    static final List<String> NEGATIVE_FALLBACK = List.of(
            "睡不着", "撑不住", "好累", "好想哭", "没用", "废物", "难过", "焦虑",
            "崩溃", "孤独", "害怕", "压抑", "喘不过气", "没人管我", "撑不下去了",
            "活着好累", "天天哭", "情绪崩溃", "想哭", "绝望");

    private final RestClient client;

    public AiModerationClient(@Value("${yuji.ai.base-url:http://localhost:8000}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(300);
        factory.setReadTimeout(900);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** 风控扫描结果 */
    public record ScanResult(String level, int score, List<String> tags) {
        public boolean isAlert() { return "ALERT".equals(level); }
        public boolean isBlock() { return "BLOCK".equals(level); }
        public boolean isWatch() { return "WATCH".equals(level); }
    }

    /** 扫描一段内容，永不抛异常：AI 失联时走本地词典兜底 */
    public ScanResult scan(String text, String context) {
        String content = text == null ? "" : text;
        if (content.isBlank()) {
            return new ScanResult("NONE", 0, List.of());
        }
        try {
            Map<String, Object> body = client.post()
                    .uri("/api/v1/moderate/scan")
                    .body(Map.of("text", content, "context", context == null ? "community" : context))
                    .retrieve()
                    .body(Map.class);
            if (body != null && body.get("level") != null) {
                @SuppressWarnings("unchecked")
                List<String> tags = body.get("tags") instanceof List<?> l
                        ? l.stream().map(String::valueOf).toList() : List.of();
                int score = body.get("score") instanceof Number n ? n.intValue() : 0;
                return new ScanResult(body.get("level").toString(), score, tags);
            }
        } catch (Exception ignored) {
            // AI 服务失联 → 本地词典兜底，风控不因依赖不可用而缺席
        }
        return fallbackScan(content);
    }

    private ScanResult fallbackScan(String text) {
        List<String> crisis = CRISIS_FALLBACK.stream().filter(text::contains).toList();
        if (!crisis.isEmpty()) {
            return new ScanResult("ALERT", 100, crisis.subList(0, Math.min(5, crisis.size())));
        }
        String lower = text.toLowerCase();
        List<String> blocked = BLOCK_FALLBACK.stream().filter(w -> lower.contains(w.toLowerCase())).toList();
        if (!blocked.isEmpty()) {
            return new ScanResult("BLOCK", 90, blocked.subList(0, Math.min(5, blocked.size())));
        }
        List<String> neg = NEGATIVE_FALLBACK.stream().filter(text::contains).toList();
        if (neg.size() >= 2) {
            return new ScanResult("WATCH", Math.min(60 + neg.size() * 5, 89),
                    neg.subList(0, Math.min(5, neg.size())));
        }
        return new ScanResult("NONE", 0, List.of());
    }

    /** 便捷判断：内容是否需要进待审核 */
    public boolean needsReview(ScanResult result) {
        return Optional.ofNullable(result).map(r -> r.isAlert() || r.isBlock()).orElse(false);
    }
}
