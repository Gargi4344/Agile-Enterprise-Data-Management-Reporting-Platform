package com.aedmp.platform.security;

import com.aedmp.platform.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final SecurityProperties properties;

    private SecretKey key() {
        byte[] bytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes.length >= 32 ? bytes : padded(bytes));
    }

    private byte[] padded(byte[] bytes) {
        byte[] out = new byte[32];
        System.arraycopy(bytes, 0, out, 0, Math.min(bytes.length, 32));
        return out;
    }

    public Instant expirationFor(Instant issuedAt) {
        return issuedAt.plus(properties.expirationMinutes(), ChronoUnit.MINUTES);
    }

    public String issue(AuthenticatedUser principal) {
        Instant now = Instant.now();
        Instant expiry = expirationFor(now);
        List<String> authorities = principal.getUser().permissionCodes().stream().toList();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(principal.getUsername())
                .claim("uid", principal.getId())
                .claim("permissions", authorities)
                .claim("roles", principal.getUser().roleNames().stream().toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
