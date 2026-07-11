package com.giftgpt.order.dto.packaging;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PackagingTheme {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String icon;
}
