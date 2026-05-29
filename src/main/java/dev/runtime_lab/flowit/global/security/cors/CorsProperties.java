package dev.runtime_lab.flowit.global.security.cors;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

@ConfigurationProperties(prefix = "flowit.security.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        List<String> exposedHeaders,
        Boolean allowCredentials,
        Duration maxAge
) {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of("http://localhost:3000");
    private static final List<String> DEFAULT_ALLOWED_METHODS = List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()
    );
    private static final List<String> DEFAULT_ALLOWED_HEADERS = List.of("*");
    private static final List<String> DEFAULT_EXPOSED_HEADERS = List.of("Location");
    private static final boolean DEFAULT_ALLOW_CREDENTIALS = true;
    private static final Duration DEFAULT_MAX_AGE = Duration.ofHours(1);

    public CorsProperties {
        final String securityCorsPrefix = "flowit.security.cors.";

        allowedOrigins = normalize(allowedOrigins, DEFAULT_ALLOWED_ORIGINS, securityCorsPrefix + "allowed-origins");
        allowedMethods = normalize(allowedMethods, DEFAULT_ALLOWED_METHODS, securityCorsPrefix + "allowed-methods");
        allowedHeaders = normalize(allowedHeaders, DEFAULT_ALLOWED_HEADERS, securityCorsPrefix + "allowed-headers");
        exposedHeaders = normalize(exposedHeaders, DEFAULT_EXPOSED_HEADERS, securityCorsPrefix + "exposed-headers");
        allowCredentials = allowCredentials != null ? allowCredentials : DEFAULT_ALLOW_CREDENTIALS;
        maxAge = maxAge != null ? maxAge : DEFAULT_MAX_AGE;

        if (allowCredentials && allowedOrigins.contains("*")) {
            throw new IllegalArgumentException(
                    "flowit.security.cors.allowed-origins must not contain * when allow-credentials is true"
            );
        }
        if (maxAge.isNegative()) {
            throw new IllegalArgumentException("flowit.security.cors.max-age must not be negative");
        }
    }

    private static List<String> normalize(List<String> values, List<String> defaultValues, String propertyName) {
        if (values == null || values.isEmpty()) {
            return defaultValues;
        }

        List<String> normalized = values.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(propertyName + " must not contain only blank values");
        }

        return List.copyOf(normalized);
    }
}
