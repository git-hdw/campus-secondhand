package com.campus.secondhand.util;

import java.math.BigDecimal;

public final class MoneyAmount {
    private static final BigDecimal MAX = new BigDecimal("99999999.99");
    private MoneyAmount() {}

    public static BigDecimal parse(Object raw, boolean allowZero) {
        if (raw == null) throw new IllegalArgumentException("请填写价格");
        BigDecimal value;
        try {
            value = new BigDecimal(raw.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("价格格式不正确");
        }
        if (value.signum() < 0 || (!allowZero && value.signum() == 0)
                || value.compareTo(MAX) > 0 || value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("价格需要在 " + (allowZero ? "0" : "0.01") + " 到 99999999.99 之间，最多两位小数");
        }
        return value;
    }
}
