package org.silkroadpartnership.theway_noti.transposition.repository;

import org.silkroadpartnership.theway_noti.transposition.entity.JobDB;
import org.silkroadpartnership.theway_noti.transposition.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobDB, String> {
    
    /**
     * Find jobs by status
     */
    List<JobDB> findByStatus(JobStatus status);
    
    /**
     * Find jobs by video_id
     */
    JobDB findByVideoId(String videoId);
    
    /**
     * Find old completed jobs for cleanup
     */
    @Query("SELECT j FROM JobDB j WHERE j.status IN :terminalStatuses AND j.finishedAt < :cutoffTime")
    List<JobDB> findOldCompletedJobs(
        @Param("terminalStatuses") List<JobStatus> terminalStatuses, 
        @Param("cutoffTime") LocalDateTime cutoffTime
    );
    
    /**
     * Update job status and message
     */
    @Modifying
    @Query("UPDATE JobDB j SET j.status = :status, j.message = :message, " +
           "j.finishedAt = CASE WHEN :status IN ('FINISHED', 'ERROR', 'CANCELED') THEN CURRENT_TIMESTAMP ELSE j.finishedAt END " +
           "WHERE j.id = :jobId")
    int updateJobStatus(@Param("jobId") String jobId, 
                       @Param("status") JobStatus status, 
                       @Param("message") String message);
    
    /**
     * Update job progress
     */
    @Modifying
    @Query("UPDATE JobDB j SET j.progress = :progress, j.message = :message WHERE j.id = :jobId")
    int updateJobProgress(@Param("jobId") String jobId, 
                         @Param("progress") Float progress, 
                         @Param("message") String message);
    
    /**
     * Update job output path
     */
    @Modifying
    @Query("UPDATE JobDB j SET j.outputPath = :outputPath WHERE j.id = :jobId")
    int updateJobOutputPath(@Param("jobId") String jobId, 
                           @Param("outputPath") String outputPath);
    
    /**
     * Count jobs by status
     */
    long countByStatus(JobStatus status);
    
    /**
     * Find jobs created after specific time
     */
    List<JobDB> findByCreatedAtAfter(LocalDateTime time);
}