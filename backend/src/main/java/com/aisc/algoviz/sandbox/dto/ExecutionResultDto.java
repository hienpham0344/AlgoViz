package com.aisc.algoviz.sandbox.dto;

import com.aisc.algoviz.sandbox.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class DTO chứa kết quả phản hồi sau khi Sandbox chạy xong code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResultDto {

    /** Trạng thái kết quả thực thi (SUCCESS, COMPILATION_ERROR, TIME_LIMIT_EXCEEDED, ...) */
    private ExecutionStatus status;

    /** Đầu ra tiêu chuẩn (stdout) thu được từ chương trình */
    private String stdout;

    /** Đầu ra lỗi (stderr) thu được từ chương trình (ví dụ: Exception trace) */
    private String stderr;

    /** Thông tin chi tiết lỗi biên dịch nếu status == COMPILATION_ERROR */
    private String compilerErrorDetails;

    /** Thời gian thực thi thực tế tính bằng mili giây */
    private long executionTimeMs;

    /** Mã thoát của process con (0 là bình thường, khác 0 là có lỗi) */
    private Integer exitCode;
}
