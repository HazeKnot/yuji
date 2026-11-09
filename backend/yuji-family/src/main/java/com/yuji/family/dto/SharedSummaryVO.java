package com.yuji.family.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 家属可见的状态摘要（默认可见，无需授权）。
 *
 * <p>刻意不包含任何情绪/量表字段：{@link #privacyNote} 直接告知家属"哪些看不到"，
 * 把隐私边界做成产品语言，而不是藏在文档里。
 */
public record SharedSummaryVO(
        String ownerNickname,
        Integer weeks,
        Integer days,
        String stage,
        LocalDate edd,
        Long daysToEdd,
        int checkupTotal,
        int checkupDone,
        String nextCheckupName,
        LocalDate nextCheckupDate,
        int taskTotal,
        int taskDone,
        List<String> visibleFields,
        String privacyNote
) {

    public static final String PRIVACY_NOTE =
            "情绪记录与量表得分不对家属开放：这是她可以不说的地方，除非她逐项授权。";
}
