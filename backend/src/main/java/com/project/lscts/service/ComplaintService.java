package com.project.lscts.service;

import com.project.lscts.dto.complaint.ComplaintRequestDTO;
import com.project.lscts.dto.complaint.ComplaintResponseDTO;
import com.project.lscts.dto.complaint.StatusHistoryResponseDTO;
import com.project.lscts.entity.ComplaintEntity;
import com.project.lscts.entity.ComplaintStatus;
import com.project.lscts.entity.StatusHistoryEntity;
import com.project.lscts.entity.UserEntity;
import com.project.lscts.repository.ComplaintRepository;
import com.project.lscts.repository.StatusHistoryRepository;
import com.project.lscts.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            StatusHistoryRepository statusHistoryRepository,
            UserRepository userRepository
    ) {
        this.complaintRepository = complaintRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userRepository = userRepository;
    }

    // Admin: view all complaints
    @Transactional(readOnly = true)
    public List<ComplaintResponseDTO> getAllComplaints() {

        return complaintRepository.findAll()
                .stream()
                .map(this::toComplaintResponse)
                .toList();
    }

    // User: view only their own complaints
    @Transactional(readOnly = true)
    public List<ComplaintResponseDTO> getMyComplaints(String email) {

        UserEntity user = findUserByEmail(email);

        return complaintRepository
                .findMyComplaintsWithUser(user.getUserId())
                .stream()
                .map(this::toComplaintResponse)
                .toList();
    }

    // User: create a complaint
    @Transactional
    public ComplaintResponseDTO createComplaint(
            ComplaintRequestDTO request,
            String email
    ) {

        UserEntity user = findUserByEmail(email);

        ComplaintEntity complaint = new ComplaintEntity();

        complaint.setUser(user);
        complaint.setTitle(request.getTitle().trim());
        complaint.setDescription(request.getDescription().trim());
        complaint.setStatus(ComplaintStatus.PENDING);

        ComplaintEntity saved = complaintRepository.save(complaint);

        // Record the initial status
        StatusHistoryEntity history = new StatusHistoryEntity();

        history.setComplaint(saved);
        history.setUpdatedBy(user);
        history.setCurrentStatus(ComplaintStatus.PENDING);
        history.setRemarks("Complaint created");

        statusHistoryRepository.save(history);

        return toComplaintResponse(saved);
    }

    // Admin: update complaint status
    @Transactional
    public ComplaintResponseDTO updateComplaintStatus(
            Long complaintId,
            ComplaintStatus newStatus,
            String adminEmail
    ) {
        if (newStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status is required"
            );
        }

        ComplaintEntity complaint = complaintRepository
                .findByIdWithUser(complaintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Complaint not found"
                ));

        UserEntity admin = findUserByEmail(adminEmail);

        if (complaint.getStatus() == newStatus) {
            return toComplaintResponse(complaint);
        }

        complaint.setStatus(newStatus);

        ComplaintEntity savedComplaint =
                complaintRepository.save(complaint);

        // Save status history
        StatusHistoryEntity history = new StatusHistoryEntity();
        history.setComplaint(savedComplaint);
        history.setUpdatedBy(admin);
        history.setCurrentStatus(newStatus);
        history.setRemarks("Status updated to " + newStatus.name());

        statusHistoryRepository.save(history);

        return toComplaintResponse(savedComplaint);
    }

    // View status history for a complaint
    @Transactional(readOnly = true)
    public List<StatusHistoryResponseDTO> getStatusHistory(
            Long complaintId,
            String email
    ) {

        ComplaintEntity complaint = complaintRepository
                .findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Complaint not found"
                ));

        UserEntity requester = findUserByEmail(email);

        // A normal user can view only their own complaint history.
        if (requester.getRole().name().equals("USER")
                && !complaint.getUser().getUserId()
                .equals(requester.getUserId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot view another user's complaint history"
            );
        }

        return statusHistoryRepository
                .findByComplaintOrderByUpdatedAtDesc(complaint)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    // Find user by email
    private UserEntity findUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    // Convert complaint entity to response DTO
    private ComplaintResponseDTO toComplaintResponse(
            ComplaintEntity complaint
    ) {

        UserEntity user = complaint.getUser();

        return new ComplaintResponseDTO(
                complaint.getComplaintId(),
                user.getUserId(),
                user.getName(),
                complaint.getTitle(),
                complaint.getDescription(),
                complaint.getStatus(),
                complaint.getComplaintCreatedAt(),
                complaint.getUpdatedAt()
        );
    }

    // Convert status history entity to response DTO
    private StatusHistoryResponseDTO toHistoryResponse(
            StatusHistoryEntity history
    ) {

        UserEntity updatedBy = history.getUpdatedBy();

        return new StatusHistoryResponseDTO(
                history.getHistoryId(),
                history.getComplaint().getComplaintId(),
                history.getCurrentStatus(),
                history.getRemarks(),
                updatedBy.getUserId(),
                updatedBy.getName(),
                history.getUpdatedAt()
        );
    }
}
