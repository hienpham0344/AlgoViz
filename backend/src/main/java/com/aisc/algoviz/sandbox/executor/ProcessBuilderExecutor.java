package com.aisc.algoviz.sandbox.executor;

import com.aisc.algoviz.sandbox.dto.ExecutionRequestDto;
import com.aisc.algoviz.sandbox.dto.ExecutionResultDto;
import com.aisc.algoviz.sandbox.enums.ExecutionStatus;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Thuật toán thực thi mã nguồn bằng ProcessBuilder (Bước 2 & Bước 3).
 * Lớp này cô lập chương trình của học sinh bằng cách khởi tạo một Process JVM độc lập,
 * áp dụng các cờ giới hạn tài nguyên (-Xmx, -Xss), truyền dữ liệu stdin, kiểm soát timeout
 * và thu thập kết quả stdout/stderr.
 */
@Component
public class ProcessBuilderExecutor {

    /**
     * Thuật toán chính chạy chương trình đã biên dịch trong một Process con riêng biệt.
     *
     * @param request DTO chứa các thông số cấu hình execution (timeLimit, memoryLimit, input,...)
     * @param compiledClassDir Đường dẫn thư mục chứa file .class vừa được biên dịch
     * @return ExecutionResultDto chứa trạng thái, thời gian chạy, stdout, stderr
     */
    public ExecutionResultDto execute(ExecutionRequestDto request, Path compiledClassDir) {
        // Thuật toán Bước 3.1: Xác định đường dẫn file thực thi java của hệ thống JDK hiện tại
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

        // Thuật toán Bước 3.2: Xây dựng lệnh khởi chạy JVM con kèm theo các cờ (JVM Flags) giới hạn tài nguyên
        List<String> command = new ArrayList<>();
        command.add(javaBin);

        // Cờ -Xmx: Giới hạn bộ nhớ Heap tối đa (Heap Limit) -> Giảm thiểu rủi ro cạn kiệt RAM server
        command.add("-Xmx" + request.getMemoryLimitMb() + "m");

        // Cờ -Xss: Giới hạn bộ nhớ Stack (Stack Size Limit) -> Cô lập lỗi đệ quy vô hạn (StackOverflowError)
        command.add("-Xss" + request.getStackLimitKb() + "k");

        // Cờ -cp: Thiết lập classpath trỏ đến thư mục tạm chứa file .class
        command.add("-cp");
        command.add(compiledClassDir.toAbsolutePath().toString());

        // Tên class chính chứa hàm main
        command.add(request.getMainClassName());

        // Thuật toán Bước 2.1: Khởi tạo ProcessBuilder với danh sách câu lệnh
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        // Đặt thư mục làm việc của process con
        processBuilder.directory(compiledClassDir.toFile());

        Process process = null;
        ExecutorService streamReadExecutor = Executors.newFixedThreadPool(2);

        try {
            long startTime = System.currentTimeMillis();

            // Thuật toán Bước 2.2: Kích hoạt chạy Process con (.start())
            process = processBuilder.start();

            // Thuật toán Bước 2.3: Ghi dữ liệu đầu vào (stdin) cho process con (nếu có bài test input)
            if (request.getInput() != null && !request.getInput().isEmpty()) {
                try (OutputStream os = process.getOutputStream();
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
                    writer.write(request.getInput());
                    writer.flush();
                } // Đóng OutputStream để phát tín hiệu EOF (End Of File) cho chương trình con
            } else {
                process.getOutputStream().close();
            }

            // Thuật toán Bước 2.4: Đọc InputStream (stdout) và ErrorStream (stderr) bất đồng bộ
            // Việc đọc bất đồng bộ qua Future giúp tránh tràn bộ đệm OS Buffer gây treo (deadlock) process con
            Future<String> stdoutFuture = streamReadExecutor.submit(new StreamGobbler(process.getInputStream()));
            Future<String> stderrFuture = streamReadExecutor.submit(new StreamGobbler(process.getErrorStream()));

            // Thuật toán Bước 2.5: Giờ chờ và kiểm soát thời gian giới hạn (Timeout Limit - waitFor)
            boolean completedOnTime = process.waitFor(request.getTimeLimitMs(), TimeUnit.MILLISECONDS);
            long executionTimeMs = System.currentTimeMillis() - startTime;

            if (!completedOnTime) {
                // Thuật toán Bước 2.6: Ép diệt process con lập tức nếu vượt quá thời gian cho phép (destroyForcibly)
                process.destroyForcibly();
                // Đảm bảo process đã chết hoàn toàn
                process.waitFor(500, TimeUnit.MILLISECONDS);

                return ExecutionResultDto.builder()
                        .status(ExecutionStatus.TIME_LIMIT_EXCEEDED)
                        .stdout(readFutureOutput(stdoutFuture))
                        .stderr("Lỗi: Chương trình thực thi quá thời gian cho phép (" + request.getTimeLimitMs() + " ms).")
                        .executionTimeMs(request.getTimeLimitMs())
                        .exitCode(-1)
                        .build();
            }

            // Lấy mã thoát của process con (0 = Thành công, Khác 0 = Có lỗi runtime exception)
            int exitCode = process.exitValue();
            String stdout = readFutureOutput(stdoutFuture);
            String stderr = readFutureOutput(stderrFuture);

            if (exitCode == 0) {
                // Chương trình chạy thành công hoàn toàn
                return ExecutionResultDto.builder()
                        .status(ExecutionStatus.SUCCESS)
                        .stdout(stdout)
                        .stderr(stderr)
                        .executionTimeMs(executionTimeMs)
                        .exitCode(exitCode)
                        .build();
            } else {
                // Chương trình gặp lỗi Runtime Error (Vd: NullPointerException, StackOverflowError, ArrayIndexOutOfBoundsException)
                return ExecutionResultDto.builder()
                        .status(ExecutionStatus.RUNTIME_ERROR)
                        .stdout(stdout)
                        .stderr(stderr)
                        .executionTimeMs(executionTimeMs)
                        .exitCode(exitCode)
                        .build();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            return ExecutionResultDto.builder()
                    .status(ExecutionStatus.SYSTEM_ERROR)
                    .stderr("Luồng thực thi bị ngắt đoạn: " + e.getMessage())
                    .build();
        } catch (IOException e) {
            return ExecutionResultDto.builder()
                    .status(ExecutionStatus.SYSTEM_ERROR)
                    .stderr("Lỗi khởi tạo process con: " + e.getMessage())
                    .build();
        } finally {
            // Dọn dẹp ThreadPool thu thập stream
            streamReadExecutor.shutdownNow();
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Helper đọc nội dung từ Stream Future an toàn.
     */
    private String readFutureOutput(Future<String> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Thuật toán đọc Stream bất đồng bộ (Stream Gobbler).
     * Thu thập từng dòng dữ liệu đầu ra từ process con để không làm tắc nghẽn bộ đệm I/O.
     */
    private static class StreamGobbler implements Callable<String> {
        private final InputStream inputStream;

        public StreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public String call() {
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append(System.lineSeparator());
                }
            } catch (IOException ignored) {
                // Bỏ qua lỗi ngắt kết nối stream khi process bị destroyForcibly
            }
            return builder.toString().trim();
        }
    }
}
