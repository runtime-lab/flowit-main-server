package dev.runtime_lab.flowit.global.dev;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LocalDevDataInitializerTest {

	@Test
	void runDelegatesToScenarioProvisioner() {
		LocalDevScenarioProvisioner scenarioProvisioner = mock(LocalDevScenarioProvisioner.class);
		LocalDevDataInitializer initializer = new LocalDevDataInitializer(scenarioProvisioner);

		initializer.run(null);

		verify(scenarioProvisioner).provision();
	}
}
