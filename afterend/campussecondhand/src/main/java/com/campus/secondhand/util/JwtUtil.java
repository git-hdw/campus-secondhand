package com.campus.secondhand.util;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
@Component
public class JwtUtil {
    private final long expiration;
    private final SecretKey key;
    private final UserRepository users;
    private final com.campus.secondhand.service.SessionRegistry sessions;
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration, UserRepository users, com.campus.secondhand.service.SessionRegistry sessions) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.users = users;
        this.sessions = sessions;
    }
    // Bind tokens to immutable identity and current credentials without exposing the password hash.
    public String generateToken(User user) {
        Date now = new Date();
        String id = java.util.UUID.randomUUID().toString();
        Date expires = new Date(now.getTime() + expiration * 1000);
        String token = Jwts.builder().setSubject(user.getId().toString()).setId(id).claim("v", 3)
                .claim("credential", credential(user)).setIssuedAt(now)
                .setExpiration(expires)
                .signWith(key, SignatureAlgorithm.HS512).compact();
        sessions.register(user.getId(), id, expires);
        return token;
    }
    public User resolve(String token) {
        Claims claims;
        long id;
        try {
            claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            if (!Integer.valueOf(3).equals(claims.get("v", Integer.class))) return null;
            id = Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) { return null; }
        User user = users.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || user.getPassword() == null) return null;
        if (!sessions.active(id, claims.getId())) return null;
        String signature = claims.get("credential", String.class);
        return signature != null && MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8),
                credential(user).getBytes(StandardCharsets.UTF_8)) ? user : null;
    }
    public String tokenId(String header) {
        if (header == null || !header.startsWith("Bearer ")) throw new IllegalArgumentException("缺少会话凭据");
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(header.substring(7).trim()).getBody().getId();
    }
    private String credential(User user) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(
                    (user.getId() + ":" + user.getPassword()).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException("Credential signing unavailable", e); }
    }
}
