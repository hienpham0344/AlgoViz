# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

# The AST/JDI tracer needs a full JDK. Code execution also needs Python,
# C++ and Node.js at runtime.
FROM eclipse-temurin:21-jdk-jammy AS runtime
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    ALGOVIZ_PYTHON_COMMAND=python3 \
    ALGOVIZ_CPP_COMMAND=g++

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 g++ curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 algoviz \
    && useradd --system --uid 10001 --gid algoviz --home-dir /app --shell /usr/sbin/nologin algoviz \
    && mkdir -p /app /tmp/algoviz \
    && chown -R algoviz:algoviz /app /tmp/algoviz
COPY --from=node:22-bookworm-slim /usr/local/bin/node /usr/local/bin/node
RUN node --version && python3 --version && g++ --version && javac -version

WORKDIR /app
COPY --from=build /build/target/algoViz-0.0.1-SNAPSHOT.jar /app/app.jar
USER algoviz

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD ["sh", "-c", "curl -fsS http://127.0.0.1:8080/api/ast/health || exit 1"]
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
