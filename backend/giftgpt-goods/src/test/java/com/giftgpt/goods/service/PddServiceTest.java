package com.giftgpt.goods.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PddServiceTest {

    private final PddService service = new PddService();

    @Test
    void shouldParseChineseSalesUnits() {
        assertEquals(125000, service.parseSalesCount("12.5万+"));
        assertEquals(3000, service.parseSalesCount("3千+"));
        assertEquals(987, service.parseSalesCount("已拼987件"));
        assertEquals(0, service.parseSalesCount(""));
    }
}
