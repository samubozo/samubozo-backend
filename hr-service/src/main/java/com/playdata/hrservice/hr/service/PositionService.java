package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.PositionResDto;
import com.playdata.hrservice.hr.entity.Position;
import com.playdata.hrservice.hr.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;

    public List<PositionResDto> getAllPositions() {
        List<Position> positions = positionRepository.findAll();
        return positions.stream()
                .map(PositionResDto::new)
                .collect(Collectors.toList());
    }
}
