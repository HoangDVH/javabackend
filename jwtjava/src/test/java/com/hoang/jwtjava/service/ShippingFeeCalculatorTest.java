package com.hoang.jwtjava.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShippingFeeCalculatorTest {

    @Test
    void underThresholdPaysShipping() {
        assertEquals(30_000, ShippingFeeCalculator.calculate(400_000));
        assertEquals(30_000, ShippingFeeCalculator.calculate(499_999));
        assertEquals(30_000, ShippingFeeCalculator.calculate(0));
    }

    @Test
    void atOrAboveThresholdFreeShipping() {
        assertEquals(0, ShippingFeeCalculator.calculate(500_000));
        assertEquals(0, ShippingFeeCalculator.calculate(1_000_000));
    }
}
