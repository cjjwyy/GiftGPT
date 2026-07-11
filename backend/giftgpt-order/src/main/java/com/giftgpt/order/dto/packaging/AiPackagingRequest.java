package com.giftgpt.order.dto.packaging;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AiPackagingRequest {
    private String productName;
    private String productCategory;
    private BigDecimal productPrice;
    private String recipientName;
    private String recipientRelation;
    private String occasion;
}
