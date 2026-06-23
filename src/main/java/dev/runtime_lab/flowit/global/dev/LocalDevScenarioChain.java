package dev.runtime_lab.flowit.global.dev;

import java.util.function.Consumer;

final class LocalDevScenarioChain {

	private final LocalDevScenarioContext context;

	private LocalDevScenarioChain(LocalDevScenarioContext context) {
		this.context = context;
	}

	static LocalDevScenarioChain start(LocalDevScenarioContext context) {
		return new LocalDevScenarioChain(context);
	}

	LocalDevScenarioChain then(Consumer<LocalDevScenarioContext> step) {
		step.accept(context);
		return this;
	}

	LocalDevScenarioContext context() {
		return context;
	}
}
