# Embabel Agent 설계

## 목표

단순 QA 시스템이 아닌

업무 지원 Agent 시스템 구축

---

# Agent 구조

## DocumentSearchAgent

역할

문서 탐색

관련 문서 검색

---

입력

```text
출장비 규정 알려줘
```

---

출력

관련 문서 목록

관련 Chunk

---

## SummaryAgent

역할

문서 요약

---

입력

문서

---

출력

요약 결과

---

## ExtractionAgent

역할

구조화 정보 추출

---

추출 예시

* 담당자
* 이메일
* 일정
* 전화번호
* 금액

---

출력

JSON

---

## ComparisonAgent

역할

문서 비교

---

입력

문서 A

문서 B

---

출력

변경점 분석

---

# Agent Routing

```text
User Question

↓

Intent Classification

↓

Agent Selection

↓

Agent Execution

↓

Response
```

---

# Intent 분류

예시

```text
요약해줘
```

↓

SummaryAgent

---

```text
비교해줘
```

↓

ComparisonAgent

---

```text
담당자 추출
```

↓

ExtractionAgent

---

# Tool 구성

## Retrieval Tool

PGVector 검색

---

## Summary Tool

요약 생성

---

## Extraction Tool

정보 추출

---

## Comparison Tool

문서 비교

---

# 향후 확장

Multi-Agent 협업

예시

Search Agent

↓

Summary Agent

↓

Extraction Agent

순차 실행
