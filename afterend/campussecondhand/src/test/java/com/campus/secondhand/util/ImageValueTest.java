package com.campus.secondhand.util;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ImageValueTest {
    @Test void acceptsExistingReferencesAndSmallPng() {
        assertDoesNotThrow(() -> ImageValue.validateList("/images/demo.png,https://example.com/photo.webp"));
        assertDoesNotThrow(() -> ImageValue.validate("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j3ioAAAAASUVORK5CYII="));
    }
    @Test void rejectsFakeImageDataAndUnsafeSchemes() {
        for (String value : new String[]{"javascript:alert(1)", "data:image/svg+xml;base64,PHN2Zz4=", "data:image/png;base64,ZmFrZQ==", "data:image/jpeg;base64,%%%"})
            assertThrows(IllegalArgumentException.class, () -> ImageValue.validate(value));
    }
}
