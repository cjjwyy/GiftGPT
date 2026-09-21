package com.giftgpt.order.dto.packaging;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class SavePackagingRequest {
    @javax.validation.constraints.Positive
    private Long planId;
    @Size(max = 64)
    private String requestKey;
    private Integer version;
    @Size(max = 200)
    private String productName;
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal productPrice;
    @Size(max = 500)
    private String productImageUrl;
    private Long productId;
    @NotBlank(message = "包装主题不能为空")
    @Pattern(regexp = "classic|korean|kraft|luxury|acrylic", message = "包装主题不合法")
    private String packagingType;
    @Size(max = 10)
    private String ribbonText;
    private String ribbonColor;
    private String scent;
    private String photoUrl;
    private String wrappingStyle;
    @Size(max = 50)
    private String customText;
    /** 前端只提交选择项；价格始终由服务端计算。 */
    @Size(max = 6)
    private List<String> customizations;
    private Long recipientId;
    @Pattern(regexp = "birthday|anniversary|valentines|festival|graduation|proposal|mothers_day|fathers_day|teachers_day|christmas|thank_you|daily|visit_patient|family_visit|other",
            message = "场景值不合法")
    private String occasion;
}
