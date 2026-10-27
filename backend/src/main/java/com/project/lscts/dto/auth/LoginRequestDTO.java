package com.project.lscts.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "password is required")
    private String password;
}
