# Requirements Document

## Introduction

더그아웃(Dugout) 백엔드 프로젝트에서 식별된 8개의 Code Smell을 체계적으로 리팩토링하기 위한 요구사항 문서이다. 각 요구사항은 영향도(Impact)와 작업량(Effort) 기준으로 우선순위가 부여되며, 리팩토링 결과 코드 중복 제거, 유지보수성 향상, 에러 처리 일관성 확보, 성능 안정성 개선을 목표로 한다.

## Glossary

- **Bedrock_Client_Facade**: AWS Bedrock API 호출을 추상화하는 공통 컴포넌트로, model ID, max_tokens, temperature 등의 파라미터를 받아 통합된 invoke 메서드를 제공한다
- **KBO_Team_Registry**: KBO 10개 팀의 ID, 약칭, 풀네임 매핑 정보를 중앙 관리하는 컴포넌트(Enum 또는 유틸리티 클래스)
- **Dashboard_Service**: 사용자 대시보드 데이터를 조회·구성하는 서비스 클래스
- **Bedrock_Error_Strategy**: Bedrock API 호출 실패 시 적용되는 에러 처리 전략(예외 throw 또는 fallback 반환)
- **S3_Cache_Manager**: S3에서 로드한 데이터를 메모리에 캐싱하고 갱신하는 공통 컴포넌트
- **Domain_Filter**: 사용자 입력이 특정 도메인에 해당하는지 판별하는 컴포넌트
- **Athena_Query_Executor**: AWS Athena 쿼리 실행 및 폴링 대기를 관리하는 컴포넌트
- **Prediction_Response_DTO**: 선수 성적 예측 응답 데이터를 담는 Data Transfer Object

## Requirements

### Requirement 1: Bedrock 호출 코드 통합 (우선순위: 최상 — Impact 높음 × Effort 중간)

**User Story:** As a 개발자, I want Bedrock API 호출 로직이 하나의 공통 컴포넌트에 집중되기를, so that 코드 중복을 제거하고 모델 변경이나 에러 처리 정책 변경 시 한 곳만 수정하면 된다.

#### Acceptance Criteria

1. THE Bedrock_Client_Facade SHALL model ID, max_tokens (1 이상 4096 이하 정수), temperature (0.0 이상 1.0 이하 실수), system prompt (null 허용, 최대 10000자), 및 messages (1개 이상의 메시지 리스트)를 파라미터로 받아 단일 invoke 메서드를 제공한다
2. WHEN ChatBedrockService, FaMarketBedrockService, ReportBedrockService, RecommendedBedrockService 중 어느 것이든 Bedrock를 호출할 때, THE Bedrock_Client_Facade SHALL 해당 호출을 위임받아 처리한다
3. THE Bedrock_Client_Facade SHALL anthropic_version 헤더 설정, JSON payload 구성, InvokeModelRequest 빌드, response body에서 content[0].text 추출까지의 파싱 로직을 내부에 캡슐화한다
4. WHEN Bedrock_Client_Facade를 도입한 후, THE 기존 4개 서비스 SHALL 각자의 인라인 Bedrock 호출 코드(payload 구성, InvokeModelRequest 빌드, response 파싱)를 제거하고 Bedrock_Client_Facade의 invoke 메서드에 위임한다
5. IF Bedrock API 호출이 예외를 발생시키면, THEN THE Bedrock_Client_Facade SHALL 예외 원인을 로깅하고 호출자에게 런타임 예외를 전파하여, 각 서비스가 도메인에 맞는 폴백 응답을 결정할 수 있게 한다
6. IF invoke 메서드에 전달된 max_tokens 또는 temperature가 허용 범위를 벗어나면, THEN THE Bedrock_Client_Facade SHALL API 호출 없이 파라미터 검증 실패를 나타내는 예외를 즉시 발생시킨다

### Requirement 2: 팀 이름-ID 매핑 중앙화 (우선순위: 상 — Impact 중간 × Effort 낮음)

**User Story:** As a 개발자, I want KBO 팀 이름과 ID 매핑이 단일 소스에서 관리되기를, so that 새로운 팀 추가나 이름 변경 시 한 곳만 수정하면 되고, 분산된 하드코딩을 제거할 수 있다.

#### Acceptance Criteria

1. THE KBO_Team_Registry SHALL 팀 ID(Long), 약칭(예: "삼성", "두산", "LG"), 풀네임(예: "삼성 라이온즈", "두산 베어스")을 포함하는 단일 데이터 소스를 제공하며, 현재 KBO 리그에 등록된 10개 팀 전체를 포함한다
2. WHEN 유효한 약칭으로 팀 ID 조회를 요청하면, THE KBO_Team_Registry SHALL 해당 팀의 ID를 반환한다
3. WHEN 유효한 팀 ID로 풀네임 조회를 요청하면, THE KBO_Team_Registry SHALL 해당 팀의 풀네임을 반환한다
4. IF 등록되지 않은 약칭으로 팀 ID 조회를 요청하면, THEN THE KBO_Team_Registry SHALL 조회 실패를 나타내는 빈 결과를 반환한다
5. IF 등록되지 않은 팀 ID로 풀네임 조회를 요청하면, THEN THE KBO_Team_Registry SHALL 조회 실패를 나타내는 빈 결과를 반환한다
6. WHEN KBO_Team_Registry를 도입한 후, THE FaMarketService SHALL 팀 이름→ID 변환 시 자체 if/else 체인 대신 KBO_Team_Registry의 약칭 조회 메서드를 사용한다
7. WHEN KBO_Team_Registry를 도입한 후, THE RecommendService SHALL 팀 ID→풀네임 변환 시 자체 FULL_TEAM_NAMES 상수 대신 KBO_Team_Registry의 풀네임 조회 메서드를 사용한다

### Requirement 3: DashboardService 메서드 분리 (우선순위: 중 — Impact 중간 × Effort 중간)

**User Story:** As a 개발자, I want DashboardService의 대형 메서드들이 단일 책임 원칙에 따라 분리되기를, so that 각 기능을 독립적으로 테스트하고 수정할 수 있다.

#### Acceptance Criteria

1. THE Dashboard_Service의 getUserDashboard() SHALL 팀 순위 조회, 뉴스 조회, 슬롯별 인사이트 구성을 각각 별도의 메서드로 분리하여, getUserDashboard() 본문이 30줄을 초과하지 않으며 분리된 각 메서드는 하나의 데이터 소스만 호출한다
2. THE Dashboard_Service의 getTeamDailyStats() SHALL 타자 Entity-to-DTO 변환 로직과 투수 Entity-to-DTO 변환 로직을 각각 별도의 매핑 메서드 또는 매퍼 클래스로 추출하여, getTeamDailyStats() 본문이 30줄을 초과하지 않는다
3. THE 분리된 각 메서드 또는 매퍼 클래스 SHALL 하나의 입력 타입을 하나의 출력 타입으로 변환하거나 하나의 데이터 소스를 조회하는 단일 작업만 수행하며, 메서드 본문이 40줄을 초과하지 않는다
4. WHEN 리팩토링이 완료된 후, THE Dashboard_Service의 getUserDashboard()와 getTeamDailyStats() SHALL 기존과 동일한 DashboardResponseDto 및 TeamDailyStatsResponseDto를 반환하여, 기존 단위 테스트가 수정 없이 통과한다
5. THE 추출된 각 매핑 메서드 또는 매퍼 클래스 SHALL 독립적으로 단위 테스트가 가능하도록 DashboardService 외부 의존성 없이 입력 Entity를 받아 DTO를 반환하는 순수 변환 로직으로 구성된다

### Requirement 4: 에러 처리 전략 일관화 (우선순위: 상 — Impact 높음 × Effort 중간)

**User Story:** As a 개발자, I want Bedrock API 호출 에러에 대한 처리 전략이 프로젝트 전체에서 일관되기를, so that 장애 발생 시 예측 가능한 동작을 보장하고 디버깅이 용이해진다.

#### Acceptance Criteria

1. THE Bedrock_Error_Strategy SHALL 호출 서비스 단위로 "fallback 문자열 반환" 또는 "예외 전파" 중 하나를 선택할 수 있는 설정 방식을 제공하며, 각 서비스는 Bedrock_Client_Facade 호출 시 자신의 전략을 지정한다
2. WHEN Bedrock API 호출이 실패할 때, THE Bedrock_Client_Facade SHALL 해당 호출에 지정된 Bedrock_Error_Strategy를 조회하여 fallback 반환 또는 예외 전파 중 해당하는 처리를 수행한다
3. WHEN Bedrock API 호출이 실패할 때, THE Bedrock_Client_Facade SHALL 호출 서비스명, 호출 대상 model ID, 실패 원인 메시지를 포함하는 에러 로그를 기록한다
4. IF fallback 전략이 지정된 서비스에서 Bedrock API 호출이 실패하면, THEN THE Bedrock_Client_Facade SHALL 호출자가 사전에 제공한 fallback 문자열을 그대로 반환한다
5. IF 예외 전파 전략이 지정된 서비스에서 Bedrock API 호출이 실패하면, THEN THE Bedrock_Client_Facade SHALL 원본 예외를 cause로 감싸는 커스텀 예외 클래스를 사용하여 예외를 던진다

### Requirement 5: S3 데이터 캐싱 패턴 통합 (우선순위: 중 — Impact 중간 × Effort 중간)

**User Story:** As a 개발자, I want S3 데이터를 메모리에 캐싱하는 패턴이 공통 컴포넌트로 추출되기를, so that 캐시 갱신 메커니즘을 일관되게 적용하고 새 캐시 대상 추가 시 보일러플레이트 코드를 줄일 수 있다.

#### Acceptance Criteria

1. THE S3_Cache_Manager SHALL S3 경로와 파싱 로직을 파라미터로 받아 데이터를 로드하고 메모리에 캐싱하는 공통 메서드를 제공하며, 캐싱된 데이터를 키(예: pcode) 기반으로 개별 조회할 수 있는 get 메서드를 제공한다
2. THE S3_Cache_Manager SHALL 등록된 캐시를 S3 경로 단위로 갱신할 수 있는 refresh 메서드를 제공한다
3. WHEN FaMarketBedrockService.init() 또는 ReportBedrockService.init()에서 S3 데이터를 로드할 때, THE S3_Cache_Manager SHALL 해당 로딩 로직을 위임받아 처리하고, 기존 서비스와 동일한 키-값 조회 결과를 반환한다
4. IF S3 로드가 실패하면, THEN THE S3_Cache_Manager SHALL 에러를 로깅하고 빈 캐시 상태로 서비스를 시작하여, 이후 캐시 조회 시 기본값(빈 문자열 또는 지정된 fallback 값)을 반환한다
5. IF refresh 수행 중 S3 로드가 실패하면, THEN THE S3_Cache_Manager SHALL 에러를 로깅하고 기존 캐시 데이터를 그대로 유지한다

### Requirement 6: 도메인 판별 로직 외부화 (우선순위: 낮음 — Impact 낮음 × Effort 낮음)

**User Story:** As a 개발자, I want 야구 도메인 질문 판별 키워드를 외부 설정으로 관리하기를, so that 키워드 추가·삭제 시 코드 변경 없이 설정만 변경하면 된다.

#### Acceptance Criteria

1. THE Domain_Filter SHALL 비야구 도메인 키워드 목록을 외부 설정 파일(application.properties 또는 별도 리소스 파일)에서 애플리케이션 시작 시 로드하며, 각 키워드는 최대 50자 이내의 문자열로 구성된다
2. WHEN 키워드 목록이 변경될 때, THE Domain_Filter SHALL 애플리케이션 재시작만으로 변경된 키워드 목록을 반영한다 (재배포·재빌드 불필요)
3. WHEN 입력 문자열이 주어질 때, THE Domain_Filter SHALL 비야구 키워드 목록과 부분 문자열 포함(contains) 매칭을 수행하여, 하나 이상의 비야구 키워드가 포함되면 false를, 포함되지 않으면 true를 반환한다
4. IF 외부 설정 파일에 키워드 목록이 비어있거나 해당 설정 키가 존재하지 않는 경우, THEN THE Domain_Filter SHALL 모든 입력을 야구 도메인 질문으로 판정한다 (true 반환)

### Requirement 7: Athena 폴링 대기 타임아웃 구현 (우선순위: 상 — Impact 높음 × Effort 낮음)

**User Story:** As a 개발자, I want Athena 쿼리 폴링에 최대 대기 시간 제한이 있기를, so that 무한 루프로 인한 스레드 블로킹과 서비스 장애를 방지할 수 있다.

#### Acceptance Criteria

1. THE Athena_Query_Executor SHALL 최대 폴링 시도 횟수(기본값: 60회), 최대 대기 시간(기본값: 30초), 폴링 간격(기본값: 500밀리초)을 설정 파라미터로 받으며, 파라미터가 명시되지 않은 경우 기본값을 적용한다
2. IF 폴링 시도가 최대 횟수를 초과하거나 누적 경과 시간이 최대 대기 시간을 초과하면, THEN THE Athena_Query_Executor SHALL 타임아웃 전용 예외를 던지고, 해당 Athena 쿼리 실행을 취소(StopQueryExecution) 요청한다
3. IF Athena 쿼리 상태가 FAILED 또는 CANCELLED를 반환하면, THEN THE Athena_Query_Executor SHALL 실패 사유를 포함한 예외를 던진다
4. WHEN 폴링이 완료(성공, 타임아웃, 실패 중 하나)되면, THE Athena_Query_Executor SHALL 총 폴링 횟수와 총 경과 시간(밀리초)을 로그에 기록한다
5. THE Athena_Query_Executor SHALL 각 폴링 시도 간 대기 시간(polling interval)을 설정 파라미터로 받아, 지정된 간격만큼 대기 후 다음 상태 조회를 수행한다

### Requirement 8: PredictionResponseDto 구조 개선 (우선순위: 낮음 — Impact 낮음 × Effort 중간)

**User Story:** As a 개발자, I want 타자 전용 필드와 투수 전용 필드가 구조적으로 분리되기를, so that DTO의 가독성이 향상되고 포지션별 필드 그룹을 명확히 구분할 수 있다.

#### Acceptance Criteria

1. THE Prediction_Response_DTO SHALL 공통 필드(name, backNumber, position, aiReport)를 최상위에 유지한다
2. THE Prediction_Response_DTO SHALL 타자 전용 필드(currAvg, predAvg, avgDiff, avgMin, avgMax, currObp, predObp, diffObp, obpMin, obpMax, currSlg, predSlg, diffSlg, slgMin, slgMax, currOps, predOps, opsDiff, opsMin, opsMax, currHr, predHr, hrDiff, hrMin, hrMax)를 별도의 내부 객체(HitterStats)로 그룹화한다
3. THE Prediction_Response_DTO SHALL 투수 전용 필드(probElite, rolePercentileTop, roleRank, roleTotal, era2025, fip2025, ip2025, whip2025, role)를 별도의 내부 객체(PitcherStats)로 그룹화한다
4. IF 선수의 positionType이 해당 내부 객체의 포지션과 일치하지 않는 경우, THEN THE Prediction_Response_DTO SHALL 해당 내부 객체를 null로 설정하여 JSON 직렬화 시 해당 키를 응답에서 제외한다
5. THE Prediction_Response_DTO SHALL 내부 객체(HitterStats, PitcherStats)의 직렬화 시 JSON 플래트닝(예: @JsonUnwrapped 또는 동등한 메커니즘)을 적용하여, 기존 API 응답의 최상위 키 구조(name, backNumber, position, currAvg, predAvg, ... , probElite, ... , aiReport)를 그대로 유지한다
6. WHEN 리팩토링 완료 후 기존 API 엔드포인트(GET /prediction)를 호출하면, THE Prediction_Response_DTO SHALL 리팩토링 이전과 동일한 JSON 키 이름 및 값 구조를 반환하여 프론트엔드 코드 변경 없이 동작한다
