package dev.runtime_lab.flowit.global.security.jwt;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import dev.runtime_lab.flowit.global.security.jwt.element.JwtAccessToken;
import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;
	private final Clock clock;

	public JwtAccessToken issueAccessToken(String subject, Map<String, Object> claims) {
		Instant issuedAt = Instant.now(clock);
		Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTimeToLive());
		JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
			.issuer(jwtProperties.issuer())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.subject(subject)
			.claim(FlowitJwtClaims.TOKEN_TYPE, FlowitJwtClaims.ACCESS_TOKEN_TYPE);

		claims.forEach(claimsBuilder::claim);

		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claimsBuilder.build())).getTokenValue();

		return JwtAccessToken.bearer(token, jwtProperties.accessTokenTimeToLive().toSeconds());
	}
}
