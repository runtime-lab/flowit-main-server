package dev.runtime_lab.flowit.global.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	@Bean
	public RSAPublicKey jwtPublicKey(JwtKeyLoader keyLoader) {
		return keyLoader.loadPublicKey();
	}

	@Bean
	public RSAPrivateKey jwtPrivateKey(JwtKeyLoader keyLoader) {
		return keyLoader.loadPrivateKey();
	}

	@Bean
	public JwtEncoder jwtEncoder(RSAPublicKey jwtPublicKey, RSAPrivateKey jwtPrivateKey) {
		RSAKey rsaKey = new RSAKey.Builder(jwtPublicKey)
			.privateKey(jwtPrivateKey)
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.RS256)
			.build();
		JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));

		return new NimbusJwtEncoder(jwkSource);
	}

	@Bean
	public JwtDecoder jwtDecoder(RSAPublicKey jwtPublicKey, JwtProperties jwtProperties) {
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(jwtPublicKey)
			.signatureAlgorithm(SignatureAlgorithm.RS256)
			.build();
		jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()));

		return jwtDecoder;
	}
}
