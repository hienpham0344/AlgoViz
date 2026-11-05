package com.aisc.algoviz.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String roleTitle;
    private String bio;
    private String location;
    private String target;
    private String role;
    private Integer points;
    private Integer solvedProblemsCount;
}
