package com.example.identityservice.security;

import com.example.identityservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JWT = Header.Payload.Signature (mỗi phần Base64URL, ngăn bởi dấu chấm)
 *  - Header   : {"alg":"HS256"}           -> JJWT tự sinh khi gọi signWith(...)
 *  - Payload  : {"sub", "role", "iat", "exp"} -> các claim set trong builder
 *  - Signature: HMAC-SHA256(header + "." + payload, secret)
 */
@Component
public class JwtUtil {

    public static final String ROLE_CLAIM = "role";

    private final Key signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt-secret}") String secret,
                   @Value("${jwt-expiration}") long expirationMs) {
        // hmacShaKeyFor sẽ ném WeakKeyException nếu secret < 256 bit
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(user.getUsername())                // claim "sub"
                .claim(ROLE_CLAIM, user.getRole().name())      // custom claim "role"
                .setIssuedAt(now)                              // claim "iat"
                .setExpiration(expiry)                         // claim "exp"
                .signWith(signingKey, SignatureAlgorithm.HS256) // Header alg + Signature
                .compact();
    }

    /** Xác minh chữ ký + hạn dùng, trả về payload. Ném JwtException nếu token không hợp lệ. */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
