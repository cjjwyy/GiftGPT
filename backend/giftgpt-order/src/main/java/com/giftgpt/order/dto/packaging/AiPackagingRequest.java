package com.giftgpt.order.dto.packaging;

import lombok.Data;
import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AiPackagingRequest {
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200)
    private String productName;
    private String productCategory;
    private BigDecimal productPrice;
    private String recipientName;
    private String recipientRelation;
    private String occasion;
}
