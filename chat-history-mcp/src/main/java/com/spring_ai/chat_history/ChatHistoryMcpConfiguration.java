package com.spring_ai.chat_history;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatHistoryMcpConfiguration {

    @Bean
    public List<ToolCallback> toolCallbacks(ChatHistoryTools tools) {
        return List.of(ToolCallbacks.from(tools));
    }
}
