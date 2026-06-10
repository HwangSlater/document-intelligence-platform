# Phase 6 구현 작업 목록

- [x] `RagRequest.java` DTO 개발 (question 단일 필드)
- [x] `RagResponse.java` DTO 개발 (question, answer 필드 포함)
- [x] `RagService.java` 개발 (검색 결과 부재 시 조기 우회 반환 로직 적용, ChatModel 호출 및 Prompt 상수 매핑)
- [x] `RagController.java` 개발 (POST /api/v1/rag/ask 엔드포인트 구현)
- [x] `RagServiceTest.java` 단위 테스트 개발 (일반 RAG 성공 및 검색 결과 부재 시 조기 우회 반환 동작 검증)
- [x] `./gradlew build` 전체 빌드 및 테스트 통과 성공 여부 검증

