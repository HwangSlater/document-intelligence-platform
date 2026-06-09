# 도메인 모델

---

## User

사용자

속성

* id
* email
* role

---

## Document

업로드 문서

속성

* id
* fileName
* fileType
* uploadDate

---

## Chunk

문서 조각

속성

* id
* documentId
* pageNumber
* content

---

## Embedding

벡터 정보

속성

* id
* chunkId
* vector

---

## Conversation

대화

속성

* id
* question
* answer
* createdAt

---

## AgentTask

Agent 작업

속성

* id
* taskType
* status

---

## AgentResult

Agent 결과

속성

* id
* taskId
* result

---

# 관계

Document

1:N

Chunk

---

Chunk

1:1

Embedding

---

Conversation

N:1

User

---

AgentTask

1:1

AgentResult
