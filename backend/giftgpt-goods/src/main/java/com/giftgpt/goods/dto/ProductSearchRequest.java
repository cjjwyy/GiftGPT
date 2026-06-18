package com.giftgpt.goods.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchRequest {

    private String keyword;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sort;
}
