package com.playdata.attendanceservice.common.util;

import com.playdata.attendanceservice.attendance.entity.WorkDayType;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek; // DayOfWeek import 추가
import java.util.HashMap;
import java.util.Map;

import static com.playdata.attendanceservice.common.constants.WorkConstants.*;

public class WorkTimeCalculator {

    /**
     * 출근 시간, 퇴근 시간, 근무 날짜를 기반으로 총 근무 시간, 정상 근무 시간, 연장 근무 시간, 심야 근무 시간을 계산합니다.
     * 점심 시간(12:00~13:00)은 근무 시간에서 제외됩니다.
     * 주말 근무는 전체 연장 근무로 간주됩니다.
     * 연장 근무는 18:00부터 22:00까지로 계산됩니다.
     * 심야 근무는 22:00부터 익일 06:00까지로 계산됩니다.
     * @param checkIn 출근 시간 (LocalDateTime)
     * @param checkOut 퇴근 시간 (LocalDateTime)
     * @param attendanceDate 근무 날짜 (LocalDate)
     * @return 각 근무 시간(totalWorkTime, normalWorkTime, overtimeWorkTime, nightWorkTime)을 포함하는 Map
     */
    public static Map<String, Duration> calculateWorkTimes(LocalDateTime checkIn, LocalDateTime checkOut, LocalDate attendanceDate) {
        Map<String, Duration> workTimes = new HashMap<>();
        Duration totalWorkTime = Duration.ZERO;
        Duration normalWorkTime = Duration.ZERO;
        Duration overtimeWorkTime = Duration.ZERO;
        Duration nightWorkTime = Duration.ZERO;

        // 출근 시간이 퇴근 시간보다 늦거나 같으면 유효하지 않은 경우
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            workTimes.put("totalWorkTime", totalWorkTime);
            workTimes.put("normalWorkTime", normalWorkTime);
            workTimes.put("overtimeWorkTime", overtimeWorkTime);
            workTimes.put("nightWorkTime", nightWorkTime);
            return workTimes;
        }

        // 근무 시간 구간
        LocalDateTime workStart = checkIn;
        LocalDateTime workEnd = checkOut;

        // 점심 시간 구간 (오늘 날짜 기준)
        LocalDateTime lunchStartDateTime = attendanceDate.atTime(LUNCH_START);
        LocalDateTime lunchEndDateTime = attendanceDate.atTime(LUNCH_END);

        // 정상 근무 시간 구간 (오늘 날짜 기준)
        LocalDateTime normalWorkStartDateTime = attendanceDate.atTime(NORMAL_WORK_START);
        LocalDateTime normalWorkEndDateTime = attendanceDate.atTime(NORMAL_WORK_END);

        // 연장 근무 시간 구간 (오늘 날짜 기준)
        LocalDateTime overtimeStartDateTime = attendanceDate.atTime(OVERTIME_START);
        LocalDateTime overtimeEndDateTime = attendanceDate.atTime(OVERTIME_END);

        // 심야 근무 시간 구간 (오늘 날짜 기준 및 익일 기준)
        LocalDateTime nightWorkStartDateTimeToday = attendanceDate.atTime(NIGHT_WORK_START);
        LocalDateTime nightWorkEndDateTimeToday = attendanceDate.plusDays(1).atStartOfDay(); // 자정
        LocalDateTime nightWorkStartDateTimeNextDay = attendanceDate.plusDays(1).atStartOfDay(); // 자정
        LocalDateTime nightWorkEndDateTimeNextDay = attendanceDate.plusDays(1).atTime(NIGHT_WORK_END);

        // 주말(토, 일)인 경우 전체를 연장 근무로 처리
        if (attendanceDate.getDayOfWeek() == DayOfWeek.SATURDAY || attendanceDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            totalWorkTime = getOverlapDuration(workStart, workEnd, workStart, workEnd); // 전체 근무 시간
            totalWorkTime = totalWorkTime.minus(getOverlapDuration(workStart, workEnd, lunchStartDateTime, lunchEndDateTime)); // 점심시간 제외
            overtimeWorkTime = totalWorkTime; // 주말은 전체가 연장 근무
        } else {
            // 평일 근무 시간 계산
            // 총 근무 시간 = 전체 근무 시간 - 점심 시간
            totalWorkTime = getOverlapDuration(workStart, workEnd, workStart, workEnd);
            totalWorkTime = totalWorkTime.minus(getOverlapDuration(workStart, workEnd, lunchStartDateTime, lunchEndDateTime));

            // 정상 근무 시간
            // 실제 근무 시간과 정상 근무 시간 구간의 겹치는 부분을 먼저 계산
            Duration grossNormalWorkTime = getOverlapDuration(workStart, workEnd, normalWorkStartDateTime, normalWorkEndDateTime);
            // 실제 근무 시간과 점심 시간 구간의 겹치는 부분을 계산
            Duration lunchOverlapWithActualWork = getOverlapDuration(workStart, workEnd, lunchStartDateTime, lunchEndDateTime);
            // 총 정상 근무 시간에서 실제 근무 시간과 겹치는 점심 시간을 제외
            normalWorkTime = grossNormalWorkTime.minus(lunchOverlapWithActualWork);

            // 연장 근무 시간 (18:00 ~ 22:00)
            overtimeWorkTime = getOverlapDuration(workStart, workEnd, overtimeStartDateTime, overtimeEndDateTime);

            // 심야 근무 시간 (22:00 ~ 익일 06:00)
            nightWorkTime = nightWorkTime.plus(getOverlapDuration(workStart, workEnd, nightWorkStartDateTimeToday, nightWorkEndDateTimeToday)); // 22:00 ~ 자정
            nightWorkTime = nightWorkTime.plus(getOverlapDuration(workStart, workEnd, nightWorkStartDateTimeNextDay, nightWorkEndDateTimeNextDay)); // 자정 ~ 익일 06:00
        }

        workTimes.put("totalWorkTime", totalWorkTime);
        workTimes.put("normalWorkTime", normalWorkTime);
        workTimes.put("overtimeWorkTime", overtimeWorkTime);
        workTimes.put("nightWorkTime", nightWorkTime);
        return workTimes;
    }

    // 두 시간 구간의 겹치는 Duration을 반환하는 헬퍼 메서드
    public static Duration getOverlapDuration(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        LocalDateTime overlapStart = max(start1, start2);
        LocalDateTime overlapEnd = min(end1, end2);

        if (overlapStart.isBefore(overlapEnd)) {
            return Duration.between(overlapStart, overlapEnd);
        }
        return Duration.ZERO;
    }

    // LocalDateTime.max 및 min 헬퍼 (Java 8 호환성을 위해 직접 구현)
    public static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    // LocalDateTime.min 헬퍼 (Java 8 호환성을 위해 직접 구현)
    public static LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    // Duration을 "HH:mm" 형식의 String으로 포맷하는 헬퍼 메서드
    public static String formatDuration(Duration duration) {
        if (duration == null) {
            return null;
        }
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * 외출/복귀 시간을 고려하여 실제 근무 시간을 계산합니다.
     *
     * @param checkInTime 출근 시간
     * @param currentTime 현재 시간 (퇴근하지 않은 경우) 또는 퇴근 시간 (퇴근한 경우)
     * @param goOutTime 외출 시간 (null일 수 있음)
     * @param returnTime 복귀 시간 (null일 수 있음)
     * @return 실제 근무 시간 (Duration)
     */
    public static Duration calculateActualWorkedDuration(LocalDateTime checkInTime, LocalDateTime currentTime,
                                                         LocalDateTime goOutTime, LocalDateTime returnTime) {
        Duration totalElapsedDuration = Duration.between(checkInTime, currentTime);
        Duration outingDuration = Duration.ZERO;

        if (goOutTime != null) {
            if (returnTime != null) {
                outingDuration = Duration.between(goOutTime, returnTime);
            } else {
                // 외출 중이고 아직 복귀하지 않은 경우, 현재 시간까지를 외출 시간으로 간주
                outingDuration = Duration.between(goOutTime, currentTime);
            }
        }

        Duration actualWorkedDuration = totalElapsedDuration.minus(outingDuration);

        // 근무 시간이 음수가 되는 경우 (예: 외출 시간이 총 경과 시간보다 긴 경우) 0으로 처리
        if (actualWorkedDuration.isNegative()) {
            actualWorkedDuration = Duration.ZERO;
        }
        return actualWorkedDuration;
    }
}
