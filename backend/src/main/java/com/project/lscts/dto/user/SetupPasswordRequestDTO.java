
package com.project.lscts.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SetupPasswordRequestDTO {

    @NotBlank(message = "Setup token is required")
    private String setupToken;

    @NotBlank(message = "Password is required")
    @Size(
            min = 8,
            max = 72,
            message = "Password must contain 8 to 72 characters"
    )
    private String newPassword;
}
