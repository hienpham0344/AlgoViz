package com.aisc.algoviz.problem.importer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình bên ngoài cho chức năng import problem và knowledge-base solution.
 *
 * <p>Import là tính năng phải chủ động bật. Mặc định được tắt trong {@code application.yaml}
 * để việc chạy ứng dụng hoặc test thông thường không vô tình ghi dataset vào database.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "algoviz.import")
public class ProblemImportProperties {

    private boolean enabled = false;
    private Source source = Source.MOCK;
    private String dataDirectory = "./data";
    private String problemsFile = "problems.json";
    private String solutionsFile = "solutions.json";
    private String mockDataDirectory = "./data/mock";
    private String mockProblemsFile = "problems.json";
    private String mockSolutionsFile = "solutions.json";

    public enum Source {
        FILE,
        MOCK
    }
}
