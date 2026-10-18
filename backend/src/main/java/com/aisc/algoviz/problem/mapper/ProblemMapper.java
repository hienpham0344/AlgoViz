package com.aisc.algoviz.problem.mapper;

import com.aisc.algoviz.problem.dto.request.CreateProblemRequestDto;
import com.aisc.algoviz.problem.dto.request.CreateSolutionRequestDto;
import com.aisc.algoviz.problem.dto.request.UpdateProblemRequestDto;
import com.aisc.algoviz.problem.dto.response.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.response.ProblemSummaryDto;
import com.aisc.algoviz.problem.dto.response.SolutionResponseDto;
import com.aisc.algoviz.problem.entity.Problem;
import com.aisc.algoviz.problem.entity.Solution;
import org.mapstruct.*;

import java.util.Locale;

/**
 * Interface Mapper chuyên trách chuyển đổi dữ liệu hai chiều giữa Entity (Database) và DTOs.
 * Triển khai tự động bằng thư viện MapStruct.
 *
 * @author AlgoViz Development Team
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProblemMapper {

    /**
     * Chuyển đổi từ Problem Entity sang ProblemSummaryDto (Dành cho trang danh sách).
     */
    ProblemSummaryDto toSummaryDto(Problem problem);

    /**
     * Chuyển đổi từ Problem Entity sang ProblemDetailDto (Dành cho màn chi tiết bài toán).
     */
    ProblemDetailDto toDetailDto(Problem problem);

    /**
     * Chuyển đổi từ Solution Entity sang SolutionResponseDto.
     */
    SolutionResponseDto toSolutionResponseDto(Solution solution);

    /**
     * Chuyển đổi từ CreateProblemRequestDto sang Problem Entity để lưu Database.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "slug", expression = "java(mapSlug(dto.getSlug(), dto.getTitle()))")
    @Mapping(target = "solutions", source = "solutions")
    Problem toEntity(CreateProblemRequestDto dto);

    /**
     * Chuyển đổi từ CreateSolutionRequestDto sang Solution Entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "problem", source = "problem")
    @Mapping(target = "language", expression = "java(dto.getLanguage() != null && !dto.getLanguage().isBlank() ? dto.getLanguage() : \"Java\")")
    Solution toSolutionEntity(CreateSolutionRequestDto dto, Problem problem);

    /**
     * Cập nhật thông tin bài toán Entity có sẵn từ UpdateProblemRequestDto.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "solutions", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpdateProblemRequestDto dto, @MappingTarget Problem problem);

    /**
     * Thiết lập liên kết hai chiều giữa Problem và Solution sau khi MapStruct chuyển đổi List Solutions.
     */
    @AfterMapping
    default void linkSolutions(@MappingTarget Problem problem) {
        if (problem.getSolutions() != null) {
            problem.getSolutions().forEach(solution -> solution.setProblem(problem));
        }
    }

    /**
     * Utility map slug: Nếu DTO cung cấp slug thì dùng, ngược lại tự động tạo từ title.
     */
    default String mapSlug(String slug, String title) {
        if (slug != null && !slug.isBlank()) {
            return slug.trim();
        }
        return generateSlug(title);
    }

    /**
     * Hàm tiện ích tự động sinh ra chuỗi Slug URL chuẩn SEO từ Tiêu đề bài toán (Title).
     */
    default String generateSlug(String title) {
        if (title == null || title.isBlank()) return "";
        return title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
