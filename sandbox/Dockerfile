FROM eclipse-temurin:17-jdk-jammy
RUN apt-get update && apt-get install -y --no-install-recommends python3 nodejs g++ coreutils \
    && rm -rf /var/lib/apt/lists/*
COPY sandbox/entrypoint.sh /usr/local/bin/algoviz-run
RUN chmod 0555 /usr/local/bin/algoviz-run
USER 65534:65534
WORKDIR /work
ENTRYPOINT ["/usr/local/bin/algoviz-run"]
