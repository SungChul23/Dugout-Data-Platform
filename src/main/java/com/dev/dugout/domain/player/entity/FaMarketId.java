package com.dev.dugout.domain.player.entity;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode // 복합키 생성
public class FaMarketId implements Serializable {
    private String pcode;
    private Integer year;
}