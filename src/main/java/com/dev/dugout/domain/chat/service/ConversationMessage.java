package com.dev.dugout.domain.chat.service;


// 역할: 대화 메시지 한 줄을 담는 단순 레코드

//toPromptFormat()으로 LLM에 넘길 텍스트 변환

//"사용자: 홈런 1위?"
//"AI: OOO 선수입니다"

public record ConversationMessage(String role, String content) {

    public String toPromptFormat() {
        String roleLabel = "user".equals(role) ? "사용자" : "AI";
        return roleLabel + ": " + content;
    }
}