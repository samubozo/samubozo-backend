package com.playdata.attendanceservice.common.constants;

import java.time.LocalTime;

public final class WorkConstants {

    private WorkConstants() {
        // 인스턴스화 방지
    }

    public static final int STANDARD_WORK_HOURS = 8; // 추가
    public static final LocalTime NORMAL_WORK_START = LocalTime.of(9, 0);
    public static final LocalTime NORMAL_WORK_END = LocalTime.of(18, 0);
    public static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    public static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    public static final LocalTime OVERTIME_START = LocalTime.of(18, 0);
    public static final LocalTime OVERTIME_END = LocalTime.of(22, 0);
    public static final LocalTime NIGHT_WORK_START = LocalTime.of(22, 0);
    public static final LocalTime NIGHT_WORK_END = LocalTime.of(6, 0);
}
