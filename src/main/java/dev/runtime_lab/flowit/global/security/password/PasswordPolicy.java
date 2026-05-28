package dev.runtime_lab.flowit.global.security.password;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

	public void validate(String password) {
		if (password == null || password.codePoints().anyMatch(this::isSpecialCharacter)) {
			throw new InvalidPasswordPolicyException();
		}
	}

	private boolean isSpecialCharacter(int codePoint) {
		return !Character.isLetterOrDigit(codePoint);
	}
}
