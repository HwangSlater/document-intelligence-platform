# -*- coding: utf-8 -*-
import json
import time
import urllib.request
import urllib.parse
import urllib.error
import sys
import io

# 윈도우 콘솔 한글 인코딩 에러 방지
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8', errors='ignore')

questions = [
    "경남대 대학원 학칙 상 수료를 위해 필요한 최저 취득 학점 규정은?",
    "대학원 석사/박사과정의 재학 연한 초과 시 제적 기준일은?",
    "가명정보 처리 가이드라인에서 규정한 가명처리의 4단계 프로세스는?",
    "가명처리 시 추가 정보의 격리 보관 및 안전성 확보 조치 의무는?",
    "2024-2025 AI말평 과제 운영 사업의 주요 데이터 구축 대상 범위는?",
    "AI말평 과제 운영 계획서 상의 단계별 최종 결과물 마감 일정은?",
    "2025년 공공부문 AI 도입 현황에서 장애 요인으로 가장 많이 꼽힌 항목은?",
    "공공부문 AI 도입 가이드라인 중 개인정보 유출 방지 의무 조항은?",
    "2025년 상반기 사이버 위협 동향 중 가장 지배적으로 발생한 해킹 유형은?",
    "사이버 위협 보고서가 권고하는 공급망 공격 예방을 위한 기본 방어 조치는?",
    "현장조사 생성형 AI 활용 연구에서 제시한 모바일 질의응답 아키텍처 제약은?",
    "생성형 AI 활용 기초연구 중 환각 차단을 위해 적용한 RAG 프롬프트 제약은?",
    "경남대 AI･SW융합전문대학원 시행규정의 특수 인턴십 필수 학점 기준은?",
    "AI･SW융합전문대학원에서 학위청구논문 제출을 위해 충족해야 할 어학 점수는?",
    "생성형 AI 윤리 가이드북에서 정의한 개발자의 3대 책임 의무 사항은?",
    "AI 윤리 가이드북이 명시한 제3자 저작물 데이터 무단 학습 방지 권고는?",
    "SW 공급망 보안 가이드라인 1.0에 따른 SBOM 필수 수록 속성은?",
    "SW 공급망 지침 상 오픈소스 보안 취약점(CVE) 점검 권장 주기는?",
    "RDBMS에 벡터 데이터를 적재할 때 데이터 무결성을 보장하기 위해 적용한 DDL 제약조건은?",
    "경남대 대학원 학칙 중 장기 해외 연수 시 등록금 전액 면제 혜택 조건은?"
]

results = []

def send_get(base_url, params):
    query_string = urllib.parse.urlencode(params)
    url = f"{base_url}?{query_string}"
    req = urllib.request.Request(url, method='GET')
    
    start_time = time.time()
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            resp_body = response.read().decode('utf-8')
            latency = int((time.time() - start_time) * 1000)
            return json.loads(resp_body), latency, True
    except Exception as e:
        print(f"Error calling GET {url}: {e}")
        return None, 0, False

def send_post(url, data_dict):
    data = json.dumps(data_dict).encode('utf-8')
    req = urllib.request.Request(
        url, 
        data=data, 
        headers={'Content-Type': 'application/json'}
    )
    
    start_time = time.time()
    try:
        with urllib.request.urlopen(req, timeout=40) as response:
            resp_body = response.read().decode('utf-8')
            latency = int((time.time() - start_time) * 1000)
            return json.loads(resp_body), latency, True
    except Exception as e:
        print(f"Error calling POST {url}: {e}")
        return None, 0, False

for i, q in enumerate(questions):
    print(f"Running Scenario {i+1}: {q}")
    
    # 1. Retrieval GET /api/v1/search?query=질문&limit=5
    search_data, search_time, search_ok = send_get(
        "http://localhost:8080/api/v1/search",
        {"query": q, "limit": 5}
    )
    time.sleep(0.5)
    
    # 2. RAG QA POST /api/v1/rag/ask
    rag_data, rag_time, rag_ok = send_post(
        "http://localhost:8080/api/v1/rag/ask",
        {"question": q}
    )
    
    # 데이터 추출
    top_score = 0.0
    top_doc = "N/A"
    if search_ok and search_data and len(search_data) > 0:
        top_score = search_data[0].get('similarity', 0.0)
        top_doc = search_data[0].get('documentTitle', 'N/A')
        
    rag_answer = "ERROR"
    if rag_ok and rag_data:
        rag_answer = rag_data.get('answer', 'ERROR')
        
    item = {
        "index": i + 1,
        "question": q,
        "search_success": search_ok,
        "search_latency_ms": search_time,
        "search_count": len(search_data) if search_data else 0,
        "top_search_score": top_score,
        "top_search_document": top_doc,
        "rag_success": rag_ok,
        "rag_latency_ms": rag_time,
        "rag_answer": rag_answer
    }
    
    results.append(item)
    print(f"Done {i+1}: Search {search_time}ms / RAG {rag_time}ms / TopScore: {top_score:.4f} / Doc: {top_doc}")
    time.sleep(1.0)

# 결과 저장
with open("C:\\Users\\pc\\.gemini\\antigravity\\brain\\d7ad8582-64b6-4274-b281-248832aeaeec\\scratch\\results.json", "w", encoding="utf-8") as f:
    json.dump(results, f, ensure_ascii=False, indent=4)

print("Verification completed! Results saved to results.json")
