package dev.runtime_lab.flowit.global.config;

import dev.runtime_lab.flowit.global.security.cors.CorsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = {
		"/actuator/health",
		"/actuator/prometheus",
		"/api/docs-preview/**",
		"/docs",
		"/docs/**",
		"/v1/public/auth/login",
		"/v1/public/auth/logout",
		"/v1/public/auth/refresh",
		"/v1/public/users/join"
	};

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		AuthenticationEntryPoint authenticationEntryPoint,
		AccessDeniedHandler accessDeniedHandler,
		CorsConfigurationSource corsConfigurationSource
	) throws Exception {
		return http
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
				.anyRequest().authenticated()
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
			.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsProperties.allowedOrigins());
		configuration.setAllowedMethods(corsProperties.allowedMethods());
		configuration.setAllowedHeaders(corsProperties.allowedHeaders());
		configuration.setExposedHeaders(corsProperties.exposedHeaders());
		configuration.setAllowCredentials(corsProperties.allowCredentials());
		configuration.setMaxAge(corsProperties.maxAge().getSeconds());

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
