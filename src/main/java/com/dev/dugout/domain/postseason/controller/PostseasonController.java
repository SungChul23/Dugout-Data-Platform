package com.dev.dugout.domain.postseason.controller;

import com.dev.dugout.domain.postseason.dto.PostseasonBracketResponseDto;
import com.dev.dugout.domain.postseason.dto.PostseasonTeamOverviewResponseDto;
import com.dev.dugout.domain.postseason.dto.PostseasonTopPlayersResponseDto;
import com.dev.dugout.domain.postseason.service.PostseasonService;
import com.dev.dugout.domain.team.dto.TeamRankResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Postseason", description = "가을야구(포스트시즌) 5강 브라켓 및 팀별 주요 선수 API")
@ApiResponses({
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
})
@RestController
@RequestMapping("/api/v1/postseason")
@RequiredArgsConstructor
public class PostseasonController {

    private final PostseasonService postseasonService;

    @Operation(
            summary = "가을야구 5강 브라켓 조회",
            description = """
                    현재 순위 1~5위 팀과 포스트시즌 매치업(와일드카드/준PO/PO/한국시리즈)을 반환합니다.

                    - 와일드카드(4위 vs 5위)는 양 팀이 확정된 상태로 반환됩니다.
                    - 준PO/PO/한국시리즈는 상위 시드(3위/2위/1위)만 확정, 상대는 승자가 정해지기 전까지 TBD로 표시됩니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "5강 브라켓 반환 성공")
    @GetMapping("/bracket")
    public ResponseEntity<PostseasonBracketResponseDto> getBracket() {
        return ResponseEntity.ok(postseasonService.getBracket());
    }

    @Operation(
            summary = "팀별 주요 선수 TOP3 조회",
            description = """
                    해당 팀의 주요 타자 3명과 투수 3명을 반환합니다.

                    - 타자: 규정타석(팀 경기수 × 3.1)을 채운 선수 중 타율/홈런/OPS 부문별 1명씩 (중복 시 다음 순위 선수)
                    - 투수: 선발 2명(규정이닝 충족, ERA 낮은 순) + 불펜 1명(팀 경기수 ÷ 3 이닝 이상, ERA 낮은 순)
                    """
    )
    @ApiResponse(responseCode = "200", description = "팀별 주요 선수 반환 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 팀 ID", content = @Content)
    @GetMapping("/teams/{teamId}/top-players")
    public ResponseEntity<PostseasonTopPlayersResponseDto> getTeamTopPlayers(
            @Parameter(description = "팀 고유 ID", example = "5")
            @PathVariable Long teamId) {
        return postseasonService.getTeamTopPlayers(teamId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "구단 대시보드(구단 개요) 조회",
            description = "슬로건, 우승 횟수, 홈구장, 정규시즌 주요 지표(승률/타율/ERA/홈런/홈·원정 성적)와 타율/ERA/홈런의 10개 구단 내 순위를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "구단 개요 반환 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 팀 ID", content = @Content)
    @GetMapping("/teams/{teamId}/overview")
    public ResponseEntity<PostseasonTeamOverviewResponseDto> getTeamOverview(
            @Parameter(description = "팀 고유 ID", example = "1")
            @PathVariable Long teamId) {
        return postseasonService.getTeamOverview(teamId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "팀별 순위 변동 추이 조회 (주간)",
            description = """
                    해당 팀의 정규시즌 순위 변동을 주 단위로 요약해 날짜 오름차순으로 반환합니다.

                    - 각 주(월~일)의 마지막 순위 1건만 포함하며, 마지막 항목은 항상 최신 순위입니다.
                    - 항목 형식은 팀 순위 API(/api/v1/performance/team-ranking)와 동일합니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "팀별 순위 변동 추이 반환 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 팀 ID", content = @Content)
    @GetMapping("/teams/{teamId}/rank-trend")
    public ResponseEntity<List<TeamRankResponseDto>> getTeamRankTrend(
            @Parameter(description = "팀 고유 ID", example = "1")
            @PathVariable Long teamId) {
        return postseasonService.getTeamRankTrend(teamId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
