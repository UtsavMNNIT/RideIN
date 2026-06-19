package com.rideflow.gateway.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Validates HS256 JWTs against a trusted keyset and extracts the caller identity.
 *
 * <p>A token is accepted iff it verifies under at least one configured key
 * <em>and</em> (when an allow-list is configured) carries an accepted issuer.
 * Trying multiple keys lets a single gateway front auth services that sign with
 * different secrets, without weakening verification — an attacker still needs a
 * key we trust.
 *
 * <p>HMAC verification is CPU-bound, not I/O, so running it inline on the event
 * loop is fine; there is no reactive call to make here.
 */
@Component
public class JwtValidator {

    private final List<JwtParser> parsers;
    private final Set<String>     allowedIssuers;

    public JwtValidator(JwtProperties props) {
        this.allowedIssuers = Set.copyOf(props.allowedIssuers());
        this.parsers = props.secrets().stream()
                .map(JwtValidator::keyFrom)
                .map(key -> Jwts.parser().verifyWith(key).build())
                .toList();
    }

    /**
     * @throws JwtValidationException if the token fails signature, expiry, or
     *         issuer checks under every trusted key
     */
    public AuthenticatedUser validate(String token) {
        Claims claims = parseWithAnyKey(token);

        if (!allowedIssuers.isEmpty() && !allowedIssuers.contains(claims.getIssuer())) {
            throw new JwtValidationException("untrusted token issuer: " + claims.getIssuer());
        }

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtValidationException("token has no subject");
        }

        return new AuthenticatedUser(
                subject,
                claims.get("role", String.class),
                claims.get("email", String.class));
    }

    private Claims parseWithAnyKey(String token) {
        SignatureException lastSignatureFailure = null;

        for (JwtParser parser : parsers) {
            try {
                Jws<Claims> jws = parser.parseSignedClaims(token);
                return jws.getPayload();
            } catch (ExpiredJwtException e) {
                // Verified under this key but past expiry — definitive, don't retry.
                throw new JwtValidationException("token expired");
            } catch (SignatureException e) {
                // Wrong key for this token — try the next trusted key.
                lastSignatureFailure = e;
            } catch (JwtValidationException e) {
                throw e;
            } catch (Exception e) {
                // Malformed / unsupported / illegal token — not retryable.
                throw new JwtValidationException("invalid token: " + e.getMessage());
            }
        }

        throw new JwtValidationException(
                lastSignatureFailure != null ? "token signature does not match any trusted key"
                                             : "token could not be verified");
    }

    private static SecretKey keyFrom(String secret) {
        // Throws WeakKeyException if < 256 bits — fail fast at startup, same as the issuers.
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
