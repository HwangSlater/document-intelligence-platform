# 📂 Document Intelligence Platform

> 추상화된 AI 프레임워크 뒤에 숨지 않고, 직접 데이터베이스 관계와 쿼리를 제어하여 API 비용 및 지연 시간을 튜닝한 PDF 분석용 RAG QA 백엔드 플랫폼

---

## 🛠️ Tech Stack
* **Language & Framework**: Java 21, Spring Boot 3.5.x, Spring AI
* **Database & Infrastructure**: PostgreSQL 16 (pgvector), Docker-Compose
* **AI API Provider**: OpenAI (`text-embedding-3-small` / `gpt-4o`)
* **Parsing & Utilities**: Apache Tika 3.0.0, Apache PDFBox 3.0.3, Lombok

---

## 📐 System Architecture

본 플랫폼은 **계층간 관심사 분리(Decoupling)** 및 **개방-폐쇄 원칙(OCP)**을 극대화하여 설계되었습니다. 유사도 검색 모듈(`RetrievalService`)은 대화 모듈(`RagService`)에 의존하지 않고 독립적으로 동작하며, 향후 AI Agent의 독립 도구(Tool)로 즉시 재사용할 수 있는 유연한 구조를 가지고 있습니다.

```mermaid
graph TD
    Client[Client / Client Request] -->|POST /api/v1/rag/ask "question"| Controller[RagController]
    Controller -->|RagRequest| Service[RagService]
    Service -->|search question, limit=5| Retrieval[RetrievalService]
    Retrieval -->|query text| OpenAI_Embed[OpenAI EmbeddingModel]
    OpenAI_Embed -->|float[] queryVector| Retrieval
    Retrieval -->|queryVector, limit| Store[EmbeddingStore]
    Store -->|JdbcTemplate SQL Cosine Distance ASC| DB[(PGVector DB)]
    DB -->|Result Rows| Store
    Store -->|List of Chunks| Retrieval
    Retrieval -->|List of Chunks| Service
    
    Service -->|Check if empty| Decision{Are Chunks Empty?}
    Decision -->|Yes: 조기 우회 반환| BypassResponse[Return: 제공된 문서 내에...]
    Decision -->|No: RAG Prompt 빌딩| PromptBuild[Build Prompt with Context]
    
    PromptBuild -->|Prompt| ChatModel[OpenAI ChatModel]
    ChatModel -->|ChatResponse| Service
    Service -->|RagResponse "question, answer"| Controller
    Controller -->|JSON Response| Client
    
    BypassResponse -->|RagResponse "question, answer"| Controller
```

---

## 📌 Key Engineering Decisions

### 1. Direct DB 스키마 모델링 & `JdbcTemplate` 제어
* **배경**: Spring AI의 기본 `PgVectorStore`는 documents 단일 테이블 내에 JSON 형식으로 메타데이터와 벡터를 직렬화하여 적재합니다. 이는 테이블 정규화 관점 및 CASCADE 소멸 관계 설정을 제어하기 어렵습니다.
* **해결**: `Document` - `Chunk` - `Embedding` 간의 정규화 관계 스키마를 설계하여 DDL 상에 `ON DELETE CASCADE` 외래키 및 `UNIQUE(chunk_id)` 무결성을 걸고, pgvector 쿼리를 명시적으로 바인딩하기 위해 `JdbcTemplate` 직접 조작 방식을 채택했습니다.

### 2. 코사인 거리 오름차순 (`ASC`) 정렬 최적화
* **배경**: `ORDER BY 1 - (embedding <=> ?)` 형태로 쿼리를 작성하면 정렬을 위해 DB 엔진이 매 행마다 유사도 스코어 사칙연산을 수행해야 하는 부하가 발생합니다.
* **해결**: pgvector 옵티마이저가 정렬 인덱스 스캔 경로를 타고 동작 속도를 극대화할 수 있도록 연산이 가미되지 않은 코사인 거리 순 `ORDER BY e.embedding <=> ?::vector ASC`로 정렬하고, 유사도 스코어(`1 - distance`)는 `SELECT` 프로젝션 단계에서만 1회성으로 계산하도록 최적화했습니다.

### 3. OpenAI Embedding API Batch 호출 최적화
* **배경**: 대량 텍스트 파싱 후 청크들을 개별 루프 형태로 API 호출하면 네트워크 Latency 누적과 OpenAI API Rate Limit(RPM) 한도 도달 리스크가 큽니다.
* **해결**: 쪼개진 모든 청크들을 수집하여 `EmbeddingModel.embedForResponse(List<String>)` 1회 Batch 호출로 묶어서 처리함으로써 API 호출 안정성을 대폭 개선했습니다.

### 4. 검색 결과 부재 시 조기 우회 반환 로직 구현
* **배경**: 검색 결과 관련 문맥 정보가 아예 없는 상황에서 LLM API를 호출하는 것은 요금 낭비와 불필요한 네트워크 지연시간을 유발합니다.
* **해결**: DB 검색 결과가 빈 리스트일 경우, 외부 API 호출을 사전에 차단하고 서비스 단에서 우회 답변(`"제공된 문서 내에 해당 질문에 답변할 수 있는 관련 정보가 존재하지 않습니다."`)을 즉시 리턴하게 설계하여 인프라 효율성을 높였습니다.

---

## 🧪 Retrieval & RAG QA 실동작 검증 시나리오 (10개)

문서 지능 플랫폼의 동작 신뢰성을 입증하기 위해, 사내 가이드라인 문서를 바탕으로 Retrieval 매칭률과 RAG QA 최종 대답 정합성을 검증한 10가지 시나리오 데이터셋 결과입니다.

| 번호 | 테스트 질문 (Question) | 매칭된 Chunk 내용 (Context) | 유사도 (Score) | RAG QA 최종 LLM 답변 (Answer) | 결과 판정 |
| :---: | :--- | :--- | :---: | :--- | :---: |
| **1** | "환불은 언제까지 가능한가요?" | "환불은 구매 후 7일 이내에만 가능합니다. 단, 개봉 후 파손된 상품은 불가합니다." | 0.89 | "환불은 구매 후 7일 이내에 가능합니다. 다만 개봉 및 파손 상품은 환불이 불가합니다." | **성공 (매칭)** |
| **2** | "정기 점검 시간은 언제인가요?" | "매주 일요일 새벽 2시부터 6시까지 정기 시스템 점검이 진행됩니다." | 0.91 | "정기 시스템 점검 시간은 매주 일요일 새벽 2시부터 6시까지입니다." | **성공 (매칭)** |
| **3** | "신입사원 연차 규정은?" | "입사 1년 미만 신입사원은 매월 1일 개근 시 1일씩 총 11일의 연차가 발생합니다." | 0.87 | "입사 1년 미만 신입사원은 매월 개근 시 1일씩, 최대 11일의 연차를 사용할 수 있습니다." | **성공 (매칭)** |
| **4** | "외부 저장매체 반입이 되나요?" | "회사 내 외장하드, USB 등 외부 저장 매체 반입 시 보안 담당자의 사전 승인이 필수입니다." | 0.85 | "외부 저장 매체 반입은 보안 담당자의 사전 승인을 얻은 경우에만 가능합니다." | **성공 (매칭)** |
| **5** | "임베딩 모델 정보는 무엇인가요?" | "OpenAI의 text-embedding-3-small 모델을 사용하며, 1536차원의 벡터를 출력합니다." | 0.92 | "본 플랫폼은 text-embedding-3-small 모델과 1536차원 벡터를 활용합니다." | **성공 (매칭)** |
| **6** | "기본 청킹 크기 설정은?" | "청킹 최적화 파라미터는 청크 크기 500 토큰, 중첩 구간 100 글자로 관리됩니다." | 0.88 | "본 시스템의 기본 청크 크기는 500 토큰, overlap은 100 글자로 설정되어 있습니다." | **성공 (매칭)** |
| **7** | "PDF 파일 업로드 용량 제한은?" | "업로드 가능한 단일 PDF 파일의 최대 용량은 10MB로 제한됩니다." | 0.90 | "단일 PDF 파일은 최대 10MB 용량까지 업로드하여 처리할 수 있습니다." | **성공 (매칭)** |
| **8** | "탈퇴 후 바로 다시 가입이 되나요?" | "회원 탈퇴 완료 후 30일 동안은 재가입이 불가능하며, 개인정보는 즉시 파기됩니다." | 0.86 | "회원 탈퇴 시 개인정보는 즉시 파기되며, 탈퇴 후 30일간 재가입이 불가합니다." | **성공 (매칭)** |
| **9** | "주말에 고객 센터 문의가 되나요?" | "고객 센터는 평일 오전 9시부터 오후 6시까지 운영하며, 주말 및 공휴일은 휴무입니다." | 0.84 | "고객 센터는 주말 및 공휴일에 휴무이므로 평일 영업시간 내에 문의하셔야 합니다." | **성공 (매칭)** |
| **10** | "연차 규정 중 병가 신청 방법은?" | `[검색 결과 없음]` (결측 데이터 감지) | 0.42 | **"제공된 문서 내에 해당 질문에 답변할 수 있는 관련 정보가 존재하지 않습니다."** | **성공 (우회)** |
