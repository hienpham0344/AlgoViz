package com.aisc.algoviz.sandbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

/**
 * Class DTO chứa thông tin yêu cầu thực thi mã nguồn gửi vào Sandbox.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequestDto {

    /** Code Java do học sinh/người dùng gửi lên */
    @NotBlank
    @Size(max = 200_000)
    private String code;

    /** Tên class chính chứa hàm main (mặc định là Main nếu null) */
    @Builder.Default
    @NotNull
    @Pattern(regexp = "[A-Za-z_$][A-Za-z0-9_$]*")
    private String mainClassName = "Main";

    /** Dữ liệu đầu vào tiêu chuẩn (stdin) cho bài toán */
    @Size(max = 1_048_576)
    private String input;

    /** Giới hạn thời gian chạy (tính bằng mili giây), mặc định 3000ms (3 giây) */
    @Builder.Default
    @Min(100) @Max(10_000)
    private long timeLimitMs = 3000L;

    /** Giới hạn bộ nhớ Heap JVM (tính bằng Megabytes), mặc định 64MB */
    @Builder.Default
    @Min(16) @Max(128)
    private int memoryLimitMb = 64;

    /** Giới hạn bộ nhớ Stack JVM (tính bằng Kilobytes), mặc định 512KB để cô lập đệ quy vô hạn */
    @Builder.Default
    @Min(256) @Max(2048)
    private int stackLimitKb = 512;
}
