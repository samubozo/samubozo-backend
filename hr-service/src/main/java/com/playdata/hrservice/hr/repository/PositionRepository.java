package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.hr.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {
}
