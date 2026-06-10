package com.documentintelligenceapplication.application.service;

import com.documentintelligenceapplication.domain.dto.RagRequest;
import com.documentintelligenceapplication.domain.dto.RagResponse;
import com.documentintelligenceapplication.domain.dto.SearchResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private static final String COMBINED_TEMPLATE = 
        "당신은 문서 지능 플랫폼의 RAG QA 전문 어시스턴트입니다.\n" +
        "반드시 아래 제공되는 'Context' 안의 정보만을 사용하여 사용자의 'Question'에 친절하고 정확하게 답변하십시오.\n" +
        "제공된 Context 내용과 사용자의 질문 내용 사이에 연관성이 없거나 답변할 근거가 부족하다면, " +
        "절대 임의로 지어내지 말고 정확히 다음과 같이 답변하십시오:\n" +
        "'제공된 문서 내에 해당 질문에 답변할 수 있는 관련 정보가 존재하지 않습니다.'\n\n" +
        "Context:\n%s\n\n" +
        "Question:\n%s";

    private final RetrievalService retrievalService;
    private final ChatModel chatModel;

    /**
     * 사용자의 질문에 대한 RAG Q&A 답변을 생성하여 반환합니다.
     * 1. RetrievalService를 통해 질문과 코사인 유사도가 가장 높은 상위 5개 청크 조회
     * 2. 검색 결과 부재 시 조기 우회 반환 (Fast-Path Bypass) 로직 작동
     * 3. 획득한 청크로 문맥 Context 빌딩
     * 4. 시스템 명령어 지침 및 유저 문맥 포맷으로 Prompt 생성 및 ChatModel 호출
     */
    public RagResponse askQuestion(RagRequest request) {
        log.info("Processing RAG QA request for question: '{}'", request.getQuestion());

        // 1. 유사 청크 조회 (K = 5 고정)
        List<SearchResultResponse> chunks = retrievalService.search(request.getQuestion(), 5);

        // 2. 검색 결과 부재 시 조기 우회 반환 로직 적용
        if (chunks.isEmpty()) {
            log.info("No relevant context found. Triggering early bypass response.");
            return new RagResponse(
                    request.getQuestion(), 
                    "제공된 문서 내에 해당 질문에 답변할 수 있는 관련 정보가 존재하지 않습니다."
            );
        }

        // 3. 지식 컨텍스트 구성
        String context = chunks.stream()
                .map(SearchResultResponse::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 4. Prompt 생성 및 ChatModel 명시적 호출
        String promptContent = String.format(COMBINED_TEMPLATE, context, request.getQuestion());
        Prompt prompt = new Prompt(promptContent);

        log.info("Requesting answer generation from OpenAI ChatModel...");
        ChatResponse chatResponse;
        try {
            chatResponse = chatModel.call(prompt);
        } catch (Exception e) {
            log.error("Failed to generate answer from ChatModel for question: '{}'", request.getQuestion(), e);
            throw new RuntimeException("Failed to generate answer from ChatModel", e);
        }

        String answer = chatResponse.getResult().getOutput().getText();
        log.info("Successfully generated RAG QA answer.");

        return new RagResponse(request.getQuestion(), answer);
    }
}
