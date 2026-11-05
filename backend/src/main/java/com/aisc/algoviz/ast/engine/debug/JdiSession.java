package com.aisc.algoviz.ast.engine.debug;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.mapping.VariableMapper;
import com.aisc.algoviz.ast.engine.mapping.IntervalValueProjection;
import com.aisc.algoviz.ast.engine.mapping.IntegerListProjection;
import com.aisc.algoviz.ast.engine.model.ExecutionContext;
import com.aisc.algoviz.ast.engine.model.ExecutionObservation;
import com.aisc.algoviz.ast.engine.model.ObservationPoint;
import com.aisc.algoviz.ast.engine.model.ObservationResult;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Launches one compiled target VM and collects local-variable observations through JDI.
 *
 * <p>The event loop uses a fixed deadline and polls the JDI queue. It never waits indefinitely for
 * a user program: on timeout the debuggee is terminated and the caller receives a warning.
 */
@Component
public class JdiSession {

  private static final String OBSERVATION_POINT_PROPERTY = "observationPoint";
  private static final long EVENT_POLL_MILLIS = 250;

  private final BreakpointMapper breakpointMapper;
  private final VariableMapper variableMapper;
  private final Duration timeout;
  private int maxSteps = 2000;

  @Autowired
  public JdiSession(BreakpointMapper breakpointMapper, VariableMapper variableMapper,
      @Value("${algoviz.ast.timeout-seconds:8}") long seconds,
      @Value("${algoviz.ast.max-steps:2000}") int maxSteps) {
    this(breakpointMapper, variableMapper, Duration.ofSeconds(Math.max(1, Math.min(seconds, 30))));
    this.maxSteps = Math.max(1, Math.min(maxSteps, 10000));
  }

  public JdiSession(BreakpointMapper breakpointMapper, VariableMapper variableMapper) {
    this(breakpointMapper, variableMapper, Duration.ofSeconds(8));
  }

  public JdiSession(
      BreakpointMapper breakpointMapper, VariableMapper variableMapper, Duration timeout) {
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("JDI observation timeout must be positive.");
    }
    this.breakpointMapper = breakpointMapper;
    this.variableMapper = variableMapper;
    this.timeout = timeout;
  }

  public DebugRun observe(TargetCompiler.CompiledTarget target, AstAnalyzer.Analysis analysis) {
    return observe(target, analysis, new IntervalValueProjection(Map.of()));
  }

  public DebugRun observe(
      TargetCompiler.CompiledTarget target,
      AstAnalyzer.Analysis analysis,
      IntervalValueProjection intervalProjection) {
    return observe(target, analysis, intervalProjection, new IntegerListProjection(Set.of()));
  }

  public DebugRun observe(
      TargetCompiler.CompiledTarget target,
      AstAnalyzer.Analysis analysis,
      IntervalValueProjection intervalProjection,
      IntegerListProjection integerListProjection) {
    return observe(target, analysis, intervalProjection, integerListProjection, "");
  }

  public DebugRun observe(TargetCompiler.CompiledTarget target, AstAnalyzer.Analysis analysis,
      IntervalValueProjection intervalProjection, IntegerListProjection integerListProjection, String input) {
    VirtualMachine virtualMachine = null;
    Process process = null;
    try {
      LaunchedTarget launched = launch(target, input);
      virtualMachine = launched.vm();
      process = launched.process();
      EventRequestManager requestManager = virtualMachine.eventRequestManager();
      ClassPrepareRequest classPrepareRequest = requestManager.createClassPrepareRequest();
      classPrepareRequest.addClassFilter(target.mainClassName());
      classPrepareRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
      classPrepareRequest.enable();
      MethodEntryRequest methodEntryRequest = requestManager.createMethodEntryRequest();
      methodEntryRequest.addClassFilter(target.mainClassName());
      methodEntryRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
      methodEntryRequest.enable();
      MethodExitRequest methodExitRequest = requestManager.createMethodExitRequest();
      methodExitRequest.addClassFilter(target.mainClassName());
      methodExitRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
      methodExitRequest.enable();

      List<ExecutionObservation> executions = new ArrayList<>();
      List<String> warnings = new ArrayList<>();
      Set<ReferenceType> installedTypes = new HashSet<>();
      InvocationTracker invocations = new InvocationTracker();
      installAlreadyPreparedTargetType(
          virtualMachine, requestManager, analysis, installedTypes, warnings);

      // The target connects suspended. Resuming only after the ClassPrepareRequest is
      // active guarantees that a lazily-loaded target class cannot miss breakpoint installation.
      virtualMachine.resume();
      boolean completed = runEventLoop(virtualMachine, requestManager, analysis, installedTypes, executions, warnings,
          invocations, intervalProjection, integerListProjection, target.workspaceDirectory());
      try {
        if (!process.waitFor(1000, TimeUnit.MILLISECONDS)) {
          terminate(process);
          warnings.add("Target did not exit after the trace ended.");
          completed = false;
        }
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new DebugException("Trace interrupted.", interrupted);
      }
      try {
        if (Files.size(target.workspaceDirectory().resolve("stdout.txt")) > 65536
            || Files.size(target.workspaceDirectory().resolve("stderr.txt")) > 65536) {
          warnings.add("Target output was truncated to 65536 bytes per stream.");
          completed = false;
        }
      } catch (IOException ex) { throw new DebugException("Unable to inspect target output.", ex); }
      return new DebugRun(List.copyOf(executions), List.copyOf(warnings),
          readOutput(target.workspaceDirectory().resolve("stdout.txt")),
          readOutput(target.workspaceDirectory().resolve("stderr.txt")), process.exitValue(), !completed);
    } catch (VMDisconnectedException ex) {
      return new DebugRun(
          List.of(), List.of("Target VM disconnected before observation completed."));
    } finally {
      disposeQuietly(virtualMachine);
      terminate(process);
    }
  }

  private record LaunchedTarget(VirtualMachine vm, Process process) {}

  private LaunchedTarget launch(TargetCompiler.CompiledTarget target, String input) {
    ListeningConnector connector = Bootstrap.virtualMachineManager().listeningConnectors().stream()
        .filter(c -> c.name().equals("com.sun.jdi.SocketListen")).findFirst()
        .orElseThrow(() -> new DebugException("The JDK does not provide SocketListen."));
    Map<String, Connector.Argument> arguments = connector.defaultArguments();
    arguments.get("localAddress").setValue("127.0.0.1");
    arguments.get("port").setValue("0");
    arguments.get("timeout").setValue("5000");
    Process process = null;
    boolean listening = false;
    try {
      String address = connector.startListening(arguments);
      listening = true;
      ProcessBuilder builder = new ProcessBuilder(
          Path.of(System.getProperty("java.home"), "bin", "java").toString(),
          "-Xmx64m", "-XX:MaxMetaspaceSize=64m", "-XX:ActiveProcessorCount=1",
          "-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=" + address,
          "-cp", target.classesDirectory().toString(), target.mainClassName());
      // The debuggee must never inherit database credentials, tokens or JVM injection options.
      builder.environment().clear();
      builder.environment().put("LANG", "C.UTF-8");
      builder.directory(target.workspaceDirectory().toFile());
      builder.redirectOutput(target.workspaceDirectory().resolve("stdout.txt").toFile());
      builder.redirectError(target.workspaceDirectory().resolve("stderr.txt").toFile());
      Path stdin = target.workspaceDirectory().resolve("stdin.txt");
      Files.writeString(stdin, input == null ? "" : input, StandardCharsets.UTF_8);
      builder.redirectInput(stdin.toFile());
      process = builder.start();
      return new LaunchedTarget(connector.accept(arguments), process);
    } catch (IOException | IllegalConnectorArgumentsException ex) {
      terminate(process);
      throw new DebugException("Unable to start the Java trace target.", ex);
    } finally {
      if (listening) {
        try { connector.stopListening(arguments); }
        catch (IOException | IllegalConnectorArgumentsException ignored) { /* already closed */ }
      }
    }
  }

  private static String readOutput(Path path) {
    try (var input = Files.newInputStream(path)) {
      return new String(input.readNBytes(65536), StandardCharsets.UTF_8).trim();
    } catch (IOException ex) {
      throw new DebugException("Unable to read trace output.", ex);
    }
  }

  private static void terminate(Process process) {
    if (process == null) return;
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    if (process.isAlive()) process.destroyForcibly();
    try { process.waitFor(1000, TimeUnit.MILLISECONDS); }
    catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
  }

  private void installAlreadyPreparedTargetType(
      VirtualMachine virtualMachine,
      EventRequestManager requestManager,
      AstAnalyzer.Analysis analysis,
      Set<ReferenceType> installedTypes,
      List<String> warnings) {
    for (ReferenceType referenceType : virtualMachine.classesByName(analysis.mainClassName())) {
      installBreakpoints(referenceType, requestManager, analysis, installedTypes, warnings);
    }
  }

  private boolean runEventLoop(
      VirtualMachine virtualMachine,
      EventRequestManager requestManager,
      AstAnalyzer.Analysis analysis,
      Set<ReferenceType> installedTypes,
      List<ExecutionObservation> executions,
      List<String> warnings,
      InvocationTracker invocations,
      IntervalValueProjection intervalProjection,
      IntegerListProjection integerListProjection, Path workspace) {
    Instant deadline = Instant.now().plus(timeout);
    boolean targetFinished = false;

    while (!targetFinished && Instant.now().isBefore(deadline)) {
      try {
        if (Files.size(workspace.resolve("stdout.txt")) + Files.size(workspace.resolve("stderr.txt")) > 1048576) {
          warnings.add("Trace stopped at the output limit; the trace is partial.");
          stopTarget(virtualMachine);
          return false;
        }
      } catch (IOException ex) { throw new DebugException("Unable to inspect target output.", ex); }
      EventSet eventSet;
      try {
        eventSet = removeNextEventSet(virtualMachine);
      } catch (VMDisconnectedException ex) {
        warnings.add("Target VM disconnected before it produced another event.");
        return false;
      }
      if (Thread.currentThread().isInterrupted()) throw new DebugException("Trace interrupted.");
      if (eventSet == null) {
        continue;
      }

      try {
        for (Event event : eventSet) {
          if (event instanceof ClassPrepareEvent classPrepareEvent) {
            installBreakpoints(
                classPrepareEvent.referenceType(),
                requestManager,
                analysis,
                installedTypes,
                warnings);
          } else if (event instanceof MethodEntryEvent entryEvent) {
            invocations.enter(entryEvent);
          } else if (event instanceof MethodExitEvent exitEvent) {
            invocations.exit(exitEvent);
          } else if (event instanceof BreakpointEvent breakpointEvent) {
            captureVariables(breakpointEvent, analysis, executions, warnings, invocations,
                intervalProjection, integerListProjection);
          } else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
            targetFinished = true;
          }
        }
      } finally {
        resumeQuietly(eventSet);
      }
      if (executions.size() >= maxSteps) {
        warnings.add("Trace stopped at the " + maxSteps + " step limit; the trace is partial.");
        stopTarget(virtualMachine);
        return false;
      }
    }

    if (!targetFinished && Instant.now().isAfter(deadline)) {
      warnings.add("JDI observation timed out after " + timeout.toSeconds() + " seconds.");
      stopTarget(virtualMachine);
    }
    return targetFinished;
  }

  private EventSet removeNextEventSet(VirtualMachine virtualMachine) {
    try {
      return virtualMachine.eventQueue().remove(EVENT_POLL_MILLIS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new DebugException("JDI event loop was interrupted.", ex);
    }
  }

  private void installBreakpoints(
      ReferenceType referenceType,
      EventRequestManager requestManager,
      AstAnalyzer.Analysis analysis,
      Set<ReferenceType> installedTypes,
      List<String> warnings) {
    if (!referenceType.name().equals(analysis.mainClassName())
        || !installedTypes.add(referenceType)) {
      return;
    }

    BreakpointMapper.MappingResult mappingResult =
        breakpointMapper.map(referenceType, analysis.observationPoints());
    warnings.addAll(mappingResult.warnings());
    for (BreakpointMapper.MappedBreakpoint mapping : mappingResult.mappings()) {
      BreakpointRequest request = requestManager.createBreakpointRequest(mapping.location());
      request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
      request.putProperty(OBSERVATION_POINT_PROPERTY, mapping.observationPoint());
      request.enable();
    }
  }

  private void captureVariables(
      BreakpointEvent breakpointEvent,
      AstAnalyzer.Analysis analysis,
      List<ExecutionObservation> executions,
      List<String> warnings,
      InvocationTracker invocations,
      IntervalValueProjection intervalProjection,
      IntegerListProjection integerListProjection) {
    Object property = breakpointEvent.request().getProperty(OBSERVATION_POINT_PROPERTY);
    if (!(property instanceof ObservationPoint observationPoint)) {
      warnings.add("A breakpoint event did not include its source observation metadata.");
      return;
    }

    ThreadReference eventThread = breakpointEvent.thread();
    try {
      // The frame comes from the event's own ThreadReference. This is essential when several target
      // threads hit independent breakpoints at approximately the same time.
      StackFrame frame = eventThread.frame(0);
      var location = breakpointEvent.location();
      int lineNumber = location.lineNumber();
      List<ObservationResult> variables =
          variableMapper.map(frame, observationPoint, analysis.variables(), lineNumber,
              intervalProjection, integerListProjection);
      ExecutionContext context =
          new ExecutionContext(
              eventThread.uniqueID(),
              eventThread.name(),
              location.declaringType().name(),
              location.method().name(),
              location.method().signature(),
              eventThread.frameCount(),
              location.codeIndex(),
              invocations.current(eventThread, location.method().name(),
                  location.method().signature()));
      executions.add(
          new ExecutionObservation(
              executions.size() + 1L,
              observationPoint.astNodeId(),
              lineNumber,
              observationPoint.startLine(),
              observationPoint.endLine(),
              observationPoint.startColumn(),
              observationPoint.endColumn(),
              observationPoint.statementKind(),
              observationPoint.code(),
              variables,
              context));
    } catch (AbsentInformationException ex) {
      throw new DebugException(
          "Target local-variable debug information is unavailable. Compile the target with -g:vars.",
          ex);
    } catch (IncompatibleThreadStateException | InvalidStackFrameException ex) {
      warnings.add(
          "Unable to inspect source line "
              + observationPoint.lineNumber()
              + " on its event thread: "
              + ex.getMessage());
    }
  }

  /** IDs are scoped by thread and allocated at method entry, including repeated sibling calls. */
  private static final class InvocationTracker {
    private record Active(String method, String signature, int depth, long id) {}

    private final Map<Long, Deque<Active>> active = new HashMap<>();
    private final Map<Long, Long> nextId = new HashMap<>();

    void enter(MethodEntryEvent event) {
      ThreadReference thread = event.thread();
      try {
        long threadId = thread.uniqueID();
        long id = nextId.merge(threadId, 1L, Long::sum);
        active.computeIfAbsent(threadId, unused -> new ArrayDeque<>())
            .addLast(new Active(event.method().name(), event.method().signature(),
                thread.frameCount(), id));
      } catch (IncompatibleThreadStateException ignored) {
        // A subsequent breakpoint can still allocate a stable fallback ID.
      }
    }

    void exit(MethodExitEvent event) {
      Deque<Active> stack = active.get(event.thread().uniqueID());
      if (stack == null) return;
      var iterator = stack.descendingIterator();
      while (iterator.hasNext()) {
        Active candidate = iterator.next();
        if (candidate.method().equals(event.method().name())
            && candidate.signature().equals(event.method().signature())) {
          iterator.remove();
          return;
        }
      }
    }

    Long current(ThreadReference thread, String method, String signature)
        throws IncompatibleThreadStateException {
      long threadId = thread.uniqueID();
      int depth = thread.frameCount();
      Deque<Active> stack = active.computeIfAbsent(threadId, unused -> new ArrayDeque<>());
      var iterator = stack.descendingIterator();
      while (iterator.hasNext()) {
        Active candidate = iterator.next();
        if (candidate.depth() == depth && candidate.method().equals(method)
            && candidate.signature().equals(signature)) return candidate.id();
      }
      long id = nextId.merge(threadId, 1L, Long::sum);
      stack.addLast(new Active(method, signature, depth, id));
      return id;
    }
  }

  private void stopTarget(VirtualMachine virtualMachine) {
    try {
      virtualMachine.exit(1);
    } catch (RuntimeException ignored) {
      // The process may already have terminated naturally between timeout detection and exit().
    }
  }

  private void resumeQuietly(EventSet eventSet) {
    try {
      eventSet.resume();
    } catch (VMDisconnectedException ignored) {
      // A VMDeathEvent/VMDisconnectEvent may make resume unnecessary.
    }
  }

  private void disposeQuietly(VirtualMachine virtualMachine) {
    if (virtualMachine == null) {
      return;
    }
    try {
      virtualMachine.dispose();
    } catch (VMDisconnectedException ignored) {
      // Normal after the target process exits.
    }
  }

  public record DebugRun(List<ExecutionObservation> executions, List<String> warnings,
      String output, String error, int exitCode, boolean partial) {
    public DebugRun(List<ExecutionObservation> executions, List<String> warnings) {
      this(executions, warnings, "", "", -1, true);
    }

    /** Compatibility projection consumed by the existing CLI and /api/debug endpoint. */
    public List<ObservationResult> results() {
      return executions.stream().flatMap(execution -> execution.variables().stream()).toList();
    }
  }
}
