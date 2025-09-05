package org.silkroadpartnership.theway_noti.transposition.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AudioDownloadResponse {
    private boolean success;
    private String message;
    private AudioFile audioFile;
    private Double progress; // For in-progress responses
    private String status;   // "success", "failure", "in_progress"
    
    public static AudioDownloadResponse success(AudioFile audioFile) {
        return AudioDownloadResponse.builder()
                .success(true)
                .status("success")
                .message("Audio downloaded successfully")
                .audioFile(audioFile)
                .progress(1.0)
                .build();
    }
    
    public static AudioDownloadResponse failure(String message) {
        return AudioDownloadResponse.builder()
                .success(false)
                .status("failure")
                .message(message)
                .progress(0.0)
                .build();
    }
    
    public static AudioDownloadResponse inProgress(String message, Double progress) {
        return AudioDownloadResponse.builder()
                .success(false) // Not yet successful
                .status("in_progress")
                .message(message)
                .progress(progress != null ? progress : 0.0)
                .build();
    }
    
    public boolean isInProgress() {
        return "in_progress".equals(status);
    }
    
    public boolean isComplete() {
        return success && audioFile != null;
    }
}