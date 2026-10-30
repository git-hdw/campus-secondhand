package com.campus.secondhand.util;

import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.UserRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtUtilTest {
    private final UserRepository users = mock(UserRepository.class);
    private final com.campus.secondhand.service.SessionRegistry sessions = mock(com.campus.secondhand.service.SessionRegistry.class);
    private final JwtUtil jwt = new JwtUtil("test-only-signing-key-".repeat(5), 3600, users, sessions);
    private User user() {
        User user = new User(); user.setId(7L); user.setUsername("before"); user.setPassword("test-password-hash");
        when(users.selectById(7L)).thenReturn(user);
        when(sessions.active(eq(7L), anyString())).thenReturn(true);
        return user;
    }
    @Test void renameDoesNotRebindIdentity() {
        User user = user(); String token = jwt.generateToken(user);
        user.setUsername("after");
        assertSame(user, jwt.resolve(token));
        verify(users, never()).findByUsername(any());
    }
    @Test void passwordChangeRevokesOldTokens() {
        User user = user(); String token = jwt.generateToken(user);
        user.setPassword("changed-hash");
        assertNull(jwt.resolve(token));
        assertSame(user, jwt.resolve(jwt.generateToken(user)));
    }
    @Test void deletedUserIsRejected() {
        User user = user(); String token = jwt.generateToken(user); user.setDeleted(1);
        assertNull(jwt.resolve(token));
    }
    @Test void invalidAndExpiredTokensAreRejected() {
        assertNull(jwt.resolve("invalid"));
        User user = user();
        JwtUtil expired = new JwtUtil("test-only-signing-key-".repeat(5), -60, users, sessions);
        assertNull(jwt.resolve(expired.generateToken(user)));
    }
    @Test void legacyUsernameTokensAreRejected() {
        String legacy = io.jsonwebtoken.Jwts.builder().setSubject("before")
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("test-only-signing-key-".repeat(5).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .compact();
        assertNull(jwt.resolve(legacy));
        verifyNoInteractions(users);
    }
}
