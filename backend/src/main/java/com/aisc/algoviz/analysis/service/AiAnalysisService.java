package com.aisc.algoviz.analysis.service;

import com.aisc.algoviz.analysis.dto.AiAnalysisRequest;
import com.aisc.algoviz.analysis.dto.AiAnalysisResponse;
import com.aisc.algoviz.ast.engine.algorithm.AlgorithmPatternAnalyzer;
import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.exception.CodeParseException;
import com.aisc.algoviz.ast.engine.model.AlgorithmHint;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AiAnalysisService {

    private final AstAnalyzer astAnalyzer;
    private final AlgorithmPatternAnalyzer patternAnalyzer;

    public AiAnalysisService() {
        this(new AstAnalyzer(), new AlgorithmPatternAnalyzer());
    }

    @Autowired
    public AiAnalysisService(AstAnalyzer astAnalyzer, AlgorithmPatternAnalyzer patternAnalyzer) {
        this.astAnalyzer = astAnalyzer;
        this.patternAnalyzer = patternAnalyzer;
    }

    private record ComplexityProfile(String time, String space, String guidance) {}

    public AiAnalysisResponse analyzeCode(AiAnalysisRequest request) {
        String code = request.getCode() != null ? request.getCode() : "";
        String lang = request.getLanguage() != null ? request.getLanguage().trim().toLowerCase(Locale.ROOT) : "python";

        if (isJavaLanguage(lang)) {
            return analyzeJavaWithAst(code, lang);
        }

        return analyzeWithHeuristics(code, lang, new ArrayList<>(), new ArrayList<>());
    }

    private boolean isJavaLanguage(String lang) {
        return !lang.contains("javascript") && (lang.equals("java") || lang.startsWith("java "));
    }

    private AiAnalysisResponse analyzeJavaWithAst(String code, String lang) {
        List<String> suggestions = new ArrayList<>();
        List<String> potentialBugs = new ArrayList<>();

        CompilationUnit unit;
        try {
            unit = parseJavaResiliently(code);
        } catch (CodeParseException ex) {
            potentialBugs.add("Cú pháp Java chưa hoàn chỉnh (" + ex.getMessage() + "). Đã chuyển sang chế độ phân tích heuristic.");
            return analyzeWithHeuristics(code, lang, suggestions, potentialBugs);
        }

        List<AlgorithmHint> hints = patternAnalyzer.analyze(unit);
        int maxLoopDepth = computeMaxLoopNesting(unit);
        boolean hasRecursion = hasRecursiveCall(unit);
        boolean hasDynamicAllocation = hasAuxiliaryAllocation(unit);

        String timeComplexity;
        String spaceComplexity;
        String detectedSummaryPrefix = "";

        if (!hints.isEmpty()) {
            AlgorithmHint primary = hints.stream()
                    .max(Comparator.comparingDouble(AlgorithmHint::confidence))
                    .orElse(hints.get(0));
            ComplexityProfile profile = complexityForHint(primary.type(), maxLoopDepth, hasDynamicAllocation);
            timeComplexity = profile.time();
            spaceComplexity = profile.space();

            int confidencePct = (int) Math.round(primary.confidence() * 100);
            detectedSummaryPrefix = String.format(
                    "AST Static Analyzer nhận diện mẫu thuật toán '%s' (độ tin cậy %d%%, visualPlan: %s, dòng %d-%d). ",
                    primary.type(), confidencePct, primary.visualPlan(), primary.startLine(), primary.endLine()
            );

            suggestions.add(String.format(
                    "Mẫu thuật toán phát hiện qua AST: %s (%d%% confidence). %s",
                    primary.type(), confidencePct, profile.guidance()
            ));
            for (String ev : primary.evidence()) {
                suggestions.add("Bằng chứng AST: " + ev);
            }
        } else {
            if (maxLoopDepth >= 3) {
                timeComplexity = "O(N³)";
                suggestions.add("Phát hiện cấu trúc vòng lặp lồng nhau bậc " + maxLoopDepth + " qua cây AST. Cân nhắc quy hoạch động hoặc tiền xử lý để giảm bậc.");
            } else if (maxLoopDepth == 2) {
                timeComplexity = "O(N²)";
                suggestions.add("Bạn đang sử dụng vòng lặp lồng nhau (Nested loops). Cân nhắc dùng Hash Map hoặc Two Pointers để giảm độ phức tạp xuống O(N).");
            } else if (maxLoopDepth == 1) {
                timeComplexity = "O(N)";
                suggestions.add("Thuật toán đang chạy tuyến tính O(N) với 1 cấp vòng lặp trong AST. Cấu trúc vòng lặp khá tối ưu.");
            } else if (hasRecursion) {
                timeComplexity = "O(N)";
                suggestions.add("Phát hiện lời gọi đệ quy trong phương thức. Hãy kiểm tra điều kiện dừng (base case) và độ sâu ngăn xếp.");
            } else {
                timeComplexity = "O(1)";
                suggestions.add("Thuật toán đạt độ phức tạp hằng số O(1). Rất tối ưu!");
            }

            if (hasDynamicAllocation) {
                spaceComplexity = "O(N)";
                suggestions.add("Sử dụng thêm cấu trúc dữ liệu cấp phát động (mảng/collection) trong AST -> Bộ nhớ O(N).");
            } else if (hasRecursion) {
                spaceComplexity = "O(N)";
                suggestions.add("Đệ quy tiêu tốn không gian ngăn xếp lời gọi (Call Stack) -> Bộ nhớ O(N).");
            } else {
                spaceComplexity = "O(1)";
            }
        }

        checkJavaAstPotentialBugs(unit, code, potentialBugs);

        String summary = String.format(
                "%sMã nguồn %s của bạn có độ phức tạp thời gian %s và bộ nhớ %s. %s",
                detectedSummaryPrefix,
                lang,
                timeComplexity,
                spaceComplexity,
                timeComplexity.equals("O(N²)") || timeComplexity.equals("O(N³)") || timeComplexity.equals("O(2^N)")
                        ? "Nên tối ưu thêm để tránh bị Time Limit Exceeded với n > 10^5."
                        : "Mã nguồn có độ phức tạp tốt!"
        );

        return AiAnalysisResponse.builder()
                .timeComplexity(timeComplexity)
                .spaceComplexity(spaceComplexity)
                .summary(summary)
                .optimizationSuggestions(suggestions)
                .potentialBugs(potentialBugs)
                .refactoredCode(code)
                .build();
    }

    private CompilationUnit parseJavaResiliently(String code) {
        try {
            return astAnalyzer.parseCompilationUnit(code);
        } catch (CodeParseException firstError) {
            String wrapped = "public class __AlgoVizAnalysisWrapper {\n" + code + "\n}";
            try {
                return astAnalyzer.parseCompilationUnit(wrapped);
            } catch (CodeParseException ignored) {
                throw firstError;
            }
        }
    }

    private int computeMaxLoopNesting(CompilationUnit unit) {
        int maxDepth = 0;
        for (Node node : unit.findAll(Node.class, AiAnalysisService::isLoopNode)) {
            int depth = 1;
            Node parent = node.getParentNode().orElse(null);
            while (parent != null) {
                if (isLoopNode(parent)) {
                    depth++;
                }
                parent = parent.getParentNode().orElse(null);
            }
            maxDepth = Math.max(maxDepth, depth);
        }
        return maxDepth;
    }

    private static boolean isLoopNode(Node node) {
        return node instanceof ForStmt
                || node instanceof WhileStmt
                || node instanceof ForEachStmt
                || node instanceof DoStmt;
    }

    private boolean hasRecursiveCall(CompilationUnit unit) {
        for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
            String name = method.getNameAsString();
            int paramCount = method.getParameters().size();
            boolean callsSelf = method.findAll(MethodCallExpr.class).stream()
                    .anyMatch(call -> call.getNameAsString().equals(name)
                            && call.getArguments().size() == paramCount);
            if (callsSelf) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAuxiliaryAllocation(CompilationUnit unit) {
        boolean hasCollectionAlloc = unit.findAll(ObjectCreationExpr.class).stream()
                .map(expr -> expr.getType().asString())
                .anyMatch(type -> type.contains("Map") || type.contains("Set")
                        || type.contains("List") || type.contains("Queue")
                        || type.contains("Deque") || type.contains("Stack"));
        if (hasCollectionAlloc) {
            return true;
        }
        return !unit.findAll(ArrayCreationExpr.class).isEmpty();
    }

    private void checkJavaAstPotentialBugs(CompilationUnit unit, String code, List<String> potentialBugs) {
        for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
            if (!method.getType().isVoidType() && method.getBody().isPresent()) {
                boolean hasReturn = !method.getBody().get().findAll(ReturnStmt.class).isEmpty();
                if (!hasReturn) {
                    potentialBugs.add("Phương thức `" + method.getNameAsString() + "` có kiểu trả về `"
                            + method.getTypeAsString() + "` nhưng chưa có câu lệnh `return`.");
                }
            }
        }
        if (code.contains(".length")) {
            potentialBugs.add("Chú ý kiểm tra trường hợp mảng rỗng (`length == 0`) hoặc `null` trước khi truy cập chỉ số.");
        }
    }

    private ComplexityProfile complexityForHint(String hintType, int loopDepth, boolean hasAuxAlloc) {
        return switch (hintType) {
            case "binary-search" -> new ComplexityProfile("O(log N)", "O(1)",
                    "Chia đôi không gian tìm kiếm ở mỗi bước đạt hiệu năng logarit tối ưu.");
            case "linear-search" -> new ComplexityProfile("O(N)", "O(1)",
                    "Duyệt tuần tự O(N); nếu mảng đã sắp xếp, có thể nâng cấp lên Binary Search O(log N).");
            case "bubble-sort", "selection-sort", "insertion-sort" -> new ComplexityProfile("O(N²)", "O(1)",
                    "Thuật toán sắp xếp bậc hai tại chỗ (in-place). Với dữ liệu lớn (N > 10^4), cân nhắc Merge Sort hoặc Quick Sort O(N log N).");
            case "quick-sort" -> new ComplexityProfile("O(N log N)", "O(log N)",
                    "Chia để trị qua chốt (pivot); lưu ý chọn pivot ngẫu nhiên hoặc median-of-three để tránh worst-case O(N²).");
            case "merge-sort" -> new ComplexityProfile("O(N log N)", "O(N)",
                    "Chia để trị ổn định (stable sort) với độ phức tạp O(N log N) trong mọi trường hợp.");
            case "heap-sort" -> new ComplexityProfile("O(N log N)", "O(1)",
                    "Sắp xếp vun đống tại chỗ tối ưu cả thời gian O(N log N) và bộ nhớ O(1).");
            case "breadth-first-search", "depth-first-search", "topological-sort" -> new ComplexityProfile("O(V + E)", "O(V)",
                    "Duyệt đồ thị tuyến tính theo số đỉnh và cạnh.");
            case "dijkstra" -> new ComplexityProfile("O((V + E) log V)", "O(V)",
                    "Tìm đường đi ngắn nhất với hàng đợi ưu tiên (PriorityQueue).");
            case "floyd-warshall" -> new ComplexityProfile("O(V³)", "O(V²)",
                    "Quy hoạch động mọi cặp đường đi ngắn nhất với 3 vòng lặp lồng nhau.");
            case "union-find" -> new ComplexityProfile("O(α(N))", "O(N)",
                    "Cấu trúc Disjoint Set Union đạt độ phức tạp gần như hằng số mỗi truy vấn.");
            case "fibonacci-dp" -> new ComplexityProfile("O(N)", "O(N)",
                    "Quy hoạch động 1 chiều; có thể tối ưu bộ nhớ xuống O(1) bằng 2 biến trạng thái.");
            case "coin-change", "knapsack" -> new ComplexityProfile("O(N · W)", "O(W)",
                    "Quy hoạch động giả đa thức theo số phần tử và dung lượng/mục tiêu.");
            case "longest-increasing-subsequence", "longest-common-subsequence", "edit-distance" ->
                    new ComplexityProfile("O(N²)", "O(N²)",
                            "Quy hoạch động trên chuỗi/dãy; có thể tối ưu không gian bảng DP hoặc dùng Binary Search (O(N log N) cho LIS).");
            case "activity-selection" -> new ComplexityProfile("O(N log N)", "O(1)",
                    "Tham lam (Greedy) lựa chọn hoạt động tối ưu sau khi sắp xếp theo thời điểm kết thúc.");
            case "jump-game" -> new ComplexityProfile("O(N)", "O(1)",
                    "Tham lam cập nhật tầm với xa nhất trong một lần duyệt O(N).");
            case "backtracking" -> new ComplexityProfile("O(2^N)", "O(N)",
                    "Quay lui vét cạn không gian trạng thái; hãy cắt nhánh sớm (pruning) để giảm số bước.");
            case "kmp" -> new ComplexityProfile("O(N + M)", "O(M)",
                    "Đối sánh chuỗi tuyến tính sử dụng mảng tiền tố LPS.");
            case "prefix-sum", "kadane", "two-pointers", "sliding-window" ->
                    new ComplexityProfile("O(N)", hasAuxAlloc && hintType.equals("prefix-sum") ? "O(N)" : "O(1)",
                            "Kỹ thuật duyệt mảng tuyến tính O(N) rất hiệu quả.");
            default -> new ComplexityProfile(loopDepth >= 2 ? "O(N²)" : "O(N)", hasAuxAlloc ? "O(N)" : "O(1)",
                    "Đã phân tích cấu trúc AST.");
        };
    }

    private AiAnalysisResponse analyzeWithHeuristics(String code, String lang, List<String> suggestions, List<String> potentialBugs) {
        String timeComplexity;
        String spaceComplexity;

        if (code.contains("for") && code.contains("range") || code.contains("while") || code.contains("for")) {
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

        if (code.contains("append") || code.contains("push") || code.contains("new List") || code.contains("vector") || code.contains("HashMap") || code.contains("dict")) {
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
