FROM eclipse-temurin:22-jdk-jammy@sha256:d8e6ba486df17bf758888d2b1b608133d1eedca8daf69d3fc6bf78d8be81e07e AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew
RUN ./gradlew --no-daemon --version

COPY src ./src

RUN ./gradlew --no-daemon -PforceApiDocs=true bootJar \
	&& JAR_PATH="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" \
	&& cp "$JAR_PATH" flowit.jar

FROM eclipse-temurin:22-jre-jammy@sha256:dbcae8b5dd4d63f81739a538ec2c09797735f04a21d814f9071b62f018326043

WORKDIR /app

ENV TZ=Asia/Seoul

RUN useradd --system --uid 10001 --create-home --home-dir /app flowit \
	&& mkdir -p /app/flowit/main-server/profile-images \
	&& chown -R flowit:flowit /app/flowit

COPY --from=builder --chown=flowit:flowit /workspace/flowit.jar /app/flowit.jar

USER flowit

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "/app/flowit.jar"]
