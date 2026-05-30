package dev.runtime_lab.flowit.global.security.jwt;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlowitJwtClaims {

	public static final String TOKEN_TYPE = "token_type";
	public static final String TOKEN_VERSION = "token_version";
	public static final String ACCESS_TOKEN_TYPE = "access";
}
