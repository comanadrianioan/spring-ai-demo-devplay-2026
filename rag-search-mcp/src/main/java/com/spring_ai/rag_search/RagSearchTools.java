package com.spring_ai.rag_search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RagSearchTools {

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    public RagSearchTools(VectorStore vectorStore, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "searchWiki",
          description = "Search the DevPlay knowledge base using semantic similarity. Returns relevant text chunks.")
    public String searchWiki(String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.builder().query(query).topK(topK).build()
        );
        List<Map<String, Object>> mapped = results.stream()
            .map(d -> Map.of(
                "text", d.getText(),
                "source", d.getMetadata().getOrDefault("source", "unknown"),
                "score", d.getScore() != null ? d.getScore() : 0.0
            ))
            .toList();
        try {
            return objectMapper.writeValueAsString(mapped);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize search results", e);
        }
    }
}
