package dev.runtime_lab.flowit.global.dev;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDevScenarioProvisioner {

	private final LocalDevBaseScenarioProvisioner baseScenarioProvisioner;
	private final LocalDevTaskScenarioStep taskScenarioStep;
	private final LocalDevTaskCommentScenarioStep taskCommentScenarioStep;
	private final LocalDevSampleRecordRegistry sampleRecordRegistry;

	@Transactional
	public void provision() {
		sampleRecordRegistry.ensureReady();

		LocalDevScenarioContext context = LocalDevScenarioChain
			.start(baseScenarioProvisioner.reconcileBaseScenario())
			.then(taskScenarioStep::reconcile)
			.then(taskCommentScenarioStep::reconcile)
			.context();

		log.info(
			"Local development data provisioned. workspaceId={} workspaceName={} userEmail={}",
			context.workspace().getId(),
			context.workspace().getName(),
			context.currentUser().email()
		);
	}
}
