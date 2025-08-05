package com.playdata.attendanceservice.attendance.service;

import com.playdata.attendanceservice.attendance.absence.entity.Absence;
import com.playdata.attendanceservice.client.dto.VacationWorkStatusRequestDto;

public interface WorkStatusService {

    void createWorkStatusForAbsence(Absence absence);

    void createWorkStatusForVacation(VacationWorkStatusRequestDto requestDto);
}
