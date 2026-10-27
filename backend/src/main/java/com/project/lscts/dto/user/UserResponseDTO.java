
package com.project.lscts.dto.user;

import com.project.lscts.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Long userId;

    private String name;

    private String email;

    private String phoneNumber;

    private Role role;

    private boolean passwordSetupCompleted;

    private LocalDateTime userCreatedAt;
}
