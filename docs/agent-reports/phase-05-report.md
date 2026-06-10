# Phase 5 종료 보고서 (phase-05-report.md)

* **단계명**: Phase 5 - 유사도 검색(Retrieval) 구현
* **작업 기간**: 2026-06-10
* **작성자**: Antigravity (AI Coding Assistant)

---

## 1. 구현 개요
* **목표**: 사용자가 입력한 한국어/영어 검색 질의(Query)에 대응하는 임베딩 벡터를 실시간 생성하고, PGVector DB와의 연동을 통해 코사인 유사도가 가장 높은 상위 K개의 텍스트 Chunk를 탐색 및 반환하는 Retrieval MVP를 구축했습니다.
* **MVP & YAGNI 최적화 내용**:
  * **단일 검색 통로 일원화**: 전체 업로드 문서를 아우르는 유사도 검색(`GET /api/v1/search`)에 집중하여 비즈니스 핵심 검색 모델을 단순화하고, 미사용 document-specific 엔드포인트 생성을 배제했습니다.
  * **임계값 조기 필터 제거**: 임계값 튜닝(Thresholding)은 도메인과 비즈니스 요구사항에 맞춰 후속 RAG 연동 단계에서 미세 조정할 영역이므로, 이번 데이터베이스 레벨 SQL 조건절에서는 걷어내어 복잡도를 낮췄습니다.
  * **저장소 빈(Bean) 합병**: 별도의 `SearchStore` 빈을 도입하지 않고, 기존 `EmbeddingStore` 클래스 내에 유사도 쿼리 로직을 통합함으로써 빈 관리 부하를 억제했습니다.
  * **극도화된 DTO 슬림화**: RAG 프롬프트 컨텍스트 구성에 반드시 필요한 핵심 데이터 필드(`chunkId`, `documentId`, `content`, `similarity`)만을 전송하는 `SearchResultResponse` DTO를 정의했습니다.

---

## 2. 데이터베이스 쿼리 및 정렬 최적화

pgvector의 코사인 연산자 `<=>` 연산 최적화를 유도하고, 매 로우마다 연산식(`1 - distance`)을 실행하지 않도록 정렬 구문을 코사인 거리 오름차순(`ASC`)으로 최적화하였습니다.

```sql
SELECT 
    c.id AS chunk_id, 
    c.document_id AS document_id, 
    c.content AS content, 
    (1 - (e.embedding <=> ?::vector)) AS similarity
FROM embeddings e
INNER JOIN chunks c ON e.chunk_id = c.id
ORDER BY e.embedding <=> ?::vector ASC
LIMIT ?;
```

> [!TIP]
> * **JOIN 최적화**: 파일 이름 추출 등을 위해 `documents` 테이블까지 JOIN하던 기존 안을 수정하여, `embeddings`와 `chunks` 테이블만 단일 JOIN하도록 구성함으로써 대형 텍스트 스캔 시 디스크 I/O와 인메모리 임시 정렬 연산 비용을 획기적으로 절감했습니다.
> * **JDBC 파라미터 매핑**: `float[]` 형태의 임베딩 벡터가 JDBC 드라이버에서 인식 에러를 내지 않도록 `java.util.Arrays.toString(queryVector)`를 이용해 PostgreSQL pgvector 표준 규격 문자열(`[0.12, -0.45, ...]`)로 변환 바인딩을 적용했습니다.

---

## 3. 검증 및 테스트 결과
* **단위 테스트 (Unit Tests)**:
  * **`EmbeddingStoreSimilarityTest.java`**: Mock `JdbcTemplate`과 `ResultSet`을 통해 코사인 거리 쿼리 포맷팅, 파라미터 바인딩 상태 검증 및 ResultSet 레코드가 `SearchResultResponse` DTO로 오차 없이 정밀하게 맵핑되는지 독립 검증을 완료했습니다.
  * **`RetrievalServiceTest.java`**: 빈 질의 시 즉각적인 예외/공백 반환, OpenAI API 장애 타임아웃 발생 시 예외 전파, 그리고 정상 쿼리 벡터 변환 후 EmbeddingStore로의 한도(limit) 검색 조율 흐름을 완벽히 격리 검증 완료했습니다.
* **전체 빌드 검증**:
  * `./gradlew build` 명령을 실행해 타입 충돌이나 순환 참조 없이 컴파일되고 모든 단위 테스트를 **완벽히 통과(BUILD SUCCESSFUL)**함을 확인했습니다.

---

## 4. RAG로의 확장 방안
이번 Phase 5에서 구현한 Retrieval MVP는 `List<SearchResultResponse>` 형식을 반환합니다. 다음 Phase 6에서 RAG를 기동할 때, 이 반환 리스트를 획득하여 프롬프트 템플릿(System Prompt / User Context Template) 내에 문자열 결합 형태로 주입하고, 최종 LLM Chat API에 질문과 함께 넘겨주기만 하면 동작하는 형태로 결합성이 극도로 낮게 설계되어 매우 유연하게 확장 가능합니다.
