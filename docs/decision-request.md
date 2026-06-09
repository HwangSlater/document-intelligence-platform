# 의사 결정 및 변경 사항 보고서 (decision-request.md)

본 문서는 프로젝트의 중대한 변경 사항(Spring Boot 버전 변경, 의존성 추가, DB 스키마 변경, 아키텍처 변경 등)을 기록하고 사전 보고하기 위한 문서입니다.

---

## [2026-06-09] Phase 1 초기 환경 구축에 따른 변경 보고

### 1. Spring Boot 버전 변경
* **기존**: `4.0.6`
* **변경**: `3.5.14`
* **사유**: Spring Boot 4.0.6의 사용 중단 및 Java 21과의 안정적인 결합을 지원하기 위해 현 시점 최신 안정 버전대인 3.5.x 계열(`3.5.14`)로 하향 조정했습니다.

### 2. 의존성 추가
* **추가 의존성**:
  * `org.springframework.ai:spring-ai-bom:1.0.0-M6` (Spring AI 의존성 관리 플랫폼)
  * `org.springframework.ai:spring-ai-openai-spring-boot-starter` (OpenAI 연동 스타터)
  * `org.springframework.ai:spring-ai-pgvector-store-spring-boot-starter` (PGVector Vector Store 연동 스타터)
  * `org.apache.tika:tika-core:3.0.0` (문서 파싱 핵심 모듈)
  * `org.apache.tika:tika-parsers-standard-package:3.0.0` (표준 문서 형식 파서)
  * `org.apache.pdfbox:pdfbox:3.0.3` (PDF 구조 분석 모듈)
* **사유**: MVP 목표인 문서 업로드, 파싱, 청킹, 임베딩, PGVector 저장 및 RAG QA를 구현하기 위한 핵심 인프라 및 파싱 라이브러리 도입을 위함입니다. (Embabel 의존성은 요구에 따라 제외)

### 3. 데이터베이스 인프라 변경
* **기존 이미지**: `postgres:latest`
* **변경 이미지**: `pgvector/pgvector:pg16`
* **사유**: 06_database_design.md에서 정의된 PGVector 기반 고속 벡터 검색 기능을 컨테이너 레벨에서 즉시 지원하기 위함입니다.
