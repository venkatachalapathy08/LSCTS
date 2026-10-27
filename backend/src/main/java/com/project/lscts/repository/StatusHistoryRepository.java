
package com.project.lscts.repository;

import com.project.lscts.entity.ComplaintEntity;
import com.project.lscts.entity.StatusHistoryEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StatusHistoryRepository
        extends JpaRepository<StatusHistoryEntity, Long> {

    // Get status history of a complaint, newest first
    List<StatusHistoryEntity>
    findByComplaintOrderByUpdatedAtDesc(ComplaintEntity complaint);

    // Delete history belonging to a user's complaints
    @Modifying
    @Query("""
        DELETE FROM StatusHistoryEntity h
        WHERE h.complaint.complaintId IN (
            SELECT c.complaintId
            FROM ComplaintEntity c
            WHERE c.user.userId = :userId
        )
    """)
    void deleteAllByComplaintOwnerId(@Param("userId") Long userId);
}
