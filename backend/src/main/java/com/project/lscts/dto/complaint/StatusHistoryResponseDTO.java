package com.project.lscts.dto.complaint;

import com.project.lscts.entity.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusHistoryResponseDTO {

    private Long historyId;

    private Long complaintId;

    private ComplaintStatus currentStatus;

    private String remarks;

    private Long updatedByUserId;

    private String updatedByName;

    private LocalDateTime updatedAt;
}