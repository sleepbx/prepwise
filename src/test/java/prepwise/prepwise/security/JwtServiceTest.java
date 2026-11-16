package prepwise.prepwise.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";
    private static final long ONE_DAY_MS = 86_400_000L;

    private final UserDetails alice = User.withUsername("alice@example.com").password("x").authorities("USER").build();
    private final UserDetails bob = User.withUsername("bob@example.com").password("x").authorities("USER").build();

    private JwtService jwtService(String secret) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expirationMs", ONE_DAY_MS);
        return service;
    }

    @Test
    void issuedTokenCarriesEmailAndValidatesForTheSameUser() {
        JwtService jwt = jwtService(SECRET);
        String token = jwt.generateToken(alice);

        assertThat(jwt.extractEmail(token)).isEqualTo("alice@example.com");
        assertThat(jwt.isTokenValid(token, alice)).isTrue();
        assertThat(jwt.isTokenValid(token, bob)).isFalse();
    }

    @Test
    void tokenExpiresAfterTwentyFourHours() {
        JwtService jwt = jwtService(SECRET);
        String token = jwt.generateToken(alice);

        long issued = jwt.extractClaim(token, Claims::getIssuedAt).getTime();
        long expires = jwt.extractClaim(token, Claims::getExpiration).getTime();

        assertThat(expires - issued).isEqualTo(ONE_DAY_MS);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = jwtService(SECRET);
        String token = jwt.generateToken(alice);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> jwt.extractEmail(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = jwtService(SECRET).generateToken(alice);
        JwtService other = jwtService("a-completely-different-secret-of-32-bytes+");

        assertThatThrownBy(() -> other.extractEmail(token)).isInstanceOf(JwtException.class);
    }
}
