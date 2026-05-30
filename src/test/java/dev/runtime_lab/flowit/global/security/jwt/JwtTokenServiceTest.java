package dev.runtime_lab.flowit.global.security.jwt;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import dev.runtime_lab.flowit.global.security.jwt.element.JwtAccessToken;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenServiceTest {

	private final Clock clock = Clock.fixed(Instant.parse("2100-01-01T00:00:00Z"), ZoneOffset.UTC);
	private final JwtProperties jwtProperties = new JwtProperties(
		"flowit-test",
		Duration.ofMinutes(15),
		Duration.ofDays(14),
		"flowit_refresh_token",
		"/v1/public/auth",
		"Lax",
		false,
		null,
		null,
		null,
		null
	);

	@Test
	void issueAccessTokenSignsTokenWithRs256() throws Exception {
		KeyPair keyPair = generateRsaKeyPair();
		JwtConfig jwtConfig = new JwtConfig();
		JwtEncoder jwtEncoder = jwtConfig.jwtEncoder(
			(RSAPublicKey) keyPair.getPublic(),
			(RSAPrivateKey) keyPair.getPrivate()
		);
		TokenVersionJwtValidator tokenVersionJwtValidator = mock(TokenVersionJwtValidator.class);
		when(tokenVersionJwtValidator.validate(any(Jwt.class))).thenReturn(OAuth2TokenValidatorResult.success());
		JwtDecoder jwtDecoder = jwtConfig.jwtDecoder((RSAPublicKey) keyPair.getPublic(), jwtProperties, tokenVersionJwtValidator);
		JwtTokenService jwtTokenService = new JwtTokenService(jwtEncoder, jwtProperties, clock);

		JwtAccessToken accessToken = jwtTokenService.issueAccessToken(
			"1001",
			Map.of(
				"email", "user@example.com",
				"name", "nickname",
				FlowitJwtClaims.TOKEN_VERSION, 7L
			)
		);

		Jwt decodedJwt = jwtDecoder.decode(accessToken.tokenValue());
		assertNotNull(accessToken.tokenValue());
		assertEquals("Bearer", accessToken.tokenType());
		assertEquals(900L, accessToken.expiresIn());
		assertEquals("RS256", decodedJwt.getHeaders().get("alg"));
		assertEquals("flowit-test", decodedJwt.getClaimAsString("iss"));
		assertEquals("1001", decodedJwt.getSubject());
		assertEquals("user@example.com", decodedJwt.getClaimAsString("email"));
		assertEquals("nickname", decodedJwt.getClaimAsString("name"));
		assertEquals("access", decodedJwt.getClaimAsString(FlowitJwtClaims.TOKEN_TYPE));
		assertEquals(7L, ((Number) decodedJwt.getClaim(FlowitJwtClaims.TOKEN_VERSION)).longValue());
		assertEquals(Instant.parse("2100-01-01T00:00:00Z"), decodedJwt.getIssuedAt());
		assertEquals(Instant.parse("2100-01-01T00:15:00Z"), decodedJwt.getExpiresAt());
	}

	private KeyPair generateRsaKeyPair() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		return keyPairGenerator.generateKeyPair();
	}
}
