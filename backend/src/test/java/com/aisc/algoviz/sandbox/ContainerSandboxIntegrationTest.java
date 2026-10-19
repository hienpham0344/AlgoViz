package com.aisc.algoviz.sandbox;

import com.aisc.algoviz.sandbox.runtime.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real Docker checks; deliberately opt-in and never run payloads on the host. */
@EnabledIfEnvironmentVariable(named="ALGOVIZ_RUN_SANDBOX_TESTS", matches="true")
class ContainerSandboxIntegrationTest {
    private final SandboxRuntime runtime = new SandboxRuntime("docker", "algoviz-runner:local");
    @TempDir Path host;
    private ContainerSandbox.Result java(String body) throws Exception {
        return runtime.execute("java", "class Main { public static void main(String[] args) throws Exception {" + body + "} }", "", "Main", 3000, 64, 512);
    }

    @Test void normalJavaAndEofWork() throws Exception {
        var result = java("System.out.print(new java.util.Scanner(System.in).hasNextLine() ? \"bad\" : \"EOF\");");
        assertEquals(0, result.exitCode(), result.stderr());
        assertEquals("EOF", result.stdout());
    }

    @Test void webRunKeepsJavaSourceLauncherCompatibility() {
        var service = new com.aisc.algoviz.service.CodeExecutionService(runtime);
        var result = service.runCodeCustomInput("java",
                "public class CustomName {public static void main(String[] args){System.out.print(42);}}", "");
        assertEquals("Success",result.getStatus(),result.getErrorMessage());
        assertEquals("42",result.getOutput());
    }

    @Test void inputAndOtherRuntimesWork() throws Exception {
        var py = runtime.execute("python", "print(int(input())+1)", "41", "Main", 3000,64,512);
        var js = runtime.execute("js", "console.log(6*7)", "", "Main",3000,64,512);
        var cpp = runtime.execute("cpp", "#include <iostream>\nint main(){std::cout<<42;}", "", "Main",3000,64,512);
        assertEquals("42",py.stdout().strip(),py.stderr());
        assertEquals("42",js.stdout().strip(),js.stderr());
        assertEquals("42",cpp.stdout().strip(),cpp.stderr());
        assertEquals(0,cpp.exitCode());
    }

    @Test void realHostFileCannotBeReadOrModified() throws Exception {
        Path secret=host.resolve("sentinel.txt");
        Files.writeString(secret,"host-sentinel");
        String path=secret.toAbsolutePath().toString().replace("\\","\\\\");
        var result=java("var p=java.nio.file.Path.of(\""+path+"\"); System.out.print(java.nio.file.Files.exists(p)); "
                + "try { java.nio.file.Files.writeString(p,\"changed\"); } catch(Exception expected) {} ");
        assertEquals(0,result.exitCode(),result.stderr());
        assertEquals("false",result.stdout());
        assertEquals("host-sentinel",Files.readString(secret));
    }

    @Test void rootInputAndDockerSocketAreNotWritableOrAvailable() throws Exception {
        var result=java("for(String p:new String[]{\"/escape.txt\",\"/input/Main.java\"}) {"
                + "try {java.nio.file.Files.writeString(java.nio.file.Path.of(p),\"bad\");System.out.print(\"WRITABLE\");}"
                + "catch(Exception expected){System.out.print(\"BLOCKED;\");}}"
                + "System.out.print(java.nio.file.Files.exists(java.nio.file.Path.of(\"/var/run/docker.sock\")));");
        assertEquals(0,result.exitCode(),result.stderr());
        assertEquals("BLOCKED;BLOCKED;false",result.stdout());
    }

    @Test void outboundNetworkIsBlocked() throws Exception {
        var result=java("try(var s=new java.net.Socket()){s.connect(new java.net.InetSocketAddress(\"1.1.1.1\",443),500);"
                + "System.out.print(\"CONNECTED\");}catch(java.io.IOException expected){System.out.print(\"BLOCKED\");}");
        assertEquals(0,result.exitCode(),result.stderr());
        assertEquals("BLOCKED",result.stdout());
    }

    @Test void subprocessCannotReachHostEither() throws Exception {
        var result=java("var p=new ProcessBuilder(\"sh\",\"-c\",\"id -u; test ! -e /var/run/docker.sock\").inheritIO().start();"
                + "if(p.waitFor()!=0)throw new Exception(\"unsafe\");");
        assertEquals(0,result.exitCode(),result.stderr());
        assertEquals("65534",result.stdout().strip());
    }

    @Test void kernelQuotasAndPrivilegeRestrictionsAreActive() throws Exception {
        var result=java("System.out.println(java.nio.file.Files.readString(java.nio.file.Path.of(\"/sys/fs/cgroup/memory.max\")).strip());"
                + "System.out.println(java.nio.file.Files.readString(java.nio.file.Path.of(\"/sys/fs/cgroup/pids.max\")).strip());"
                + "System.out.println(java.nio.file.Files.readString(java.nio.file.Path.of(\"/sys/fs/cgroup/cpu.max\")).strip());"
                + "System.out.print(java.nio.file.Files.readString(java.nio.file.Path.of(\"/proc/self/status\")));");
        assertEquals(0,result.exitCode(),result.stderr());
        assertTrue(result.stdout().contains("268435456\n64\n100000 100000"),result.stdout());
        assertTrue(result.stdout().contains("NoNewPrivs:\t1"));
        assertTrue(result.stdout().contains("CapEff:\t0000000000000000"));
    }

    @Test void excessiveHeapAllocationIsRejected() throws Exception {
        var result=java("byte[] tooLarge=new byte[128*1024*1024];System.out.print(tooLarge.length);");
        assertNotEquals(0,result.exitCode());
        assertTrue(result.stderr().contains("OutOfMemoryError"),result.stderr());
    }

    @Test void infiniteProgramIsKilledAndNoJobRemains() throws Exception {
        var result=runtime.execute("java","class Main {public static void main(String[]a){while(true){}}}","","Main",500,64,512);
        assertTrue(result.timedOut() || result.exitCode()==137 || result.exitCode()==124, result.toString());
        assertNoContainers();
    }

    @Test void outputIsBoundedAndContainerIsRemoved() throws Exception {
        var result=java("System.out.print(\"x\".repeat(2_000_000));");
        assertTrue(result.outputExceeded());
        assertTrue(result.stdout().length()<=1_048_576);
        assertNoContainers();
    }

    @Test void writableDataDoesNotSurviveNextJob() throws Exception {
        assertEquals(0,java("java.nio.file.Files.writeString(java.nio.file.Path.of(\"/work/private.txt\"),\"secret\");").exitCode());
        assertEquals("false",java("System.out.print(java.nio.file.Files.exists(java.nio.file.Path.of(\"/work/private.txt\")));").stdout());
    }

    @Test void missingImageFailsClosed() {
        var missing=new ContainerSandbox("docker","algoviz-image-does-not-exist:local",1024);
        assertThrows(java.io.IOException.class,()->missing.run(Map.of("Main.java","class Main {}"),List.of("run"),Duration.ofSeconds(1),256));
    }

    private static void assertNoContainers() throws Exception {
        var process=new ProcessBuilder("docker","ps","-aq","--filter","label=algoviz.sandbox=true").start();
        String output=new String(process.getInputStream().readAllBytes());
        assertEquals(0,process.waitFor());
        assertTrue(output.isBlank(),output);
    }
}
