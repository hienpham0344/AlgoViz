# syntax=docker/dockerfile:1

# AlgoViz all-in-one image: builds the Vite frontend, packages it into the
# Spring Boot JAR, and serves API + UI from a single non-root container.

FROM node:22-bookworm-slim AS frontend-build
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /build
COPY backend/pom.xml ./
RUN mvn -B -ntp dependency:go-offline
# The frontend is copied between the sources and the build so edits to Java
# code do not invalidate the Maven dependency cache.
COPY backend/ ./
COPY --from=frontend-build /build/frontend/dist ./src/main/resources/static
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jdk-jammy AS runtime
ENV TZ=UTC \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    ALGOVIZ_PYTHON_COMMAND=python3 \
    ALGOVIZ_CPP_COMMAND=g++ \
    ALGOVIZ_DATA_DIRECTORY=/app/data

# A full JDK is required: the AST/JDI tracer needs javac and the jdk.jdi module.
# Judge toolchain for the native execution mode: Python, C++ and a current Node.js.
RUN apt-get update \
    && apt-get install -y --no-install-recommends g++ python3 ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=frontend-build /usr/local/bin/node /usr/local/bin/node

RUN groupadd --system --gid 10001 algoviz \
    && useradd --system --uid 10001 --gid algoviz --home-dir /app --shell /usr/sbin/nologin algoviz \
    && mkdir -p /app/data /tmp/algoviz \
    && chown -R algoviz:algoviz /app /tmp/algoviz

WORKDIR /app
COPY --from=backend-build /build/target/algoViz-0.0.1-SNAPSHOT.jar /app/app.jar
USER algoviz

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD ["sh", "-c", "curl -fsS http://127.0.0.1:8080/api/ast/health || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
