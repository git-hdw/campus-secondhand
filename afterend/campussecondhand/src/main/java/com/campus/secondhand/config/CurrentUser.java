package com.campus.secondhand.config;
import com.campus.secondhand.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
public final class CurrentUser {
    private CurrentUser() {}
    public static Optional<User> get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user
                ? Optional.of(user) : Optional.empty();
    }
}
