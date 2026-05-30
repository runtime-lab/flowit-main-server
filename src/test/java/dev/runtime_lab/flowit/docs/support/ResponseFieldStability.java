package dev.runtime_lab.flowit.docs.support;

import org.springframework.restdocs.snippet.Attributes.Attribute;

import static org.springframework.restdocs.snippet.Attributes.key;

public final class ResponseFieldStability {

	private ResponseFieldStability() {
	}

	public static Attribute[] stable() {
		return attributes("Stable", "stable");
	}

	public static Attribute[] experimental() {
		return attributes("Experimental", "experimental");
	}

	public static Attribute[] deprecated() {
		return attributes("Deprecated", "deprecated");
	}

	private static Attribute[] attributes(String label, String cssClass) {
		return new Attribute[] {
			key("stability").value(label),
			key("stabilityClass").value(cssClass)
		};
	}
}
