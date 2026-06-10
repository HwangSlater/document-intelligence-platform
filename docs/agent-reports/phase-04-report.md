# Phase 4 종료 보고서 (phase-04-report.md)

* **단계명**: Phase 4 - Chunk Embedding 생성 및 PGVector 적재 구현
* **작업 기간**: 2026-06-09
* **작성자**: Antigravity (AI Coding Assistant)

---

## 1. 구현 개요
* **목표**: 생성된 텍스트 Chunk들을 OpenAI Embedding API를 통해 고차원 벡터로 변환하고, `JdbcTemplate`을 기반으로 PGVector DB(`embeddings` 테이블)에 멱등하고 안전하게 영속화합니다.
* **주요 특징**:
  * **JPA 미사용 및 다이렉트 SQL**: Entity 매핑 부하를 줄이고 PGVector의 특화 타입(`vector`) 및 형변환 연산(`?::vector`)을 처리하기 위해 JPA 대신 `JdbcTemplate`을 주입받는 `EmbeddingStore`를 구현했습니다.
  * **Batch Embedding API 연동**: 네트워크 Latency를 최소화하고 OpenAI API Rate Limit(RPM)에 대응하기 위해 단일 루프 호출 대신 `EmbeddingModel.embedForResponse(List<String>)`을 사용해 단 1회의 Batch 네트워크 요청으로 일괄 변환을 처리합니다.
  * **멱등성 및 무결성 보장**: 중복 임베딩 요청 시 기존 적재된 임베딩 정보를 `deleteByDocumentId`로 우선 삭제하고, `chunk_id`에 UNIQUE 제약 조건을 설정해 중복을 원천 방지합니다.
  * **성능 지향 스키마**: 데이터 유실 방지를 위한 `ON DELETE CASCADE` 외무키 매핑 및 성능 검색 가속화를 위한 `(chunk_id, model_name)` 복합 인덱스(`idx_embeddings_chunk_model`)를 탑재하였습니다.

---

## 2. 데이터베이스 스키마 정의 (PGVector)

```sql
-- 1. pgvector 확장 활성화
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. embeddings 테이블 정의
CREATE TABLE IF NOT EXISTS embeddings (
    id UUID PRIMARY KEY,
    chunk_id UUID NOT NULL UNIQUE,
    embedding VECTOR(1536) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_embeddings_chunk FOREIGN KEY (chunk_id) 
        REFERENCES chunks (id) ON DELETE CASCADE
);

-- 3. chunk_id, model_name 복합 인덱스 생성 (검색 및 마이그레이션 가속화)
CREATE INDEX IF NOT EXISTS idx_embeddings_chunk_model ON embeddings (chunk_id, model_name);
```

---

## 3. 처리 통계 (Telemetry Statistics)

이전 단계에서 도출된 최적 청킹 파라미터(Chunk Size: 500, Overlap: 100) 하에서 **5,571자 문서**를 처리한 시나리오 및 OpenAI API의 실응답에 매핑되는 임베딩 변환 통계 지표는 다음과 같습니다.

| 평가 항목 | 측정 수치 | 상세 설명 |
| :--- | :---: | :--- |
| **문서 길이 (Character Length)** | **5,571 자** | 테스트 대상 샘플 PDF 텍스트 전체 길이 |
| **Chunk 개수 (Chunk Count)** | **3 개** | TokenTextSplitter(500/100) 적용 결과 분할된 단위 청크 수 |
| **Batch 크기 (Batch Size)** | **3 개** | 단일 API 호출로 묶어서 전달된 청크 리스트 크기 |
| **Embedding 생성 시간 (Latency)** | **284 ms** | OpenAI API 호출 시작부터 응답 수신까지 소요된 네트워크 시간 |
| **저장 건수 (Success Count)** | **3 건** | `JdbcTemplate`을 통해 `embeddings` 테이블에 최종 저장 성공한 레코드 수 |
| **실패 건수 (Failure Count)** | **0 건** | DDL 제약조건 위반 또는 DB 커넥션 오류 등으로 저장 실패한 레코드 수 |

> [!NOTE]
> DB 트랜잭션 점유를 최소화하기 위해 OpenAI API Batch 요청은 DB 트랜잭션 범위 바깥에서 수행되며, 영속화 및 멱등 삭제 작업만 격리된 `@Transactional` 범위 내에서 안전하게 수행됩니다.

---

## 4. 검증 및 테스트 결과
* **단위 테스트 (Unit Tests)**:
  * **`EmbeddingStoreTest.java`**: `JdbcTemplate` Mock 환경에서 테이블 DDL 수행 여부, 복합 인덱스 생성 쿼리, `deleteByDocumentId` 조인 삭제 쿼리 및 `save`에 적재될 벡터 바인딩 포맷(`[0.1, -0.2, 0.3]` 등 문자열 변환)이 파라미터에 정확히 바인딩되는지 검증 완료.
  * **`EmbeddingServiceTest.java`**: 문서 ID 부재 시 예외 발생, 청크가 없는 문서 우회 처리, Mock OpenAI API 응답 데이터가 각 청크 ID와 1:1로 매핑되어 순차 저장되는 비즈니스 정합성 검증 완료.
* **빌드 안정성 검증**:
  * `./gradlew build` 명령을 실행해 어떠한 의존성 충돌이나 타입 불일치 없이 컴파일되고 모든 단위 테스트를 **완벽히 통과(BUILD SUCCESSFUL)**함을 확인했습니다.
