package com.giftgpt.enterprise.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BatchOrderResponse {
    private int total;
    private BigDecimal totalAmount;
    private List<Long> giftRecordIds;
    private List<Long> orderIds;
}
