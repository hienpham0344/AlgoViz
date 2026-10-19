# AlgoViz execution sandbox

All HTTP execution paths require Docker Linux containers. There is no host-process fallback:

- `/api/v1/sandbox/execute`: compilation and Java execution inside a disposable container.
- `/api/submissions/run` and `/api/submissions/submit`: Java/Python/JavaScript/C++ inside the same restricted runner.
- AST-tool `/api/debug`, `/api/debug/steps`, and the AST CLI: parsing/compilation/JDI collection inside a separate worker container. Playback remains compatible with the existing trace schema.

## Build and run

Start Docker Desktop with **Linux containers** (or a local Linux Docker Engine).
From the AlgoViz repository root:

```powershell
docker build -t algoviz-runner:local -f sandbox/Dockerfile .
docker build -t algoviz-ast-worker:local -f AST-tool/backend/Dockerfile.sandbox AST-tool/backend
```

Then start the backend/AST service normally. The runner images must be built in advance; execution uses `--pull=never`. If the daemon or image is unavailable, execution fails with a sandbox error. It never executes caller code directly on the API host.

The AST-tool repository is independent and ignored by the root repository. Its sandbox changes live on `codex/sandbox-isolation` and must be distributed alongside this branch. Its worker image extends `algoviz-runner:local`, so build the base image first. An independent copy of the small container launcher lives in each repository; keep the two launcher files synchronized.

Optional deployment settings:

- `ALGOVIZ_DOCKER_COMMAND`: trusted server-side Docker CLI path.
- `ALGOVIZ_SANDBOX_IMAGE`: backend runner image (default `algoviz-runner:local`).
- `ALGOVIZ_AST_SANDBOX_IMAGE`: AST worker image (default `algoviz-ast-worker:local`).

Use the daemon on the same host as the API because the input directory is a bind mount. Do not expose Docker access to users, mount the Docker socket into the job, or pass user-provided Docker arguments/images/mounts.

## Boundary and enforced limits

Each job has a fresh container with network disabled, non-root UID/GID 65534, read-only root filesystem, all Linux capabilities removed and `no-new-privileges`. Docker's default seccomp policy remains enabled. No repository, home directory, credentials, environment secrets, Docker socket or other job directory is mounted. Only that request's source/input files are mounted read-only at `/input`.

The program can read the runtime libraries inside its **container**. Writable scratch space is limited to private tmpfs mounts, not the host filesystem. Subprocesses remain in the same container namespaces and share its quotas; they cannot escape merely by calling ProcessBuilder or reflection. This replaces the old regex blacklist as the isolation boundary.

| Resource | Backend run | AST trace |
|---|---|---|
| Container memory / swap | 256 MiB total, no extra swap | 512 MiB total, no extra swap |
| CPU | 1 CPU | 1 CPU |
| Processes / threads | 64 | 64 |
| Writable scratch | `/work` 64 MiB + `/tmp` 32 MiB + `/dev/shm` 8 MiB | Same |
| Source | 200,000 bytes | Upload 200,000 bytes |
| Input | 1 MiB | Input embedded in Java source; no stdin API yet |
| Output returned | 1 MiB per stream | 8 MiB trace JSON; target stdout/stderr capped separately |
| Java heap / stack | Heap 16–128 MiB, stack 256–2048 KiB (validated) | Target heap 64 MiB, stack 512 KiB |
| Timeout | Compile max 10 s; run 0.1–10 s; host watchdog adds 15 s for compile/start overhead | JDI 30 s; container watchdog 35 s; host watchdog 40 s |
| Concurrent jobs | 2 per backend JVM | 2 per AST service JVM |

Container creation has an additional 10 s control timeout; removal has a 5 s timeout. On deadline, output overflow or interruption, the host forcibly removes the named container (including descendants), then deletes its own input directory. No Docker restart policy is configured. A daemon/host failure can interrupt cleanup; operators should remove stale containers carrying `algoviz.sandbox=true` after recovery. Do not run this cleanup indiscriminately during active jobs.

`executionTimeMs` includes Docker startup/compilation overhead; it is not an algorithm microbenchmark. Exit 137 may mean timeout or an OOM kill and is reported as a resource limit, not proof of a specific cause.

## Verification

Unit tests do not need Docker. Real isolation tests are opt-in so missing Docker is explicit:

```powershell
$env:ALGOVIZ_RUN_SANDBOX_TESTS='true'
cd backend
./mvnw.cmd test
```

For AST-tool, set the same variable and run Maven tests in `AST-tool/backend` after building its worker image. Integration tests exercise real containers; trusted low-level JDI unit/integration fixtures also remain to check AST and execution semantics directly.

Security checks include normal execution, stdin EOF, other runtimes, access attempts to a temporary host sentinel, writes to root/read-only input, absent Docker socket, blocked outbound connections, non-root subprocesses, timeout cleanup, output limits, and state isolation across jobs. These payloads run only inside containers.

## Deployment limits

Containers share the Docker host kernel; this is not a proof against kernel/runtime vulnerabilities. Keep Docker/images patched. For a public multi-tenant service, deploy the execution daemon on a dedicated worker host with no application/database secrets; consider rootless Docker and a stronger runtime such as gVisor or microVMs. Multi-instance admission control and per-account rate limits require a shared job queue. This implementation limits each service process locally.

References: [Docker run isolation and privileges](https://docs.docker.com/engine/containers/run/), [resource constraints](https://docs.docker.com/engine/containers/resource_constraints/), [Docker daemon security](https://docs.docker.com/engine/security/).
