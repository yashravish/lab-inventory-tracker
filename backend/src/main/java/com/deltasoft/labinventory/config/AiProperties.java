package com.deltasoft.labinventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("anthropic")
public class AiProperties {

    private String apiKey = "";
    private String model = "claude-haiku-4-5-20251001";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey == null ? "" : apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
