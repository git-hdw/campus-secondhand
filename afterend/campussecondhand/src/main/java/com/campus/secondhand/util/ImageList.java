package com.campus.secondhand.util;

import java.util.ArrayList;
import java.util.List;

public final class ImageList {
    private ImageList() {}

    public static List<String> split(String images) {
        List<String> out = new ArrayList<>();
        if (images == null || images.isEmpty()) {
            return out;
        }
        String rest = images;
        while (!rest.isEmpty()) {
            if (rest.startsWith("data:")) {
                int meta = rest.indexOf(',');
                if (meta < 0) {
                    out.add(rest.trim());
                    break;
                }
                int sep = rest.indexOf(',', meta + 1);
                if (sep < 0) {
                    out.add(rest.trim());
                    break;
                }
                out.add(rest.substring(0, sep).trim());
                rest = rest.substring(sep + 1);
            } else {
                int sep = rest.indexOf(',');
                if (sep < 0) {
                    out.add(rest.trim());
                    break;
                }
                out.add(rest.substring(0, sep).trim());
                rest = rest.substring(sep + 1);
            }
        }
        out.removeIf(String::isEmpty);
        return out;
    }

    public static String first(String images) {
        List<String> all = split(images);
        return all.isEmpty() ? "" : all.get(0);
    }
}
