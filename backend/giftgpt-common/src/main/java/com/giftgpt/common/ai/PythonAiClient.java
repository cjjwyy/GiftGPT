package com.giftgpt.common.ai;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
@Slf4j
@Component
public class PythonAiClient {
    @Value("${giftgpt.ai.python.base-url:http://localhost:8000}") private String baseUrl;
    private final RestTemplate restTemplate = new RestTemplate();
    public GreetingResult generateGreeting(Map<String, Object> payload) {
        String url = baseUrl + "/api/v1/ai/greeting/generate";
        try {
            ResponseEntity<GreetingResult> resp = restTemplate.postForEntity(url, payload, GreetingResult.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) return resp.getBody();
            throw new RuntimeException("Python AI non-2xx: " + resp.getStatusCode());
        } catch (Exception e) {
            log.warn("Python greeting generate failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class GreetingResult { private String content; private String styleTemplate; }
}