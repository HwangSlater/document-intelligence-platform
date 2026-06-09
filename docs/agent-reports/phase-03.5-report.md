# Phase 3.5 종료 보고서 (phase-03.5-report.md)

* **단계명**: Phase 3.5 - Chunking 파라미터 최적화 실험
* **작업 기간**: 2026-06-09
* **작성자**: Antigravity (AI Coding Assistant)

---

## 1. 실험 설계 및 목표
* **목표**: Spring AI `TokenTextSplitter` 파라미터(Chunk Size) 변화에 따른 문서 청크 개수 및 크기 분포를 측정하여, RAG 검색 환경에 가장 이상적인 기본 최적값을 도출합니다.
* **실험 텍스트**: 약 **37,351 자** 분량의 의미 단락들로 구성된 확장 텍스트 문서 ([ChunkingServiceTest.java](file:///c:/Users/pc/IdeaProjects/document-intelligence-platform/src/test/java/com/documentintelligenceapplication/application/service/ChunkingServiceTest.java))
* **중첩 구간 (Overlap)**: 단절 예방을 위해 각 Chunk Size의 20%로 동적 할당하여 평가 진행.

---

## 2. 실험 결과 통계 (Statistics)

| 실험 규격 | Chunk Size (Tokens) | Overlap (Chars) | Chunk 개수 | 평균 Chunk 크기 (Chars) | 최대 Chunk 크기 (Chars) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **실험 1** | **300** | 60 | **24 개** | 1,554.96 자 | 1,624 자 |
| **실험 2** | **500** | 100 | **15 개** | 2,488.73 자 | 2,683 자 |
| **실험 3** | **800** | 160 | **9 개** | 4,148.89 자 | 4,268 자 |
| **실험 4** | **1000** | 200 | **8 개** | 4,667.50 자 | 5,364 자 |

---

## 3. 비교 분석 및 최적 기본값 제안

### **🔍 비교 요약**
1. **소형 청킹 (300 Tokens)**:
   * 평균 약 1,500자 단위로 조밀하게 나뉩니다. 다수의 작은 맥락으로 쪼개져 벡터 조밀도는 높으나, 긴 호흡의 의미 단락이 도중에 쪼개져 문맥 유실(Context Fragmentation) 위험도가 큽니다.
2. **대형 청킹 (800 ~ 1000 Tokens)**:
   * 평균 4,000자 이상의 매우 큰 덩어리로 유지되며, 청크 개수가 8~9개로 최소화됩니다. 
   * 한 청크 내에 불필요한 주제가 혼재(Noisy Context)되어 검색 적합도가 희석되며, RAG 컨텍스트 주입 시 불필요한 LLM 토큰 낭비 및 지연(Latency)이 크게 증가합니다.
3. **중형 청킹 (500 Tokens) - 🌟 최적 제안**:
   * 평균 크기가 약 **2,488 자** 수준으로, 하나의 완결성 높은 단락(약 3~4개의 소문단)과 주변 문맥을 안전하게 보관합니다.
   * RAG 연동 시 Top-5 조각을 검색하더라도 총 12,000자 내외로 주입할 수 있어 LLM 컨텍스트 부하가 가장 적합합니다.

### **💡 최종 제안 기본값**
* **`app.chunking.size`**: **`500`** (Tokens)
* **`app.chunking.overlap`**: **`100`** (Tokens/Chars - 청크 크기의 20% 중첩 비율 적용)

---

## 4. 설정 파일 반영 내역
제안된 최적 기본값(500 / 100)을 [application.yml](file:///c:/Users/pc/IdeaProjects/document-intelligence-platform/src/main/resources/application.yml) 파일에 최종 반영 완료했습니다.
```yaml
app:
  chunking:
    size: 500
    overlap: 100
```
