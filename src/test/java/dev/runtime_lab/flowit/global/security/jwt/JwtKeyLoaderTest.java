package dev.runtime_lab.flowit.global.security.jwt;

import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtKeyLoaderTest {

	@Test
	void localDevelopmentKeysCanSignAndVerify() throws Exception {
		JwtKeyLoader keyLoader = new JwtKeyLoader(
			new JwtProperties(
				"flowit-test",
				Duration.ofMinutes(15),
				Duration.ofDays(14),
				"flowit_refresh_token",
				"/v1/public/auth",
				"Lax",
				false,
				"classpath:jwt/local-dev-private.pem",
				"classpath:jwt/local-dev-public.pem",
				null,
				null
			),
			new DefaultResourceLoader()
		);
		RSAPrivateKey privateKey = keyLoader.loadPrivateKey();
		RSAPublicKey publicKey = keyLoader.loadPublicKey();

		byte[] payload = "flowit-local-dev-jwt-key-check".getBytes(StandardCharsets.UTF_8);
		Signature signer = Signature.getInstance("SHA256withRSA");
		signer.initSign(privateKey);
		signer.update(payload);

		Signature verifier = Signature.getInstance("SHA256withRSA");
		verifier.initVerify(publicKey);
		verifier.update(payload);

		assertTrue(verifier.verify(signer.sign()));
	}
}
