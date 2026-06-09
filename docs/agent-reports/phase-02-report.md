# Phase 2 종료 보고서 (phase-02-report.md)

* **단계명**: Phase 2 - PDF 업로드, 로컬 저장 및 메타데이터 관리
* **작업 기간**: 2026-06-09
* **작성자**: Antigravity (AI Coding Assistant)

---

## 1. 단계 목표 및 요구사항 제한
* **핵심 목표**: PDF 업로드, 서버 로컬 저장소 파일 저장, 데이터베이스 문서 메타데이터 저장, Apache Tika를 통한 텍스트 파싱 처리 구현.
* **제한 사항**: Embedding 생성 제외, PGVector 저장 및 쿼리 제외, OpenAI 호출 제외, RAG 구성 제외, Agent Workflow 제외. (Chunk Entity 미생성 및 DocumentDetail API 제외)

---

## 2. 수행 내용 및 산출물
* **도메인 엔티티 설계**:
  * [Document.java](file:///c:/Users/pc/IdeaProjects/document-intelligence-platform/src/main/java/com/documentintelligenceapplication/domain/entity/Document.java): 파싱된 본문을 저장하기 위해 `@Lob` 및 `@Column(columnDefinition = "TEXT")`로 매핑한 `extractedText` 필드를 구성한 단일 엔티티 설계.
  * [ProcessingStatus.java](file:///c:/Users/pc/IdeaProjects/document-intelligence-platform/src/main/java/com/documentintelligenceapplication/domain/entity/ProcessingStatus.java): `UPLOADED`, `PARSED`, `FAILED` 상태 구성.
* **비즈니스 서비스 및 파서 구현**:
  * [FileStorageService.java](file:///c:/Users/pc/IdeaProjects/document-intelligence-platform/src/main/java/com/documentintelligenceapplication/infrastructure/storage/FileStorageService.java): `uploads` 디렉토리에 UUID를 조합해 파일을 안전하게 디스크에 저장하는 기능 구현 (디렉토리 침투 예방책 적용).
  * [TikaDocumentParser.java](file:///c:/Users/pc/IdeaProjects/document-intelligence-platform/src/main/java/com/documentintelligenceapplication/infrastructure/parser/TikaDocumentParser.java): Apache Tika Facade 인터페이스를 활용하여 파일에서 본문 텍스트를 추출하는 파싱 핵심 기능 개발.
  * [DocumentService.java](file:///c:/Users/pc/IdeaProjects/document-intelligence-platform/src/main/java/com/documentintelligenceapplication/application/service/DocumentService.java): 업로드 파일 저장, Tika 텍스트 추출, Document 메타데이터와 추출 텍스트의 데이터베이스 트랜잭션 단위 영속화 및 삭제 로직 오케스트레이션 구현.
* **REST API 및 예외 처리**:
  * [DocumentController.java](file:///c:/Users/pc/IdeaProjects/document-intelligence-platform/src/main/java/com/documentintelligenceapplication/presentation/controller/DocumentController.java): POST(업로드), GET(목록), DELETE(삭제) 엔드포인트 제공.
  * [GlobalExceptionHandler.java](file:///c:/Users/pc/IdeaProjects/document-intelligence-platform/src/main/java/com/documentintelligenceapplication/presentation/exception/GlobalExceptionHandler.java): 커스텀 비즈니스 에러(`DocumentNotFoundException`, `InvalidFileException`, `DocumentParseException` 등) 발생 시 공통 에러 포맷(`ErrorResponse`)으로 JSON 응답 처리.

---

## 3. 검증 결과
* JUnit 단위 테스트 및 빌드 컴파일 검증을 위해 `./gradlew build` 명령을 최종 실행하여 **`BUILD SUCCESSFUL`**로 모든 점검을 완수했습니다.
