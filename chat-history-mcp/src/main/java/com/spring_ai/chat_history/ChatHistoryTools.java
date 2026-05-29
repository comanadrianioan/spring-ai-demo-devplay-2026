package com.spring_ai.chat_history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ChatHistoryTools {

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    public ChatHistoryTools(VectorStore vectorStore, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "recordChatHistory",
          description = "Store a question and its answer in the semantic cache for future reuse.")
    public String recordChatHistory(String question, String answer) {
        String id = UUID.randomUUID().toString();
        Document doc = new Document(id, question, Map.of(
            "answer", answer,
            "timestamp", Instant.now().toString()
        ));
        vectorStore.add(List.of(doc));
        return id;
    }

    @Tool(name = "searchChatHistory",
          description = "Search past Q&A pairs by semantic similarity to the question. Returns matches above minScore.")
    public String searchChatHistory(String question, int topK, double minScore) {
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(minScore)
                .build()
        );
        List<Map<String, Object>> mapped = results.stream()
            .map(d -> Map.of(
                "question", d.getText(),
                "answer", d.getMetadata().getOrDefault("answer", ""),
                "timestamp", d.getMetadata().getOrDefault("timestamp", ""),
                "score", d.getScore() != null ? d.getScore() : 0.0
            ))
            .toList();
        try {
            return objectMapper.writeValueAsString(mapped);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize history results", e);
        }
    }
}
