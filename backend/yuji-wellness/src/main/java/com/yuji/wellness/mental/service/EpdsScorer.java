package com.yuji.wellness.mental.service;

import com.yuji.wellness.support.RiskLevel;

import java.util.List;

/**
 * EPDS 计分：10 题，每题 0-3 分，总分 0-30。
 *
 * <p>分级口径（爱丁堡产后抑郁量表常用界值）：
 * 0-9 绿档、10-12 黄档、≥13 红档；第 10 题（自伤念头）只要 >0 直接红档。
 * 这是筛查分层，不是诊断。
 */
public final class EpdsScorer {

    private EpdsScorer() {
    }

    public record Outcome(int score, RiskLevel level, boolean selfHarmIdea) {
    }

    public static Outcome score(List<Integer> answers) {
        if (answers == null || answers.size() != 10) {
            throw new IllegalArgumentException("EPDS 需要 10 个答案");
        }
        int total = 0;
        for (Integer answer : answers) {
            if (answer == null || answer < 0 || answer > 3) {
                throw new IllegalArgumentException("EPDS 每题只能是 0-3 分");
            }
            total += answer;
        }
        boolean selfHarm = answers.get(9) != null && answers.get(9) > 0;
        RiskLevel level;
        if (selfHarm || total >= 13) {
            level = RiskLevel.RED;
        } else if (total >= 10) {
            level = RiskLevel.YELLOW;
        } else {
            level = RiskLevel.GREEN;
        }
        return new Outcome(total, level, selfHarm);
    }
}
