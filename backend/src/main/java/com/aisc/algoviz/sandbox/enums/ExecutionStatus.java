package com.aisc.algoviz.sandbox.enums;

/**
 * Enum đại diện cho trạng thái kết quả thực thi mã nguồn trong Sandbox.
 */
public enum ExecutionStatus {
    /** Thực thi thành công không có lỗi */
    SUCCESS,

    /** Lỗi biên dịch (Compilation Error - CE) */
    COMPILATION_ERROR,

    /** Vượt quá giới hạn thời gian chạy (Time Limit Exceeded - TLE) */
    TIME_LIMIT_EXCEEDED,

    /** Lỗi khi chạy chương trình (Runtime Error - RE) như StackOverflowError, NullPointerException,... */
    RUNTIME_ERROR,

    /** Vi phạm chính sách an toàn (Security Violation) do chứa mã nguy hiểm */
    SECURITY_VIOLATION,

    /** Lỗi hệ thống Sandbox (Internal System Error) */
    SYSTEM_ERROR
}
