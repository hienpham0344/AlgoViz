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

    private boolean enabled;
    private Source source;
    private String dataDirectory;
    private String problemsFile;
    private String solutionsFile;
    private String mockDataDirectory;
    private String mockProblemsFile;
    private String mockSolutionsFile;

    public enum Source {
        FILE,
        MOCK
    }
}
