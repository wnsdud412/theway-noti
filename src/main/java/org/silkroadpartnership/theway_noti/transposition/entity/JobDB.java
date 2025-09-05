package org.silkroadpartnership.theway_noti.transposition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDB {
    
    @Id
    @Column(length = 20)
    private String id;  // YouTube video_id
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;
    
    @Column(nullable = false)
    @Builder.Default
    private Float progress = 0.0f;
    
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String message = "";
    
    @Column(name = "video_id", length = 20, nullable = false)
    private String videoId;  // YouTube video_id (same as id)
    
    @Column(name = "audio_format", length = 10)
    @Builder.Default
    private String audioFormat = "mp3";
    
    @Column(name = "output_path", length = 500)
    private String outputPath;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = JobStatus.QUEUED;
        }
        if (progress == null) {
            progress = 0.0f;
        }
        if (message == null) {
            message = "";
        }
        if (audioFormat == null) {
            audioFormat = "mp3";
        }
    }
    
    public boolean isTerminalStatus() {
        return status == JobStatus.FINISHED || 
               status == JobStatus.ERROR || 
               status == JobStatus.CANCELED;
    }
    
    public boolean canBeCanceled() {
        return status == JobStatus.QUEUED || status == JobStatus.RUNNING;
    }
}