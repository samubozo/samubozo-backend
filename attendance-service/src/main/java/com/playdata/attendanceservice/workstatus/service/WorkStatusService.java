package com.playdata.attendanceservice.workstatus.service;

import com.playdata.attendanceservice.absence.entity.Absence;
import com.playdata.attendanceservice.client.dto.VacationWorkStatusRequestDto;

public interface WorkStatusService {

    void createWorkStatusForAbsence(Absence absence);

    void createWorkStatusForVacation(VacationWorkStatusRequestDto requestDto);
}
