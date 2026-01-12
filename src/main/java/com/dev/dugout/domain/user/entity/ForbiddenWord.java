package com.dev.dugout.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Table(name = "forbidden_words")
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ForbiddenWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String word; // 금칙어 단어

    private String category;

    private String severity;

    private String note;
}