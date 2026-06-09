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

---

## [2026-06-09] Phase 2 데이터베이스 스키마 변경 보고

### 1. 데이터베이스 테이블 스키마 신규 추가
* **대상 테이블**: `documents`
* **상세 구조**:
  * `id`: UUID (Primary Key)
  * `file_name`: VARCHAR(255) (사용자가 업로드한 원본 파일명)
  * `file_path`: VARCHAR(500) (서버 내 로컬 파일 저장 경로)
  * `file_type`: VARCHAR(50) (파일 확장자 형식, e.g., pdf)
  * `file_size`: BIGINT (파일 용량 바이트 크기)
  * `upload_date`: TIMESTAMP (업로드 및 생성 일시)
  * `extracted_text`: TEXT / LOB (문서로부터 파싱 추출된 전체 텍스트 본문)
  * `processing_status`: VARCHAR(50) (문서 처리 상태: `UPLOADED`, `PARSED`, `FAILED`)
* **사유**: 업로드된 문서의 메타데이터와 파일 경로를 관리하고, 텍스트 추출 검증을 위해 추출한 본문을 통합 테이블 컬럼(`extracted_text`)에 임시 보관하기 위함입니다. (이번 Phase에서는 Chunk 엔티티를 분리하지 않음)

---

## [2026-06-09] Phase 3 데이터베이스 스키마 변경 보고

### 1. 데이터베이스 테이블 스키마 신규 추가
* **대상 테이블**: `chunks`
* **상세 구조**:
  * `id`: UUID (Primary Key)
  * `document_id`: UUID (외래 키, documents 테이블의 id 참조. ON DELETE CASCADE)
  * `chunk_index`: INT (문서 내 청크 순서 보장 인덱스, 0부터 시작)
  * `content`: TEXT (분할된 텍스트 본문 조각)
  * `created_at`: TIMESTAMP (생성 일시)
* **사유**: 추출된 긴 문서 본문을 대형 언어 모델(LLM)이 처리할 수 있는 의미 단위(1000자 규격)로 재귀 분할하여, 개별 청크 단위로 영속화 관리하기 위함입니다.
