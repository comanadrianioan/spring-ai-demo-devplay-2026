package com.spring_ai.web_search;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Configuration
public class WebSearchMcpConfiguration {

    @Bean
    public List<ToolCallback> toolCallbacks(WebSearchTools tools) {
        return List.of(ToolCallbacks.from(tools));
    }

    @Bean
    public RestTemplate restTemplate(TavilyProperties props) {
        Duration timeout = Duration.ofSeconds(props.getTimeoutSeconds());
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(timeout)
            .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }
}
