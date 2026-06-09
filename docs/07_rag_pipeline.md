# RAG Pipeline 설계

## 목표

문서 기반 답변 생성

환각 최소화

출처 제공

---

# 전체 흐름

```text
Document Upload

↓

Parsing

↓

Chunking

↓

Embedding

↓

Vector Storage

↓

Retrieval

↓

Context Assembly

↓

LLM Generation

↓

Answer
```

---

# Step 1

문서 업로드

지원

* PDF
* DOCX
* TXT

---

# Step 2

문서 파싱

사용 라이브러리

* Apache Tika
* PDFBox

출력

```json
{
  "page": 15,
  "content": "..."
}
```

---

# Step 3

Chunking

전략

Recursive Character Splitting

설정

```yaml
chunk-size: 1000
chunk-overlap: 200
```

---

# Step 4

Embedding

후보

## OpenAI

text-embedding-3-large

---

## BGE-M3

로컬 가능

---

# Step 5

Vector Storage

PGVector 저장

---

# Step 6

Retrieval

전략

Similarity Search

설정

```yaml
top-k: 5
score-threshold: 0.75
```

---

# Step 7

Context 생성

Retriever 결과 병합

예시

```text
Chunk 1

Chunk 2

Chunk 3
```

---

# Step 8

Prompt 생성

규칙

* Context만 사용
* 추측 금지
* 출처 제공

---

# Step 9

LLM 응답 생성

출력

```json
{
  "answer": "...",
  "sources": [
    {
      "document": "policy.pdf",
      "page": 15
    }
  ]
}
```

---

# 품질 개선 계획

## Hybrid Search

BM25 + Dense Retrieval

---

## Reranker

Cross Encoder 적용

---

## Query Expansion

질문 확장

---

## Context Compression

불필요 문맥 제거
