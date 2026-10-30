package com.campus.secondhand.config;

import jakarta.servlet.http.HttpServletRequest;

public final class PublicApi {
    private PublicApi() {}

    public static boolean matches(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("OPTIONS".equals(method)) return true;
        if ("POST".equals(method)) {
            return "/api/auth/login".equals(path)
                    || "/api/auth/register".equals(path)
                    || "/api/auth/forgot-password".equals(path);
        }
        if (!"GET".equals(method)) return false;
        if ("/api/stats".equals(path)
                || "/api/products/list".equals(path)
                || "/api/products/search".equals(path)
                || "/api/categories".equals(path)) {
            return true;
        }
        if (path.startsWith("/api/products/category/") || path.startsWith("/api/categories/")) return true;
        return path.matches("/api/products/\\d+") || path.matches("/api/user/user/\\d+");
    }
}
