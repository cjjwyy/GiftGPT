package com.giftgpt.order.dto.packaging;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SavePackagingRequest {
    private String productName;
    private BigDecimal productPrice;
    private String productImageUrl;
    private Long productId;
    private String packagingType;
    private String ribbonText;
    private String ribbonColor;
    private String scent;
    private String photoUrl;
    private String wrappingStyle;
    private String customText;
    private BigDecimal price;
    private Long recipientId;
    private String occasion;
}
