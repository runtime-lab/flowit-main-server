package dev.runtime_lab.flowit.global.config;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigCorsIntegrationTest.SecureController.class)
@Import({SecurityConfig.class, SecurityConfigCorsIntegrationTest.SecurityTestConfig.class})
class SecurityConfigCorsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void preflightFromLocalReactApplicationPassesBeforeAuthentication() throws Exception {
		mockMvc.perform(options("/secure")
				.header(HttpHeaders.ORIGIN, "http://localhost:3000")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")));
	}

	@RestController
	static class SecureController {

		@GetMapping("/secure")
		String secure() {
			return "ok";
		}
	}

	@TestConfiguration
	static class SecurityTestConfig {

		@Bean
		AuthenticationEntryPoint authenticationEntryPoint() {
			return (request, response, authException) -> response.sendError(401);
		}

		@Bean
		AccessDeniedHandler accessDeniedHandler() {
			return (request, response, accessDeniedException) -> response.sendError(403);
		}

		@Bean
		JwtDecoder jwtDecoder() {
			return token -> Jwt.withTokenValue(token)
				.header("alg", "none")
				.subject("test")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		}
	}
}
