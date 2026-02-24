package com.dev.dugout.domain.user.dto;

import lombok.Data;

@Data
public class DashboardRequestDto {
    private Long playerId;
    private int slotNumber;
}
