package com.dev.dugout.domain.postseason.controller;

import com.dev.dugout.domain.postseason.dto.PostseasonBracketResponseDto;
import com.dev.dugout.domain.postseason.dto.PostseasonTopPlayersResponseDto;
import com.dev.dugout.domain.postseason.service.PostseasonService;
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
            description = "해당 팀의 타자 TOP3(OPS 기준)와 투수 TOP3(ERA 기준)를 반환합니다."
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
}
