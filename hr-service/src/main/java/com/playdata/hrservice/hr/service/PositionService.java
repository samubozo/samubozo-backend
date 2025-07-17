package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.PositionResDto;

import java.util.List;

public interface PositionService {
    List<PositionResDto> getAllPositions();
}
