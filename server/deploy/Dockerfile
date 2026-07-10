# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

RUN chmod +x ./gradlew

COPY src ./src

ARG SERVER_GRADLE_TASKS="clean build"
RUN --mount=type=cache,target=/root/.gradle \
    sh -c "./gradlew --no-daemon ${SERVER_GRADLE_TASKS}"

RUN set -eux; \
    jar_file="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | sort | head -n 1)"; \
    test -n "$jar_file"; \
    cp "$jar_file" /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
