package com.aisc.algoviz.service;

import com.aisc.algoviz.dto.AiAnalysisRequest;
import com.aisc.algoviz.dto.AiAnalysisResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiAnalysisService {

    public AiAnalysisResponse analyzeCode(AiAnalysisRequest request) {
        String code = request.getCode();
        String lang = request.getLanguage() != null ? request.getLanguage().toLowerCase() : "python";

        String timeComplexity = "O(N)";
        String spaceComplexity = "O(1)";
        List<String> suggestions = new ArrayList<>();
        List<String> potentialBugs = new ArrayList<>();

        if (code.contains("for") && code.contains("range") || code.contains("while")) {
            long loopCount = code.lines().filter(l -> l.contains("for") || l.contains("while")).count();
            if (loopCount >= 2) {
                timeComplexity = "O(N²)";
                suggestions.add("Bạn đang sử dụng vòng lặp lồng nhau (Nested loops). Cân nhắc dùng Hash Map hoặc Two Pointers để giảm độ phức tạp xuống O(N).");
            } else {
                timeComplexity = "O(N)";
                suggestions.add("Thuật toán đang chạy tuyến tính O(N). Cấu trúc vòng lặp khá tối ưu.");
            }
        } else {
            timeComplexity = "O(1)";
            suggestions.add("Thuật toán đạt độ phức tạp hằng số O(1). Rất tối ưu!");
        }

        if (code.contains("append") || code.contains("push") || code.contains("new List") || code.contains("vector")) {
            spaceComplexity = "O(N)";
            suggestions.add("Sử dụng thêm mảng/danh sách phụ thuộc vào kích thước đầu vào -> Bộ nhớ O(N).");
        } else {
            spaceComplexity = "O(1)";
        }

        if (!code.contains("return") && (lang.contains("py") || lang.contains("js") || lang.contains("java") || lang.contains("cpp"))) {
            potentialBugs.add("Chưa thấy câu lệnh `return`. Đảm bảo hàm của bạn có trả về kết quả.");
        }

        if (code.contains("range(len(") || code.contains(".length")) {
            potentialBugs.add("Chú ý trường hợp mảng rỗng (empty input array) hoặc có 1 phần tử.");
        }

        String summary = String.format("Mã nguồn %s của bạn có độ phức tạp thời gian %s và bộ nhớ %s. %s",
                lang, timeComplexity, spaceComplexity,
                timeComplexity.equals("O(N²)") ? "Nên tối ưu thêm để tránh bị Time Limit Exceeded với n > 10^5." : "Mã nguồn gọn gàng!");

        return AiAnalysisResponse.builder()
                .timeComplexity(timeComplexity)
                .spaceComplexity(spaceComplexity)
                .summary(summary)
                .optimizationSuggestions(suggestions)
                .potentialBugs(potentialBugs)
                .refactoredCode(code)
                .build();
    }
}
