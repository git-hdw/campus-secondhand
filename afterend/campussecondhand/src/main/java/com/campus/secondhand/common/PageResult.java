package com.campus.secondhand.common;
import java.util.List;
public record PageResult<T>(List<T> items, long total, long page, long pageSize) {
    public static int size(Integer value) { return value == null ? 20 : Math.max(1, Math.min(50, value)); }
    public static int number(Integer value) { return value == null ? 1 : Math.max(1, value); }
}
