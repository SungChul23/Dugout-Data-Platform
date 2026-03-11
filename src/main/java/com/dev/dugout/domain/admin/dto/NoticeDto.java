package com.dev.dugout.domain.admin.dto;

import com.dev.dugout.domain.admin.entity.NoticeType;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeDto {
    private Long id;
    private NoticeType type;
    private String version;
    private String title;
    private String content;
    private String updateDate;
}