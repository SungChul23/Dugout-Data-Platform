package com.dev.dugout.infrastructure.aws.bedrock;

import lombok.Getter;

/**
 * Bedrock API 호출 실패 시 발생하는 커스텀 예외.
 * 호출 서비스명과 대상 모델 ID를 포함하여 디버깅을 용이하게 한다.
 */
@Getter
public class BedrockInvocationException extends RuntimeException {

    private final String callerName;
    private final String modelId;

    public BedrockInvocationException(String callerName, String modelId, String message, Throwable cause) {
        super(message, cause);
        this.callerName = callerName;
        this.modelId = modelId;
    }

    public BedrockInvocationException(String callerName, String modelId, String message) {
        super(message);
        this.callerName = callerName;
        this.modelId = modelId;
    }
}
