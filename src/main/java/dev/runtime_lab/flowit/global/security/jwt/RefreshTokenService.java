package dev.runtime_lab.flowit.global.security.jwt;

import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import dev.runtime_lab.flowit.global.security.jwt.element.RefreshToken;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final String KEY_PREFIX = "flowit:auth:refresh:";
	private static final int TOKEN_BYTES = 32;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final StringRedisTemplate stringRedisTemplate;
	private final JwtProperties jwtProperties;

	public RefreshToken issue(String userId) {
		String token = generateToken();
		String key = keyOf(token);

		stringRedisTemplate.opsForValue()
			.set(key, userId, jwtProperties.refreshTokenTimeToLive());

		return new RefreshToken(token, jwtProperties.refreshTokenTimeToLive().toSeconds());
	}

	public Optional<String> consume(String token) {
		String userId = stringRedisTemplate.opsForValue().getAndDelete(keyOf(token));

		return Optional.ofNullable(userId);
	}

	public boolean revoke(String token) {
		return Boolean.TRUE.equals(stringRedisTemplate.delete(keyOf(token)));
	}

	String keyOf(String token) {
		return KEY_PREFIX + hash(token);
	}

	private String generateToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(bytes);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String token) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] digest = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));

			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 message digest is unavailable", exception);
		}
	}
}
