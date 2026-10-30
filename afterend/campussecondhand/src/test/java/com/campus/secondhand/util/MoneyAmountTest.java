package com.campus.secondhand.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MoneyAmountTest {
    @Test void rejectsValuesThatCannotBeStoredWithoutLoss() {
        for (Object value : new Object[]{"NaN", "1.234", "100000000", "-1", "0"}) {
            assertThrows(IllegalArgumentException.class, () -> MoneyAmount.parse(value, false));
        }
        assertThrows(IllegalArgumentException.class, () -> MoneyAmount.parse(null, false));
    }
    @Test void allowsExactCentsAndOptionalZeroOriginalPrice() {
        assertEquals(new BigDecimal("0.01"), MoneyAmount.parse("0.01", false));
        assertEquals(new BigDecimal("99999999.99"), MoneyAmount.parse("99999999.99", false));
        assertEquals(BigDecimal.ZERO, MoneyAmount.parse("0", true));
        assertEquals(0, MoneyAmount.parse("25.000", false).compareTo(new BigDecimal("25")));
    }
}
