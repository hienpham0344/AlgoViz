# Sandbox verification — 2026-10-19

Environment: Windows host, Docker Desktop Linux engine 29.8.0, Java 17. Tests were run with locally built `algoviz-runner:local` and `algoviz-ast-worker:local` images. The public web frontend/backend were not restarted.

## Results

| Suite | Result |
|---|---|
| Backend full `mvn test`, `ALGOVIZ_RUN_SANDBOX_TESTS=true` | 26 tests: 24 passed, 2 skipped, no failures/errors |
| Backend real-container security/runtime integration | 13 tests passed (included above) |
| AST-tool full `mvn test`, same opt-in flag | 142 tests passed, none skipped, no failures/errors |

The two skipped backend tests are database-import tests gated by `ALGOVIZ_RUN_IMPORT_TEST`; no production dataset import was requested. The AST suite includes eight multipart API tests using the worker container, two dedicated isolated-trace integration tests, and the existing AST/JDI semantics tests. Trusted low-level JDI fixtures execute test programs directly; public execution routes and the CLI delegate to the container worker.

Backend real-container checks:

1. Java starts normally and receives EOF for empty stdin.
2. Web Run still accepts a Java public class whose name differs from `Main`.
3. Python input, JavaScript and C++ produce their expected outputs.
4. A temporary host sentinel cannot be read/modified by the job; its content remains unchanged.
5. Root filesystem and input mount reject writes; Docker socket is absent.
6. Outbound TCP connection is blocked by the disabled network.
7. A subprocess runs as UID 65534 and remains inside the container boundary.
8. Actual cgroup limits report memory=268435456 bytes, pids=64, CPU=one core; `NoNewPrivs=1`, effective capabilities=0.
9. A 128 MiB allocation fails under the 64 MiB Java heap policy.
10. An infinite loop terminates and its container is removed.
11. Excessive stdout is bounded to 1 MiB and its container is removed.
12. A file created in one job is absent in the next.
13. An unavailable runner image causes execution to fail closed.

Additional unit tests check path traversal/resource-limit rejection, container compilation errors, output overflow handling, and Docker failure without host fallback. AST-specific tests verify correct runtime locals and blocked host-file/network access while collecting JDI trace.

The first verification runs found and fixed: a noexec scratch mount breaking C++ execution, JSON deserialization of omitted empty Bubble Sort metadata, and a null Process in an existing mocked JDI disconnect test. The successful counts above are from subsequent runs after these fixes.

Logs and XML reports are under each backend's ignored `target/` directory (`sandbox-tests.log`, `sandbox-worker-image-tests.log`, `surefire-reports/`). These are local build artifacts, not committed test evidence files. This summary records the observed checks; it is not a claim of resistance to every kernel/container exploit. See [deployment limits](README.md#deployment-limits).
