package com.project.lscts.repository;

import com.project.lscts.entity.ComplaintEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ComplaintRepository
        extends JpaRepository<ComplaintEntity, Long> {



    @Modifying
    @Query("DELETE FROM ComplaintEntity c WHERE c.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT c
        FROM ComplaintEntity c
        JOIN FETCH c.user
        WHERE c.user.userId = :userId
        ORDER BY c.complaintCreatedAt DESC
    """)
    List<ComplaintEntity> findMyComplaintsWithUser(
            @Param("userId") Long userId
    );

    @Query("""
        SELECT c
        FROM ComplaintEntity c
        JOIN FETCH c.user
        WHERE c.complaintId = :complaintId
    """)
    Optional<ComplaintEntity> findByIdWithUser(
            @Param("complaintId") Long complaintId
    );
}