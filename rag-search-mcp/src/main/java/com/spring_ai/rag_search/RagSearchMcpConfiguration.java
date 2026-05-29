package com.spring_ai.rag_search;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RagSearchMcpConfiguration {

    @Bean
    public List<ToolCallback> toolCallbacks(RagSearchTools tools) {
        return List.of(ToolCallbacks.from(tools));
    }
}
