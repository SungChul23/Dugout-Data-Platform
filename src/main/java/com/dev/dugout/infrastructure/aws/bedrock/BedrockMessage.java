package com.dev.dugout.infrastructure.aws.bedrock;

/**
 * Bedrock API 호출 시 사용되는 메시지 레코드.
 *
 * @param role    메시지 역할 ("user" 또는 "assistant")
 * @param content 메시지 내용
 */
public record BedrockMessage(String role, String content) {

    public static BedrockMessage user(String content) {
        return new BedrockMessage("user", content);
    }

    public static BedrockMessage assistant(String content) {
        return new BedrockMessage("assistant", content);
    }
}
