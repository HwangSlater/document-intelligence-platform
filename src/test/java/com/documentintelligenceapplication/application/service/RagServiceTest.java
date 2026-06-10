package com.documentintelligenceapplication.application.service;

import com.documentintelligenceapplication.domain.dto.RagRequest;
import com.documentintelligenceapplication.domain.dto.RagResponse;
import com.documentintelligenceapplication.domain.dto.SearchResultResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private RetrievalService retrievalService;

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private RagService ragService;

    @Test
    @DisplayName("조회된 청크가 전혀 없는 경우 ChatModel을 호출하지 않고 조기 우회 반환 응답을 돌려준다")
    void askQuestion_BypassWhenNoChunks() {
        // Given
        RagRequest request = new RagRequest();
        request.setQuestion("Unrelated question");

        // Retrieval에서 빈 청크 목록 반환
        when(retrievalService.search(eq("Unrelated question"), eq(5))).thenReturn(Collections.emptyList());

        // When
        RagResponse response = ragService.askQuestion(request);

        // Then
        assertThat(response.getQuestion()).isEqualTo("Unrelated question");
        assertThat(response.getAnswer()).isEqualTo("제공된 문서 내에 해당 질문에 답변할 수 있는 관련 정보가 존재하지 않습니다.");
        
        // ChatModel이 전혀 기동되지 않았음을 검증
        verifyNoInteractions(chatModel);
        verify(retrievalService, times(1)).search(eq("Unrelated question"), eq(5));
    }

    @Test
    @DisplayName("검색된 컨텍스트 청크가 존재할 때 프롬프트를 구성해 ChatModel을 호출하고 최종 답변을 생성한다")
    void askQuestion_Success() {
        // Given
        RagRequest request = new RagRequest();
        request.setQuestion("What is Spring AI?");

        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        SearchResultResponse mockChunk = SearchResultResponse.builder()
                .chunkId(chunkId)
                .documentId(docId)
                .content("Spring AI is a framework for AI integration.")
                .similarity(0.95)
                .build();

        when(retrievalService.search(eq("What is Spring AI?"), eq(5))).thenReturn(List.of(mockChunk));

        // Mock ChatModel Response
        org.springframework.ai.chat.messages.AssistantMessage assistantMessage = 
                new org.springframework.ai.chat.messages.AssistantMessage("Spring AI is a framework designed for integrating AI services in Spring apps.");
        Generation generation = new Generation(assistantMessage);
        ChatResponse mockChatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);

        // When
        RagResponse response = ragService.askQuestion(request);

        // Then
        assertThat(response.getQuestion()).isEqualTo("What is Spring AI?");
        assertThat(response.getAnswer()).isEqualTo("Spring AI is a framework designed for integrating AI services in Spring apps.");

        verify(retrievalService, times(1)).search(eq("What is Spring AI?"), eq(5));
        verify(chatModel, times(1)).call(any(Prompt.class));
    }
}
