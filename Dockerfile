FROM eclipse-temurin:17-jdk-jammy@sha256:beabb759e6f9653c843958d1d1f5cecb881dfb85aa6081e2bef099ab1260344e AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew
RUN ./gradlew --no-daemon --version

COPY src ./src

RUN ./gradlew --no-daemon bootJar \
	&& JAR_PATH="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" \
	&& cp "$JAR_PATH" flowit.jar

FROM eclipse-temurin:17-jre-jammy@sha256:47c73dc23524b031bed0a5030410c722af6a8b49d4b25898ea8f4615895065f0

WORKDIR /app

ENV TZ=Asia/Seoul

RUN useradd --system --uid 10001 --create-home --home-dir /app flowit

COPY --from=builder --chown=flowit:flowit /workspace/flowit.jar /app/flowit.jar

USER flowit

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "/app/flowit.jar"]
