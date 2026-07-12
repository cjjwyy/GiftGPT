package com.giftgpt.common.ai;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
public class DeepseekDto {
    @Data public static class Message { private String role; private String content; }
    @Data public static class Request {
        private String model;
        private List<Message> messages;
        private double temperature = 0.7;
        @JsonProperty("max_tokens") private int maxTokens = 2048;
        @JsonProperty("stream") private boolean stream = false;
    }
    @Data public static class Choice { private Message message; }
    @Data public static class Response { private List<Choice> choices; }
}