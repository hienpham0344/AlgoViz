package com.aisc.algoviz.sandbox.service;

import com.aisc.algoviz.sandbox.dto.ExecutionRequestDto;
import com.aisc.algoviz.sandbox.dto.ExecutionResultDto;

/**
 * Interface định nghĩa dịch vụ Sandbox thực thi mã nguồn.
 */
public interface SandboxService {

    /**
     * Nhận mã nguồn Java của học sinh, thực hiện kiểm tra an toàn, biên dịch và chạy trong Sandbox.
     *
     * @param request Yêu cầu thực thi mã nguồn
     * @return ExecutionResultDto Kết quả thực thi
     */
    ExecutionResultDto executeCode(ExecutionRequestDto request);
}
