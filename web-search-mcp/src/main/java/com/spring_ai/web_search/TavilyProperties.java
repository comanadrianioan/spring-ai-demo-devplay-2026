package com.spring_ai.web_search;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tavily")
public class TavilyProperties {
    private String apiKey;
    private String baseUrl = "https://api.tavily.com";
    private int timeoutSeconds = 10;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
