package com.hoang.jwtjava.service;

/**
 * Free shipping when product subtotal &gt;= 500_000 VND; otherwise flat 30_000.
 */
public final class ShippingFeeCalculator {

    public static final int FREE_SHIPPING_THRESHOLD = 500_000;
    public static final int STANDARD_SHIPPING_FEE = 30_000;

    private ShippingFeeCalculator() {
    }

    public static int calculate(int subtotal) {
        if (subtotal < 0)
            return STANDARD_SHIPPING_FEE;
        return subtotal < FREE_SHIPPING_THRESHOLD ? STANDARD_SHIPPING_FEE : 0;
    }
}
