package com.dev.dugout.domain.admin.controller;


import com.dev.dugout.domain.admin.dto.NoticeDto;
import com.dev.dugout.domain.admin.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService noticeService;


    @GetMapping
    public ResponseEntity<List<NoticeDto>> getAll() {
        return ResponseEntity.ok(noticeService.findAllNotices());
    }
}
