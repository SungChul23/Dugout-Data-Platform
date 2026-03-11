package com.dev.dugout.domain.admin.service;


import com.dev.dugout.domain.admin.dto.NoticeDto;
import com.dev.dugout.domain.admin.entity.Notice;
import com.dev.dugout.domain.admin.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    // 공지사항 가져오기
    public List<NoticeDto> findAllNotices() {
        return noticeRepository.findAllByOrderByIdDesc().stream()
                .map(notice -> NoticeDto.builder() // Entity -> DTO 변환 로직
                        .id(notice.getId())
                        .type(notice.getType())
                        .version(notice.getVersion())
                        .title(notice.getTitle())
                        .content(notice.getContent())
                        .updateDate(notice.getUpdateDate())
                        .build())
                .collect(Collectors.toList());
    }
}
