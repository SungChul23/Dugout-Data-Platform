package com.dev.dugout.domain.admin.dto;

import com.dev.dugout.domain.admin.entity.NoticeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "공지사항 응답 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeDto {

    @Schema(description = "공지사항 고유 ID", example = "1")
    private Long id;

    @Schema(description = "공지 유형 (FEATURE: 신기능, IMPROVEMENT: 개선, EVENT: 이벤트)", example = "FEATURE")
    private NoticeType type;

    @Schema(description = "버전 태그", example = "v1.2.0")
    private String version;

    @Schema(description = "공지사항 제목", example = "골든글러브 예측 모델 업데이트 안내")
    private String title;

    @Schema(description = "공지사항 본문", example = "2026시즌 골든글러브 예측 정확도가 향상되었습니다.")
    private String content;

    @Schema(description = "업데이트 날짜 (YYYY-MM-DD)", example = "2026-06-01")
    private String updateDate;
}
