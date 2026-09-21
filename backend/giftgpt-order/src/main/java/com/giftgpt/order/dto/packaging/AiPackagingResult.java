package com.giftgpt.order.dto.packaging;

import lombok.Data;

@Data
public class AiPackagingResult {
    private boolean aiGenerated;
    private String packagingType;
    private String ribbonText;
    private String ribbonColor;
    private String scent;
    private String wrappingStyle;
    private String reason;
}
