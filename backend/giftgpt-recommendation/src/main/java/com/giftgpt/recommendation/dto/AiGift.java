package com.giftgpt.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiGift {

    @NotBlank(message = "礼物名称不能为空")
    @Size(max = 120, message = "礼物名称不能超过120个字符")
    private String name;
    @PositiveOrZero(message = "候选价格不能为负数")
    private double price;
    @Size(max = 120, message = "推荐理由不能超过120个字符")
    private String reason;
    @Size(max = 8, message = "匹配标签不能超过8个")
    private List<String> tags;
    private String platform;
    /** 关键词权重（关键词推荐模型）：权重越高越优先搜索 */
    private double weight = 1.0;
}
