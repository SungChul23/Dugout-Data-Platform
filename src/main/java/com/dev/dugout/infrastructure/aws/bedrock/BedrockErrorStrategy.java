package com.dev.dugout.infrastructure.aws.bedrock;

/**
 * Bedrock API 호출 실패 시 적용되는 에러 처리 전략.
 */
public enum BedrockErrorStrategy {

    /**
     * 예외를 그대로 전파한다. (ChatBedrockService 등 즉시 실패가 필요한 경우)
     */
    THROW_EXCEPTION,

    /**
     * 호출자가 제공한 fallback 문자열을 반환한다.
     * (FaMarketBedrockService, ReportBedrockService, RecommendedBedrockService 등)
     */
    RETURN_FALLBACK
}
