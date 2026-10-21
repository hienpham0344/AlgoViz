package com.aisc.algoviz.problem.importer.config;

import com.aisc.algoviz.problem.importer.source.FileProblemDataSource;
import com.aisc.algoviz.problem.importer.source.JsonProblemDataReader;
import com.aisc.algoviz.problem.importer.source.MockProblemDataSource;
import com.aisc.algoviz.problem.importer.source.ProblemDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ProblemImportProperties.class)
public class ProblemImportConfiguration {

    private final ProblemImportProperties properties;

    @Bean
    public JsonProblemDataReader jsonProblemDataReader(
            com.fasterxml.jackson.databind.ObjectMapper objectMapper
    ) {
        return new JsonProblemDataReader(objectMapper);
    }

    @Bean
    public ProblemDataSource problemDataSource(JsonProblemDataReader reader) {
        // Chọn nguồn dữ liệu từ cấu hình để dữ liệu thật và fixture dùng chung một luồng import,
        // đồng thời không phải nhúng dữ liệu fixture vào code production.
        ProblemImportProperties.Source source = properties.getSource() != null ? properties.getSource() : ProblemImportProperties.Source.MOCK;
        return switch (source) {
            case FILE -> new FileProblemDataSource(properties, reader);
            case MOCK -> new MockProblemDataSource(properties, reader);
        };
    }

}
