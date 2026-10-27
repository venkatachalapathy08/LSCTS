package com.project.lscts.dto.complaint;

import com.project.lscts.entity.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintResponseDTO {

    private Long complaintId;

    private Long userId;

    private String username;

    private String title;

    private String description;

    private ComplaintStatus status;

    private LocalDateTime complaintCreatedAt;

    private LocalDateTime updatedAt;
}
