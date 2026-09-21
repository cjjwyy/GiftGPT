package com.giftgpt.common.ai;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftgpt.common.mapper.AiInvocationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final long RETRY_BASE_MS = 500;
    @Value("${giftgpt.ai.deepseek.api-key:}") private String apiKey;
    @Value("${giftgpt.ai.deepseek.base-url:https://api.deepseek.com/v1}") private String baseUrl;
    @Value("${giftgpt.ai.deepseek.model:deepseek-chat}") private String model;
    @Value("${giftgpt.ai.deepseek.max-retries:1}") private int maxRetries;
    @Value("${giftgpt.ai.deepseek.connect-timeout-ms:5000}") private int connectTimeoutMs;
    @Value("${giftgpt.ai.deepseek.read-timeout-ms:30000}") private int readTimeoutMs;
    @Autowired(required = false)
    private AiInvocationLogMapper invocationLogMapper;
    private static final ObjectMapper M = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    public String chat(String system, String prompt, int maxTokens) throws IOException {
        long startedAt = System.currentTimeMillis();
        IOException last = new IOException("Deepseek call failed without response");
        int retryLimit = Math.max(0, Math.min(maxRetries, 3));
        for (int attempt = 0; attempt <= retryLimit; attempt++) {
            try {
                ChatResult result = doChat(system, prompt, maxTokens);
                long latencyMs = System.currentTimeMillis() - startedAt;
                log.info("ai_invocation model={} latencyMs={} success=true fallback=false",
                        model, latencyMs);
                saveInvocation(result.usage, latencyMs, true, false, null);
                return result.content;
            } catch (IOException e) {
                last = e;
                if (attempt == retryLimit || !retryable(e)) {
                    break;
                }
                log.warn("Deepseek call failed, retry {}/{}: {}", attempt + 1, retryLimit, safeMessage(e));
                sleepBeforeRetry(attempt);
            }
        }
        long latencyMs = System.currentTimeMillis() - startedAt;
        log.info("ai_invocation model={} latencyMs={} success=false fallback=true",
                model, latencyMs);
        saveInvocation(null, latencyMs, false, true, last.getClass().getSimpleName());
        throw last;
    }

    private ChatResult doChat(String system, String prompt, int maxTokens) throws IOException {
        if (!isConfigured()) throw new IOException("Deepseek api-key not configured");
        DeepseekDto.Request req = new DeepseekDto.Request();
        req.setModel(model); req.setMaxTokens(Math.max(256, Math.min(maxTokens, 8192)));
        DeepseekDto.Message sys = new DeepseekDto.Message();
        sys.setRole("system"); sys.setContent(system);
        DeepseekDto.Message user = new DeepseekDto.Message();
        user.setRole("user"); user.setContent(prompt);
        req.setMessages(List.of(sys, user));
        String body = M.writeValueAsString(req);
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        HttpURLConnection conn = (HttpURLConnection) new URL(normalizedBaseUrl + "/chat/completions").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(Math.max(1000, connectTimeoutMs));
        conn.setReadTimeout(Math.max(1000, readTimeoutMs));
        try {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            InputStream stream = code == 200 ? conn.getInputStream() : conn.getErrorStream();
            String response;
            if (stream == null) {
                response = "";
            } else {
                try (InputStream in = stream) {
                    response = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            if (code != 200) {
                throw new HttpStatusIOException(code, abbreviate(response, 300));
            }
            DeepseekDto.Response ds = M.readValue(response, DeepseekDto.Response.class);
            if (ds == null || ds.getChoices() == null || ds.getChoices().isEmpty()
                    || ds.getChoices().get(0).getMessage() == null
                    || ds.getChoices().get(0).getMessage().getContent() == null
                    || ds.getChoices().get(0).getMessage().getContent().isBlank()) {
                throw new IOException("Deepseek returned empty choices");
            }
            return new ChatResult(ds.getChoices().get(0).getMessage().getContent(), ds.getUsage());
        } finally {
            conn.disconnect();
        }
    }

    private boolean retryable(IOException e) {
        if (e instanceof HttpStatusIOException) {
            int status = ((HttpStatusIOException) e).status;
            return status == 408 || status == 429 || status >= 500;
        }
        String msg = e.getMessage();
        return msg == null || !msg.contains("api-key not configured");
    }

    private String safeMessage(IOException e) {
        return abbreviate(e.getMessage(), 300);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "...";
    }

    private static class HttpStatusIOException extends IOException {
        private final int status;
        private HttpStatusIOException(int status, String body) {
            super("Deepseek API returned " + status + ": " + body);
            this.status = status;
        }
    }

    private static class ChatResult {
        private final String content;
        private final DeepseekDto.Usage usage;

        private ChatResult(String content, DeepseekDto.Usage usage) {
            this.content = content;
            this.usage = usage;
        }
    }

    private void saveInvocation(DeepseekDto.Usage usage, long latencyMs,
                                boolean success, boolean fallback, String errorType) {
        if (invocationLogMapper == null) {
            return;
        }
        try {
            AiInvocationLog invocation = new AiInvocationLog();
            invocation.setScene("deepseek_chat");
            invocation.setModel(model);
            invocation.setPromptTokens(usage == null ? 0 : usage.getPromptTokens());
            invocation.setCompletionTokens(usage == null ? 0 : usage.getCompletionTokens());
            invocation.setLatencyMs(latencyMs);
            invocation.setSuccess(success ? 1 : 0);
            invocation.setFallback(fallback ? 1 : 0);
            invocation.setErrorType(errorType);
            invocationLogMapper.insert(invocation);
        } catch (Exception e) {
            log.warn("Failed to persist AI invocation metrics: {}", e.getMessage());
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(RETRY_BASE_MS * (1L << attempt));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
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
