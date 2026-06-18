package com.giftgpt.common.enums;

import lombok.Getter;

@Getter
public enum OccasionEnum {
    BIRTHDAY("birthday", "生日"),
    ANNIVERSARY("anniversary", "纪念日"),
    FESTIVAL("festival", "节庆"),
    PROPOSAL("proposal", "求婚"),
    GRADUATION("graduation", "毕业"),
    MOTHERS_DAY("mothers_day", "母亲节"),
    FATHERS_DAY("fathers_day", "父亲节"),
    TEACHERS_DAY("teachers_day", "教师节"),
    VALENTINES("valentines", "情人节"),
    CHRISTMAS("christmas", "圣诞"),
    THANK_YOU("thank_you", "感谢"),
    OTHER("other", "其他"),
    ;

    private final String code;
    private final String desc;

    OccasionEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
