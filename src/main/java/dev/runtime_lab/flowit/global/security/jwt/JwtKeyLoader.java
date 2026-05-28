package dev.runtime_lab.flowit.global.security.jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import dev.runtime_lab.flowit.global.security.jwt.element.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
class JwtKeyLoader {

	private final JwtProperties properties;
	private final ResourceLoader resourceLoader;

	RSAPublicKey loadPublicKey() {
		String keyMaterial = loadKeyMaterial(properties.publicKey(), properties.publicKeyLocation(), "public");
		byte[] keyBytes = decodeKeyMaterial(keyMaterial, "PUBLIC KEY");

		try {
			return (RSAPublicKey) keyFactory().generatePublic(new X509EncodedKeySpec(keyBytes));
		}
		catch (InvalidKeySpecException exception) {
			throw new IllegalStateException("JWT public key must be X.509 SubjectPublicKeyInfo PEM or Base64 DER", exception);
		}
	}

	RSAPrivateKey loadPrivateKey() {
		String keyMaterial = loadKeyMaterial(properties.privateKey(), properties.privateKeyLocation(), "private");
		byte[] keyBytes = decodeKeyMaterial(keyMaterial, "PRIVATE KEY");

		try {
			return (RSAPrivateKey) keyFactory().generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
		}
		catch (InvalidKeySpecException exception) {
			throw new IllegalStateException("JWT private key must be PKCS#8 PEM or Base64 DER", exception);
		}
	}

	private String loadKeyMaterial(String inlineKey, String keyLocation, String keyName) {
		if (StringUtils.hasText(inlineKey)) {
			return inlineKey;
		}
		if (!StringUtils.hasText(keyLocation)) {
			throw new IllegalStateException("JWT " + keyName + " key or key location must be configured");
		}

		Resource resource = resourceLoader.getResource(keyLocation);
		try {
			if (!resource.exists()) {
				throw new IllegalStateException("JWT " + keyName + " key resource does not exist: " + keyLocation);
			}
			return resource.getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException exception) {
			throw new IllegalStateException("Failed to read JWT " + keyName + " key resource: " + keyLocation, exception);
		}
	}

	private byte[] decodeKeyMaterial(String keyMaterial, String pemLabel) {
		String normalized = keyMaterial
			.replace("\\n", "\n")
			.replace("-----BEGIN " + pemLabel + "-----", "")
			.replace("-----END " + pemLabel + "-----", "")
			.replaceAll("\\s", "");

		try {
			return Base64.getDecoder().decode(normalized);
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalStateException("JWT " + pemLabel + " material is not valid PEM or Base64", exception);
		}
	}

	private KeyFactory keyFactory() {
		try {
			return KeyFactory.getInstance("RSA");
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("RSA key factory is unavailable", exception);
		}
	}
}
