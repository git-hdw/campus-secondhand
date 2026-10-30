package com.campus.secondhand.util;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RequestValueTest {
    @Test void rejectsMissingFractionalNegativeAndOverflowIds() {
        for (Object value : new Object[]{null, -1, 0, 1.2, "9223372036854775808", "oops"})
            assertThrows(IllegalArgumentException.class, () -> RequestValue.positiveId(value));
        assertEquals(9L, RequestValue.positiveId("9"));
    }
    @Test void textDoesNotSilentlyCoerceObjects() {
        assertThrows(IllegalArgumentException.class, () -> RequestValue.text(java.util.Map.of("value", "title")));
        assertEquals("title", RequestValue.text("title"));
    }
}
