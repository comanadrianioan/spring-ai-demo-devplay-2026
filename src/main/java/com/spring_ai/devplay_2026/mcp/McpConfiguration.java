package com.spring_ai.devplay_2026.mcp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.spring_ai.devplay_2026.tools.ToolsConfiguration;

@Configuration
public class McpConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpConfiguration.class);

    @Bean
    public List<ToolCallback> toolCallbacks(ToolsConfiguration toolsConfiguration) {
        List<ToolCallback> callbacks = List.of(ToolCallbacks.from(toolsConfiguration));
        logger.info("Registered {} MCP tools", callbacks.size());
        return callbacks;
    }

}
