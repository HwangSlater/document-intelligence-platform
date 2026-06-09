# 데이터베이스 설계

## 개요

본 프로젝트는 PostgreSQL을 메인 데이터베이스로 사용한다.

벡터 검색은 PGVector Extension을 활용한다.

---

# documents

문서 메타데이터 저장

```sql
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255),
    file_type VARCHAR(50),
    file_size BIGINT,
    upload_date TIMESTAMP,
    processing_status VARCHAR(50)
);
```

---

# chunks

문서 청크 저장

```sql
CREATE TABLE chunks (
    id UUID PRIMARY KEY,
    document_id UUID,
    page_number INT,
    chunk_index INT,
    content TEXT,
    created_at TIMESTAMP
);
```

---

# embeddings

벡터 저장

```sql
CREATE TABLE embeddings (
    id UUID PRIMARY KEY,
    chunk_id UUID,
    embedding VECTOR(1536)
);
```

---

# conversations

대화 저장

```sql
CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    user_id UUID,
    question TEXT,
    answer TEXT,
    created_at TIMESTAMP
);
```

---

# agent_tasks

Agent 실행 이력

```sql
CREATE TABLE agent_tasks (
    id UUID PRIMARY KEY,
    task_type VARCHAR(50),
    task_status VARCHAR(30),
    input_data JSONB,
    created_at TIMESTAMP
);
```

---

# agent_results

Agent 결과

```sql
CREATE TABLE agent_results (
    id UUID PRIMARY KEY,
    task_id UUID,
    result_data JSONB,
    completed_at TIMESTAMP
);
```

---

# 인덱스

```sql
CREATE INDEX idx_document_name
ON documents(file_name);

CREATE INDEX idx_chunk_document
ON chunks(document_id);

CREATE INDEX idx_conversation_created
ON conversations(created_at);
```

---

# PGVector 인덱스

```sql
CREATE INDEX embedding_vector_idx
ON embeddings
USING hnsw (embedding vector_cosine_ops);
```

---

# 저장 전략

Document

↓

Chunk

↓

Embedding

순서로 저장

삭제 시 Cascade 처리
