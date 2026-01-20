# Dugout (더그아웃)
> **AWS DEA 기반 데이터 엔지니어링 및 AI KBO 분석 플랫폼**

본 프로젝트는 AWS Certified Data Engineer – Associate (DEA) 과정에서 습득한 데이터 아키텍처 지식을 실무에 적용하고, 머신러닝 기반의 선수 성적 예측 기능을 제공하는 KBO 특화 데이터 플랫폼입니다.

<br>

## 📅 Development Log (2026.01)

### 🚀 Phase 1: 고가용성 인프라 및 배포 자동화 (01.03 ~ 01.07)
**"이론적 지식을 프로덕션 환경에 투영하는 데이터 아키텍처 설계"**

- **01.03 ~ 01.05: 프로젝트 고도화 전략 수립**
  - **AWS DEA** 자격 취득 기반, RDB 중심 구조에서 **Data Lake(S3) + Serverless Query(Athena)** 분석형 아키텍처로 확장.
  - **Amazon Bedrock**을 활용한 Generative AI 도입으로 사용자 맞춤형 인사이트 제공 환경 구축.
- **01.06: Full-Stack 환경 및 CI/CD 구축**
  - **CI/CD:** GitHub Actions 기반 빌드/배포 자동화 및 Docker를 활용한 환경 일관성 유지.
  - **Proxy:** Nginx 리버스 프록시 설정을 통한 보안 강화 및 통신 최적화.
- **01.07: VPC 기반 보안 인프라 및 DB 보안**
  - **Network:** Public/Private Subnet 분리를 통한 EC2/RDS 격리 및 **최소 권한 원칙(Least Privilege)** 적용.
  - **DB Access:** Bastion Host를 통한 **SSH 터널링**으로 데이터베이스 직접 노출 차단 및 보안성 확보.

<br>

### 💡 Phase 2: 데이터 파이프라인 및 지능형 로직 구현 (01.08 ~ 01.10)
**"비정형 데이터의 정형화와 AI 기반 개인화 서비스"**

- **"나의 팀 찾기" (Serverless Analytics Workflow)**
  - **ETL:** 사용자 로그/통계 데이터를 S3 적재 후, **AWS Glue Crawler**로 메타데이터 카탈로그 자동 생성.
  - **AI Logic:** **Few-shot Prompting** 기법을 Bedrock에 적용하여 사용자 성향 기반 구단 추천 정확도 개선.
- **"실시간 KBO 뉴스" (Data Integration)**
  - Naver Search API 연동 및 성능 향상을 위한 Caching 전략 도입 검토.
  - 구단별 특화 키워드 필터링을 위한 동적 쿼리 파라미터 로직 구현.
- **Legacy 데이터 마이그레이션**
  - 구 버전 데이터(용어/룰)를 신규 시스템 스키마에 맞춰 정형화 및 이관 완료.

<br>

### 🛠 Phase 3: 관계형 모델링 및 사용자 인증 고도화 (01.11 ~ 01.12)
**"확장 가능하고 견고한 백엔드 시스템 구축"**

- **01.11: 26시즌 대응 DB 스키마 설계**
  - **Optimization:** 대용량 경기 기록 조회를 대비하여 `game`, `player_stats` 등 주요 엔티티에 **Index 설계 및 쿼리 최적화**.
  - **Data Ingestion:** Python(BeautifulSoup/Selenium) 크롤러를 제작하여 26시즌 일정을 DB에 사전 적재.
- **01.12: 보안 강화 및 UX 최적화**
  - **Auth:** **JWT & Refresh Token** 도입으로 보안성과 무중단 사용자 경험 확보.
  - **Security:** 정규표현식 기반 공통 필터 구현으로 SQL Injection 방지 및 욕설/혐오 표현 필터링 로직 적용.
  - **Dashboard:** 사용자별 선호 구단 데이터를 시각화하는 개인화 대시보드 구축.

<br>

### 📊 Phase 4: 머신러닝 기반 성적 예측 모델링 (01.15 ~ 진행 중)
**"도메인 지식을 결합한 ML 성능 개선"**

- **Feature Engineering**
  - 2015~2025년(10년치) KBO 전 선수 성적을 수집하여 유의미한 예측 지표(타석당 안타, 홈런 비율 등) 추출.
- **과적합(Overfitting) 해결 및 일반화 성능 확보**
  - **Problem:** 특정 선수 1인의 데이터만 학습 시 샘플 부족으로 인한 예측값 왜곡(Outlier) 발생.
  - **Solution:** 야구 통계학의 **'에이징 커브(Aging Curve)'** 개념 주입. 전 선수층 데이터를 통합 학습시켜 나이에 따른 보편적 성적 변화 흐름을 베이스라인으로 설정하여 모델의 일반화 성능(Generalization) 극대화.
- **XGBoost 모델 최적화**
  - Learning Rate, Max Depth 등 하이퍼파라미터 튜닝을 통해 **RMSE 최소화** 진행 중.

<br>

## 🛠 Tech Stack

| Category | Technology |
| :--- | :--- |
| **Frontend** | React, Tailwind CSS |
| **Backend** | Java Spring Boot, Spring Data JPA, JWT |
| **Data/ML** | Python, XGBoost, Scikit-learn, BeautifulSoup |
| **AWS Infra** | EC2, RDS (MySQL), S3, Athena, Glue, Bedrock ,SageMaker AI|
| **DevOps** | GitHub Actions, Nginx, VPC |

<br>

## 🌟 Key Highlights
- **Data Governance:** AWS DEA 역량을 활용하여 S3-Athena 중심의 서버리스 분석 파이프라인 구축.
- **Problem Solving:** 단순 모델 튜닝이 아닌 도메인 지식(Aging Curve)을 활용한 데이터셋 재구조화로 과적합 문제 해결.
- **Secure Architecture:** 실제 상용 환경 수준의 VPC 격리 및 SSH 터널링 보안 아키텍처 적용.
