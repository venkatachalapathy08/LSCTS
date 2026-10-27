
package com.project.lscts.controller;

import com.project.lscts.dto.complaint.ComplaintRequestDTO;
import com.project.lscts.dto.complaint.ComplaintResponseDTO;
import com.project.lscts.dto.complaint.StatusHistoryResponseDTO;
import com.project.lscts.dto.complaint.StatusUpdateRequestDTO;
import com.project.lscts.service.ComplaintService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // Admin: GET /api/complaints
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintResponseDTO>> getAllComplaints() {

        return ResponseEntity.ok(
                complaintService.getAllComplaints()
        );
    }

    // User: GET /api/complaints/my
    @GetMapping("/my")
    public ResponseEntity<List<ComplaintResponseDTO>> getMyComplaints(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                complaintService.getMyComplaints(
                        authentication.getName()
                )
        );
    }

    // User: POST /api/complaints
    @PostMapping
    public ResponseEntity<ComplaintResponseDTO> createComplaint(
            @Valid @RequestBody ComplaintRequestDTO request,
            Authentication authentication
    ) {

        ComplaintResponseDTO response =
                complaintService.createComplaint(
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Admin: PATCH /api/complaints/{id}/status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintResponseDTO> updateComplaintStatus(
            @PathVariable("id") Long complaintId,
            @Valid @RequestBody StatusUpdateRequestDTO request,
            Authentication authentication
    ) {

        ComplaintResponseDTO updatedComplaint =
                complaintService.updateComplaintStatus(
                        complaintId,
                        request.getStatus(),
                        authentication.getName()
                );

        return ResponseEntity.ok(updatedComplaint);
    }

    // Complaint status history
    @GetMapping("/{id}/history")
    public ResponseEntity<List<StatusHistoryResponseDTO>> getHistory(
            @PathVariable("id") Long id,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                complaintService.getStatusHistory(
                        id,
                        authentication.getName()
                )
        );
    }
}
