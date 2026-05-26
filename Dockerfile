FROM eclipse-temurin:25-jdk-jammy@sha256:7bb4493421ff8fe7d0361d0518e5abf0026fc6ac774ecdf28bb6b90d4fd4c4f8 AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew
RUN ./gradlew --no-daemon --version

COPY src ./src

RUN ./gradlew --no-daemon bootJar \
	&& JAR_PATH="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" \
	&& cp "$JAR_PATH" flowit.jar

FROM eclipse-temurin:25-jre-jammy@sha256:c3e62cd0cece58d8de8d760ab95a5014f3b5a6ea32178f54270edb5b4aab9d1f

WORKDIR /app

ENV TZ=Asia/Seoul

RUN useradd --system --uid 10001 --create-home --home-dir /app flowit

COPY --from=builder --chown=flowit:flowit /workspace/flowit.jar /app/flowit.jar

USER flowit

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "/app/flowit.jar"]
