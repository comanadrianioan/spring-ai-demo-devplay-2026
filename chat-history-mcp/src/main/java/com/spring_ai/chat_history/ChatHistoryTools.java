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

    @Tool(name = "recordChatHistory", description = """
            Save a question and its final answer into the semantic cache so that identical or similar \
            questions asked later can be answered instantly without redoing the work. \
            Parameters: 'question' is the user's original query; 'answer' is the response you produced. \
            Returns the ID of the stored record. Call this once, after you have a good answer.""")
    public String recordChatHistory(String question, String answer) {
        String id = UUID.randomUUID().toString();
        Document doc = new Document(id, question, Map.of(
                "answer", answer,
                "timestamp", Instant.now().toString()));
        vectorStore.add(List.of(doc));
        return id;
    }

    @Tool(name = "searchChatHistory", description = """
            Look in the semantic cache for previously answered questions that mean the same thing as the \
            current one, so a cached answer can be reused instead of recomputed. \
            Returns matching question/answer pairs with their timestamp and similarity score. \
            Use this to reuse a previously given answer when the same or a similar question comes up again.""")
    public String searchChatHistory(String question) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(3)
                        .similarityThreshold(0.7)
                        .build());
        List<Map<String, Object>> mapped = results.stream()
                .map(d -> Map.of(
                        "question", d.getText(),
                        "answer", d.getMetadata().getOrDefault("answer", ""),
                        "timestamp", d.getMetadata().getOrDefault("timestamp", ""),
                        "score", d.getScore() != null ? d.getScore() : 0.0))
                .toList();
        try {
            return objectMapper.writeValueAsString(mapped);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize history results", e);
        }
    }
}
