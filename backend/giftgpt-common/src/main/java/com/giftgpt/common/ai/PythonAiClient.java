package com.giftgpt.common.ai;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.util.Base64;
import java.util.Map;
@Slf4j
@Component
public class PythonAiClient {
    @Value("${giftgpt.ai.python.base-url:http://localhost:8000}") private String baseUrl;
    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }
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

    public byte[] generateQrCode(String content) {
        String url = baseUrl + "/api/v1/ai/qrcode/generate";
        try {
            ResponseEntity<QrCodeResult> response = restTemplate.postForEntity(
                    url, Map.of("content", content), QrCodeResult.class);
            QrCodeResult body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null
                    || body.getImageBase64() == null || body.getImageBase64().isBlank()) {
                throw new RuntimeException("Python QR service returned an empty image");
            }
            byte[] image = Base64.getDecoder().decode(body.getImageBase64());
            if (image.length == 0 || image.length > 1024 * 1024) {
                throw new RuntimeException("Python QR image size is invalid");
            }
            return image;
        } catch (Exception e) {
            log.warn("Python QR generate failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class GreetingResult { private String content; private String styleTemplate; }
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class QrCodeResult { private String imageBase64; }
}
