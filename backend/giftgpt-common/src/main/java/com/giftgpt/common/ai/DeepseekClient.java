// ponytail: 仅 PackagingService 使用；RecommendationService 按并行 KG 决定暂保留自有副本，后续合并
package com.giftgpt.common.ai;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
@Slf4j
@Component
public class DeepseekClient {
    @Value("${giftgpt.ai.deepseek.api-key:}") private String apiKey;
    @Value("${giftgpt.ai.deepseek.base-url:https://api.deepseek.com/v1}") private String baseUrl;
    @Value("${giftgpt.ai.deepseek.model:deepseek-chat}") private String model;
    private static final ObjectMapper M = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }
    public String chat(String system, String prompt, int maxTokens) throws IOException {
        if (!isConfigured()) throw new IOException("Deepseek api-key not configured");
        DeepseekDto.Request req = new DeepseekDto.Request();
        req.setModel(model); req.setMaxTokens(maxTokens);
        DeepseekDto.Message sys = new DeepseekDto.Message();
        sys.setRole("system"); sys.setContent(system);
        DeepseekDto.Message user = new DeepseekDto.Message();
        user.setRole("user"); user.setContent(prompt);
        req.setMessages(List.of(sys, user));
        String body = M.writeValueAsString(req);
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/chat/completions").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true); conn.setConnectTimeout(60000); conn.setReadTimeout(120000);
        try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        int code = conn.getResponseCode();
        if (code != 200) {
            String err = new String(conn.getErrorStream() == null ? new byte[0] : conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("Deepseek API returned " + code + ": " + err);
        }
        String resp = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        DeepseekDto.Response ds = M.readValue(resp, DeepseekDto.Response.class);
        return ds.getChoices().get(0).getMessage().getContent();
    }
    public static String stripMarkdown(String content) {
        String json = content == null ? "" : content.trim();
        if (json.startsWith("```")) {
            int s = json.indexOf("\n") + 1; int e = json.lastIndexOf("```");
            if (e > s) json = json.substring(s, e).trim();
        }
        return json;
    }
}