package dev.runtime_lab.flowit.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import dev.runtime_lab.flowit.global.stereotype.InternalService;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
	packages = "dev.runtime_lab.flowit",
	importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

	private static final Pattern DOMAIN_PACKAGE = Pattern.compile(
			"dev\\.runtime_lab\\.flowit\\.domain\\.([^.]+)\\..*"
	);

	@ArchTest
	static final ArchRule domain_classes_should_not_depend_on_other_domain_repositories =
			classes()
					.that().resideInAPackage("..domain..")
					.should(notDependOnOtherDomainRepositories());

	@ArchTest
	static final ArchRule domain_repositories_should_not_depend_on_other_domain_dtos =
			classes()
					.that().resideInAPackage("..domain..repository..")
					.should(notDependOnOtherDomainDtos());

	private static ArchCondition<JavaClass> notDependOnOtherDomainRepositories() {
		return new ArchCondition<>("not depend on repositories in other domain packages") {
			@Override
			public void check(JavaClass item, ConditionEvents events) {
				Optional<String> sourceDomain = domainOf(item);

				if (sourceDomain.isEmpty()) {
					return;
				}

				for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
					JavaClass target = dependency.getTargetClass();

					if (!target.getPackageName().contains(".domain.")
						|| !target.getPackageName().contains(".repository")) {
						continue;
					}

					Optional<String> targetDomain = domainOf(target);
					if (targetDomain.isPresent() && !sourceDomain.get().equals(targetDomain.get())) {
						events.add(SimpleConditionEvent.violated(
								item,
								item.getName() + " depends on " + target.getName()
						));
					}
				}
			}
		};
	}

	private static ArchCondition<JavaClass> notDependOnOtherDomainDtos() {
		return new ArchCondition<>("not depend on DTOs in other domain packages") {
			@Override
			public void check(JavaClass item, ConditionEvents events) {
				Optional<String> sourceDomain = domainOf(item);

				if (sourceDomain.isEmpty()) {
					return;
				}

				for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
					JavaClass target = dependency.getTargetClass();

					if (!target.getPackageName().contains(".domain.")
						|| !target.getPackageName().contains(".dto")) {
						continue;
					}

					Optional<String> targetDomain = domainOf(target);
					if (targetDomain.isPresent() && !sourceDomain.get().equals(targetDomain.get())) {
						events.add(SimpleConditionEvent.violated(
								item,
								item.getName() + " depends on " + target.getName()
						));
					}
				}
			}
		};
	}

	private static Optional<String> domainOf(JavaClass item) {
		Matcher matcher = DOMAIN_PACKAGE.matcher(item.getPackageName());
		if (!matcher.matches()) {
			return Optional.empty();
		}

		return Optional.of(matcher.group(1));
	}

	@ArchTest
	static final ArchRule controllers_must_not_depend_on_internal_services =
			noClasses()
				.that().resideInAPackage("..controller..")
				.should().dependOnClassesThat().areAnnotatedWith(InternalService.class);

	@ArchTest
	static final ArchRule controllers_must_not_depend_on_internal_service_packages =
			noClasses()
				.that().resideInAPackage("..controller..")
				.should().dependOnClassesThat().resideInAPackage("..service.internal..");

	@ArchTest
	static final ArchRule internal_services_should_be_annotated =
			classes()
					.that().resideInAPackage("..domain..service.internal..")
					.and().areTopLevelClasses()
					.and().areNotRecords()
					.should().beAnnotatedWith(InternalService.class);

	@ArchTest
	static final ArchRule internal_service_annotation_should_stay_in_internal_package =
			classes()
					.that().areAnnotatedWith(InternalService.class)
					.should().resideInAPackage("..domain..service.internal..");

}
