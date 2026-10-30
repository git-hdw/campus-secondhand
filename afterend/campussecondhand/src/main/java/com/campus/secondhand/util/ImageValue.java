package com.campus.secondhand.util;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.imageio.ImageIO;

/** Validates stored image references; never fetches remote addresses. */
public final class ImageValue {
    private ImageValue() {}
    public static void validateList(String images) {
        if (images == null || images.isBlank()) return;
        if (images.getBytes(StandardCharsets.UTF_8).length > 200000) throw new IllegalArgumentException("图片内容过大");
        var values = ImageList.split(images);
        if (values.size() > 9) throw new IllegalArgumentException("最多上传 9 张图片");
        values.forEach(ImageValue::validate);
    }
    public static void validate(String value) {
        if (value == null || value.isBlank()) return;
        if (value.startsWith("/") && !value.startsWith("//") && !value.contains("\\")) return;
        if (!value.startsWith("data:")) {
            try {
                URI uri = URI.create(value);
                if (("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                        && uri.getHost() != null && uri.getUserInfo() == null) return;
            } catch (IllegalArgumentException ignored) { }
            throw new IllegalArgumentException("图片地址格式不正确");
        }
        int comma = value.indexOf(',');
        if (comma < 0) throw new IllegalArgumentException("图片数据格式不正确");
        String metadata = value.substring(0, comma);
        if (!java.util.Set.of("data:image/jpeg;base64", "data:image/png;base64", "data:image/webp;base64").contains(metadata))
            throw new IllegalArgumentException("仅支持 JPG、PNG、WebP 图片");
        try {
            byte[] bytes = Base64.getDecoder().decode(value.substring(comma + 1));
            if (bytes.length == 0 || bytes.length > 150000) throw new IllegalArgumentException("图片内容过大或为空");
            if (metadata.contains("webp")) {
                validateWebp(bytes);
                return;
            }
            try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw new IllegalArgumentException("图片内容无效");
                var reader = readers.next();
                try {
                    reader.setInput(stream);
                    String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    if (!(metadata.contains("jpeg") && format.equals("jpeg")) && !(metadata.contains("png") && format.equals("png")))
                        throw new IllegalArgumentException("图片类型与内容不一致");
                    checkDimensions(reader.getWidth(0), reader.getHeight(0));
                } finally { reader.dispose(); }
            }
        } catch (java.io.IOException | IllegalArgumentException e) { throw new IllegalArgumentException("图片数据无效或尺寸过大"); }
    }
    private static void checkDimensions(long width, long height) {
        if (width <= 0 || height <= 0 || width > 8192 || height > 8192 || width * height > 20000000)
            throw new IllegalArgumentException("图片尺寸过大");
    }
    private static long little(byte[] b, int offset, int length) {
        long n = 0;
        for (int i = 0; i < length; i++) n |= (long) (b[offset + i] & 255) << (i * 8);
        return n;
    }
    private static void validateWebp(byte[] b) {
        if (b.length < 30 || !new String(b, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                || !new String(b, 8, 4, StandardCharsets.US_ASCII).equals("WEBP") || little(b, 4, 4) + 8 != b.length)
            throw new IllegalArgumentException("WebP 数据无效");
        String kind = new String(b, 12, 4, StandardCharsets.US_ASCII);
        if (kind.equals("VP8X")) checkDimensions(1 + little(b, 24, 3), 1 + little(b, 27, 3));
        else if (kind.equals("VP8L") && b[20] == 0x2f) {
            long bits = little(b, 21, 4);
            checkDimensions((bits & 16383) + 1, ((bits >> 14) & 16383) + 1);
        } else if (kind.equals("VP8 ") && (b[23] & 255) == 157 && b[24] == 1 && b[25] == 42)
            checkDimensions(little(b, 26, 2) & 16383, little(b, 28, 2) & 16383);
        else throw new IllegalArgumentException("WebP 数据无效");
    }
}
