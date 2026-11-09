package com.yuji.wellness.mental.service;

import com.yuji.wellness.support.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EpdsScorerTest {

    @Test
    void 低分是绿档() {
        EpdsScorer.Outcome outcome = EpdsScorer.score(List.of(0, 1, 0, 1, 0, 1, 0, 1, 0, 0));
        assertThat(outcome.score()).isEqualTo(4);
        assertThat(outcome.level()).isEqualTo(RiskLevel.GREEN);
        assertThat(outcome.selfHarmIdea()).isFalse();
    }

    @Test
    void 十分且第十题为零进入黄档() {
        EpdsScorer.Outcome outcome = EpdsScorer.score(List.of(2, 1, 1, 1, 1, 1, 1, 1, 1, 0));
        assertThat(outcome.score()).isEqualTo(10);
        assertThat(outcome.level()).isEqualTo(RiskLevel.YELLOW);
    }

    @Test
    void 总分达到十四且第十题为零也进入红档() {
        EpdsScorer.Outcome outcome = EpdsScorer.score(List.of(2, 2, 2, 2, 2, 2, 2, 0, 0, 0));
        assertThat(outcome.score()).isEqualTo(14);
        assertThat(outcome.level()).isEqualTo(RiskLevel.RED);
        assertThat(outcome.selfHarmIdea()).isFalse();
    }

    @Test
    void 第十题只要大于零直接红档() {
        EpdsScorer.Outcome outcome = EpdsScorer.score(List.of(0, 0, 0, 0, 0, 0, 0, 0, 0, 1));
        assertThat(outcome.score()).isEqualTo(1);
        assertThat(outcome.level()).isEqualTo(RiskLevel.RED);
        assertThat(outcome.selfHarmIdea()).isTrue();
    }

    @Test
    void 答案数量或取值不合法直接拒绝() {
        assertThatThrownBy(() -> EpdsScorer.score(List.of(0, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EpdsScorer.score(List.of(0, 1, 2, 3, 0, 1, 2, 4, 0, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
