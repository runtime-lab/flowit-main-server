package dev.runtime_lab.flowit.global.dev;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "flowit.local-dev-data", name = "enabled", havingValue = "true")
public class LocalDevDataInitializer implements ApplicationRunner {

	private final LocalDevScenarioProvisioner localDevScenarioProvisioner;

	@Override
	public void run(ApplicationArguments args) {
		localDevScenarioProvisioner.provision();
	}
}
