package com.campus.secondhand.config;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.UserRepository;
import com.campus.secondhand.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class ApiGateFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public ApiGateFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        User user;
        try { user = current(request.getHeader("Authorization")); }
        catch (org.springframework.dao.DataAccessException e) {
            org.slf4j.LoggerFactory.getLogger(ApiGateFilter.class).error("Authentication store unavailable", e);
            write(response, 503, "服务暂时不可用，请稍后重试");
            return;
        }
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            write(response, 403, "账户已被禁用");
            return;
        }
        String role = user.getRole() != null && user.getRole() == 1 ? "ROLE_ADMIN" : "ROLE_USER";
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    private User current(String header) {
        if (header == null || !header.startsWith("Bearer ")) return null;
        return jwtUtil.resolve(header.substring(7).trim());
    }

    public static void write(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(ApiResponse.httpStatus(code));
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
