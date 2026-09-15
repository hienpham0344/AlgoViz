package com.aisc.algoviz.sandbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class DTO chứa thông tin yêu cầu thực thi mã nguồn gửi vào Sandbox.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequestDto {

    /** Code Java do học sinh/người dùng gửi lên */
    private String code;

    /** Tên class chính chứa hàm main (mặc định là Main nếu null) */
    @Builder.Default
    private String mainClassName = "Main";

    /** Dữ liệu đầu vào tiêu chuẩn (stdin) cho bài toán */
    private String input;

    /** Giới hạn thời gian chạy (tính bằng mili giây), mặc định 3000ms (3 giây) */
    @Builder.Default
    private long timeLimitMs = 3000L;

    /** Giới hạn bộ nhớ Heap JVM (tính bằng Megabytes), mặc định 64MB */
    @Builder.Default
    private int memoryLimitMb = 64;

    /** Giới hạn bộ nhớ Stack JVM (tính bằng Kilobytes), mặc định 512KB để cô lập đệ quy vô hạn */
    @Builder.Default
    private int stackLimitKb = 512;
}
