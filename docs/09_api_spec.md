# API Specification

## Base URL

```text
/api/v1
```

---

# Document API

## 문서 업로드

POST

```http
/documents
```

Request

multipart/form-data

```text
file
```

Response

```json
{
  "documentId": "uuid",
  "status": "UPLOADED"
}
```

---

## 문서 목록

GET

```http
/documents
```

Response

```json
[
  {
    "id": "...",
    "name": "policy.pdf"
  }
]
```

---

## 문서 삭제

DELETE

```http
/documents/{id}
```

---

# Chat API

## 질문

POST

```http
/chat
```

Request

```json
{
  "question": "출장비 규정 알려줘"
}
```

---

Response

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

# Summary API

POST

```http
/summary
```

Request

```json
{
  "documentId": "uuid"
}
```

---

Response

```json
{
  "summary": "..."
}
```

---

# Extraction API

POST

```http
/extract
```

Request

```json
{
  "documentId": "uuid",
  "target": "email"
}
```

---

Response

```json
{
  "emails": [
    "example@test.com"
  ]
}
```

---

# Compare API

POST

```http
/compare
```

Request

```json
{
  "documentA": "uuid",
  "documentB": "uuid"
}
```

---

Response

```json
{
  "added": [],
  "removed": [],
  "modified": []
}
```

---

# Agent API

POST

```http
/agent
```

Request

```json
{
  "query": "출장비 규정 변경사항 비교해줘"
}
```

Response

```json
{
  "agent": "ComparisonAgent",
  "result": {}
}
```
