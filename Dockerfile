FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew
RUN ./gradlew --no-daemon --version

COPY src ./src

RUN ./gradlew --no-daemon bootJar \
	&& JAR_PATH="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" \
	&& cp "$JAR_PATH" flowit.jar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ENV TZ=Asia/Seoul

RUN useradd --system --uid 10001 --create-home --home-dir /app flowit

COPY --from=builder --chown=flowit:flowit /workspace/flowit.jar /app/flowit.jar

USER flowit

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "/app/flowit.jar"]
