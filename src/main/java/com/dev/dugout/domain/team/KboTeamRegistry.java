package com.dev.dugout.domain.team;

import java.util.Arrays;
import java.util.Optional;

/**
 * KBO 10개 팀의 ID, 약칭, 풀네임 매핑을 중앙 관리하는 Enum.
 * FaMarketService, RecommendService 등에서 분산되어 있던 팀 매핑 로직을 통합한다.
 */
public enum KboTeamRegistry {

    SAMSUNG(1L, "삼성", "삼성 라이온즈"),
    DOOSAN(2L, "두산", "두산 베어스"),
    LG(3L, "LG", "LG 트윈스"),
    LOTTE(4L, "롯데", "롯데 자이언츠"),
    KIA(5L, "KIA", "KIA 타이거즈"),
    HANWHA(6L, "한화", "한화 이글스"),
    SSG(7L, "SSG", "SSG 랜더스"),
    KIWOOM(8L, "키움", "키움 히어로즈"),
    NC(9L, "NC", "NC 다이노스"),
    KT(10L, "kt", "kt wiz");

    private final Long id;
    private final String shortName;
    private final String fullName;

    KboTeamRegistry(Long id, String shortName, String fullName) {
        this.id = id;
        this.shortName = shortName;
        this.fullName = fullName;
    }

    public Long getId() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }

    public String getFullName() {
        return fullName;
    }

    /**
     * 약칭(또는 부분 문자열 포함)으로 팀 ID를 조회한다.
     * 대소문자를 무시하고 contains 매칭을 수행한다.
     *
     * @param name 팀 이름 (약칭 또는 풀네임 일부)
     * @return 매칭된 팀 ID, 없으면 Optional.empty()
     */
    public static Optional<Long> findIdByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();

        return Arrays.stream(values())
                .filter(t -> name.contains(t.shortName) ||
                        name.equalsIgnoreCase(t.shortName) ||
                        name.toLowerCase().contains(t.shortName.toLowerCase()))
                .map(KboTeamRegistry::getId)
                .findFirst();
    }

    /**
     * 팀 ID(문자열)로 풀네임을 조회한다.
     * RecommendService의 FULL_TEAM_NAMES 상수를 대체한다.
     *
     * @param id 팀 ID (문자열 형태, 예: "1", "10")
     * @return 매칭된 팀 풀네임, 없으면 Optional.empty()
     */
    public static Optional<String> findFullNameById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();

        try {
            Long teamId = Long.parseLong(id);
            return findFullNameById(teamId);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * 팀 ID(Long)로 풀네임을 조회한다.
     *
     * @param id 팀 ID
     * @return 매칭된 팀 풀네임, 없으면 Optional.empty()
     */
    public static Optional<String> findFullNameById(Long id) {
        if (id == null) return Optional.empty();

        return Arrays.stream(values())
                .filter(t -> t.id.equals(id))
                .map(KboTeamRegistry::getFullName)
                .findFirst();
    }
}
