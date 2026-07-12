package com.giftgpt.order.dto;

import lombok.Data;
import java.util.List;

@Data
public class LogisticsResponse {
    private String orderNo;
    private String status;
    private String logisticsNo;
    private String logisticsCompany;
    private List<Event> events;

    @Data
    public static class Event {
        private String eventTime;
        private String location;
        private String status;
        private String description;
    }
}
