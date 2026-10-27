package com.project.lscts.dto.auth;

import com.project.lscts.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {

    private Long id;

    private String name;

    private String token;

    private Role role;
}
