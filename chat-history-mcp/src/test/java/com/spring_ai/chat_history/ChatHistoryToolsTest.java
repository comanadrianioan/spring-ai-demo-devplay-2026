package com.spring_ai.chat_history;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatHistoryToolsTest {

    @Mock
    VectorStore vectorStore;

    ChatHistoryTools tools;

    @BeforeEach
    void setUp() {
        tools = new ChatHistoryTools(vectorStore, new ObjectMapper());
    }

    @Test
    void recordReturnsUuid() {
        String id = tools.recordChatHistory("What is DevPlay?", "A game dev conference in Bucharest.");
        assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        verify(vectorStore, times(1)).add(any());
    }

    @Test
    void recordStoresQuestionAsDocumentText() {
        tools.recordChatHistory("What is DevPlay?", "A game dev conference.");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        Document stored = captor.getValue().get(0);
        assertThat(stored.getText()).isEqualTo("What is DevPlay?");
        assertThat(stored.getMetadata()).containsKey("answer");
        assertThat(stored.getMetadata()).containsKey("timestamp");
    }

    @Test
    void searchAppliesMinScore() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        tools.searchChatHistory("DevPlay?", 3, 0.85);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getSimilarityThreshold()).isEqualTo(0.85);
        assertThat(captor.getValue().getTopK()).isEqualTo(3);
    }
}
