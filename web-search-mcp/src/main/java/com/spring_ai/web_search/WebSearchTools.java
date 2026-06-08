package com.spring_ai.web_search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebSearchTools {

    @Autowired
    private TavilyClient tavilyClient;
    @Autowired
    private ObjectMapper objectMapper;

    @Tool(name = "searchWeb",
          description = """
                  Search the public internet for current, real-time, or niche information. \
                  Parameters: 'query' is the search text; 'maxResults' caps how many results to return. \
                  Returns a list of web results with titles, URLs, and content snippets. \
                  Use this when you need up-to-date information from the open web.""")
    public String searchWeb(String query, int maxResults) {
        try {
            return objectMapper.writeValueAsString(tavilyClient.search(query, maxResults));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize search results", e);
        }
    }
}
