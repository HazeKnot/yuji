package com.yuji.wellness.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 列表里的相对时间：今天 14:20 / 昨天 / 3 天前 / 8月12日。 */
public final class TimeText {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M月d日");

    private TimeText() {
    }

    public static String of(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        LocalDate day = time.toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.equals(today)) {
            return "今天 " + time.format(TIME);
        }
        if (day.equals(today.minusDays(1))) {
            return "昨天 " + time.format(TIME);
        }
        long diff = today.toEpochDay() - day.toEpochDay();
        if (diff > 1 && diff < 7) {
            return diff + " 天前";
        }
        return day.format(DATE);
    }
}
