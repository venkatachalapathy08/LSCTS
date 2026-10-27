
package com.project.lscts.dto.complaint;

import com.project.lscts.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatusUpdateRequestDTO {

    @NotNull(message = "Status is required")
    private ComplaintStatus status;
}
