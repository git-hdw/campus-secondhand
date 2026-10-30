package com.campus.secondhand.util;
public final class RequestValue {
    private RequestValue() {}
    public static Long positiveId(Object raw) {
        if (raw == null) throw new IllegalArgumentException("请提供有效 ID");
        try {
            long id = new java.math.BigDecimal(raw.toString()).longValueExact();
            if (id <= 0) throw new IllegalArgumentException("ID 必须为正整数");
            return id;
        } catch (ArithmeticException | NumberFormatException e) { throw new IllegalArgumentException("ID 必须为正整数"); }
    }
    public static String text(Object raw) {
        if (raw == null) return null;
        if (!(raw instanceof String value)) throw new IllegalArgumentException("文本字段格式不正确");
        return value;
    }
}
