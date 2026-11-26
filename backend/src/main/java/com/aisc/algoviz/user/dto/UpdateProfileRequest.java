package com.aisc.algoviz.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 100)
    private String fullName;
    @Email
    @Pattern(regexp = ".*\\S.*", message = "Email must not be blank")
    @Size(max = 254)
    private String email;
    @Size(max = 2048)
    private String avatarUrl;
    @Size(max = 100)
    private String roleTitle;
    @Size(max = 2000)
    private String bio;
    @Size(max = 100)
    private String location;
    @Size(max = 200)
    private String target;
}
