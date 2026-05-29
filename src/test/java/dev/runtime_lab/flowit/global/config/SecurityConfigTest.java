package dev.runtime_lab.flowit.global.config;

import dev.runtime_lab.flowit.global.security.cors.CorsProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

	@Test
	void corsConfigurationAllowsLocalReactApplication() {
		CorsProperties corsProperties = new CorsProperties(
			List.of("http://localhost:3000"),
			List.of("GET", "POST", "OPTIONS"),
			List.of("*"),
			List.of("Location"),
			true,
			Duration.ofHours(1)
		);
		CorsConfigurationSource corsConfigurationSource = new SecurityConfig()
			.corsConfigurationSource(corsProperties);
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/v1/public/auth/login");
		request.addHeader(HttpHeaders.ORIGIN, "http://localhost:3000");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");

		CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

		assertNotNull(configuration);
		assertEquals("http://localhost:3000", configuration.checkOrigin("http://localhost:3000"));
		assertTrue(configuration.getAllowedMethods().contains("OPTIONS"));
		assertTrue(configuration.getAllowedHeaders().contains("*"));
		assertTrue(configuration.getAllowCredentials());
		assertEquals(3_600L, configuration.getMaxAge());
	}

	@Test
	void corsPropertiesRejectsWildcardOriginWhenCredentialsAreAllowed() {
		assertThrows(IllegalArgumentException.class, () -> new CorsProperties(
			List.of("*"),
			List.of("GET", "POST", "OPTIONS"),
			List.of("*"),
			List.of("Location"),
			true,
			Duration.ofHours(1)
		));
	}
}
