package com.dev.dugout.domain.team.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//팀 정보(id,팀 이름, 지역, 경기장)
//가장 기본이 되는 Master Data)
public class Team {

    @Id
    private long id;
    private String name;
    private String city;
    private String stadiumName; //"현재 주력 홈구장" - 추후 경기장이 수정 될 수도 있으니
    private String slogan; // 팀 슬로건

}
