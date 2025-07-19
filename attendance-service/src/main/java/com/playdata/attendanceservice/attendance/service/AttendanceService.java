package com.playdata.attendanceservice.attendance.service;

import com.playdata.attendanceservice.attendance.dto.AttendanceResDto;
import com.playdata.attendanceservice.attendance.dto.PersonalAttendanceStatsDto;
import com.playdata.attendanceservice.attendance.dto.WorkTimeDto;
import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.common.dto.CommonResDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceService {

    Attendance recordCheckIn(Long userId, String ipAddress);

    Attendance recordCheckIn(Long userId, String ipAddress, LocalDateTime checkInDateTime);

    Attendance recordCheckOut(Long userId);

    Attendance recordCheckOut(Long userId, LocalDateTime checkOutDateTime);

    List<Attendance> getMonthlyAttendances(Long userId, int year, int month);

    Attendance recordGoOut(Long userId);



    Attendance recordReturn(Long userId);

    WorkTimeDto getRemainingWorkTime(Long userId);

    Optional<AttendanceResDto> getTodayAttendance(Long userId);

    PersonalAttendanceStatsDto getPersonalAttendanceStats(Long userId, int year, int month);

    CommonResDto<com.playdata.attendanceservice.client.dto.VacationBalanceResDto> getPersonalVacationBalance(Long userId);

    void requestHalfDayVacation(Long userId, com.playdata.attendanceservice.client.dto.VacationRequestDto requestDto);

    CommonResDto<List<com.playdata.attendanceservice.client.dto.Vacation>> getPersonalMonthlyHalfDayVacations(Long userId, int year, int month);

    String getLateThreshold();
}