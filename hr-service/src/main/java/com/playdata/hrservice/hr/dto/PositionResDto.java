package com.playdata.hrservice.hr.dto;

import com.playdata.hrservice.hr.entity.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionResDto {
    private Long positionId;
    private String positionName;
    private String hrRole;

    public PositionResDto(Position position) {
        this.positionId = position.getPositionId();
        this.positionName = position.getPositionName();
        this.hrRole = position.getHrRole();
    }
}
