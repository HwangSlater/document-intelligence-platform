# Phase 3 구현 작업 목록

- [x] 데이터베이스 chunks 스키마 추가 및 사전 보고서 기록 (`docs/decision-request.md`)
- [x] `application.yml`에 청킹 관련 설정 추가 (`app.chunking.size`, `app.chunking.overlap`)
- [x] `Chunk` 도메인 엔티티 및 `ChunkRepository` 개발
- [x] `ChunkingService` 개발 (Spring AI `TokenTextSplitter` 연동)
- [x] DocumentService 수정 (텍스트 추출 후 ChunkingService.splitAndSave 호출 및 통계 로깅 추가)
- [x] 전체 빌드 및 테스트 동작 여부 검증 (`./gradlew build`)
- [x] 기능별 로컬 저장소 커밋 진행 및 최종 Push 상태 검증
- [x] Phase 3 종료 보고서 작성 (`docs/agent-reports/phase-03-report.md`)

## Phase 3.5 추가 최적화 작업 목록
- [x] 청킹 파라미터 최적화 실험 수행 (chunk-size: 300, 500, 800, 1000)
- [x] 실험 데이터를 토대로 RAG 최적 기본값 제안 및 yml 적용
- [x] Phase 3.5 최적화 실험 보고서 작성 (`docs/agent-reports/phase-03.5-report.md`)
