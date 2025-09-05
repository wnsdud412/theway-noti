package org.silkroadpartnership.theway_noti.transposition.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.silkroadpartnership.theway_noti.transposition.entity.AudioDownloadResponse;
import org.silkroadpartnership.theway_noti.transposition.entity.AudioFile;
import org.silkroadpartnership.theway_noti.transposition.entity.JobDB;
import org.silkroadpartnership.theway_noti.transposition.entity.JobStatus;
import org.silkroadpartnership.theway_noti.transposition.exception.AudioDownloadException;
import org.silkroadpartnership.theway_noti.transposition.repository.JobRepository;
import org.silkroadpartnership.theway_noti.transposition.util.YouTubeUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioDownloadService {
    
    private final SongDlClient songDlClient;
    private final JobRepository jobRepository;
    
    /**
     * Handle audio request following spec_4.md workflow
     */
    @Transactional
    public AudioDownloadResponse handleAudioRequest(String input) {
        try {
            // Extract video ID from URL or validate if it's already a video ID
            String videoId = YouTubeUtils.extractVideoId(input);
            if (videoId == null) {
                return AudioDownloadResponse.failure("유효하지 않은 YouTube URL 또는 비디오 ID입니다.");
            }
            
            log.info("Processing audio request for videoId: {}", videoId);
            
            // 1. DB에서 기존 작업 확인
            JobDB existingJob = jobRepository.findByVideoId(videoId);
            
            if (existingJob == null) {
                // 2-1. 작업이 없으면 song-dl에 다운로드 요청
                return requestNewDownload(videoId);
                
            } else if (existingJob.getStatus() == JobStatus.FINISHED && existingJob.getOutputPath() != null) {
                // 2-2. 완료되었으면 파일 직접 전송
                return handleCompletedJob(existingJob);
                
            } else if (existingJob.getStatus() == JobStatus.QUEUED || existingJob.getStatus() == JobStatus.RUNNING) {
                // 2-3. 진행중이면 진행률 표시
                return AudioDownloadResponse.inProgress(
                    String.format("🎵 다운로드 중... (%.1f%%)", existingJob.getProgress() * 100),
                    existingJob.getProgress().doubleValue()
                );
                
            } else { // ERROR, CANCELED
                // 2-4. 실패했으면 재시도
                return retryFailedJob(videoId, existingJob);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error in handleAudioRequest for input: {}", YouTubeUtils.sanitizeForLog(input), e);
            return AudioDownloadResponse.failure("음성 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    private AudioDownloadResponse requestNewDownload(String videoId) {
        log.info("Requesting new download for videoId: {}", videoId);
        
        SongDlClient.SongDlJobResponse response = songDlClient.createJob(videoId, "mp3");
        
        if (response.isError()) {
            return AudioDownloadResponse.failure("다운로드 요청 실패: " + response.getError());
        }
        
        return AudioDownloadResponse.inProgress("🎵 다운로드를 시작했습니다. 잠시 후 다시 시도해주세요.", 0.0);
    }
    
    private AudioDownloadResponse handleCompletedJob(JobDB job) {
        log.info("Handling completed job for videoId: {}", job.getVideoId());
        
        // 파일 존재 여부 확인
        File outputFile = new File(job.getOutputPath());
        if (!outputFile.exists()) {
            log.warn("Output file not found for job {}: {}", job.getId(), job.getOutputPath());
            
            // 파일이 없으면 재다운로드 요청
            job.setStatus(JobStatus.QUEUED);
            job.setMessage("파일을 다시 준비하고 있습니다.");
            job.setProgress(0.0f);
            job.setOutputPath(null);
            jobRepository.save(job);
            
            SongDlClient.SongDlJobResponse response = songDlClient.createJob(job.getVideoId(), job.getAudioFormat());
            if (response.isError()) {
                return AudioDownloadResponse.failure("파일 재생성 요청 실패: " + response.getError());
            }
            
            return AudioDownloadResponse.inProgress("🎵 파일을 다시 준비하고 있습니다.", 0.0);
        }
        
        // 파일이 존재하면 AudioFile 객체 생성하여 반환
        AudioFile audioFile = createAudioFileFromJob(job, outputFile);
        return AudioDownloadResponse.success(audioFile);
    }
    
    private AudioDownloadResponse retryFailedJob(String videoId, JobDB failedJob) {
        log.info("Retrying failed job for videoId: {}", videoId);
        
        // 재시도 요청
        SongDlClient.SongDlJobResponse response = songDlClient.createJob(videoId, failedJob.getAudioFormat());
        
        if (response.isError()) {
            return AudioDownloadResponse.failure("재시도 요청 실패: " + response.getError());
        }
        
        return AudioDownloadResponse.inProgress("🎵 이전 다운로드에 문제가 있어 다시 시도합니다.", 0.0);
    }
    
    private AudioFile createAudioFileFromJob(JobDB job, File file) {
        // Use message field as title when download is successful
        String title = (job.getMessage() != null && !job.getMessage().isEmpty()) 
            ? job.getMessage() 
            : "Downloaded Audio";
            
        return AudioFile.builder()
                .videoId(job.getVideoId())
                .title(title)
                .fileName(file.getName())
                .filePath(file.getAbsolutePath())
                .extension(job.getAudioFormat())
                .fileSize(file.length())
                .format("audio/" + job.getAudioFormat())
                .build();
    }
    
    /**
     * Get audio file resource for streaming/download
     */
    public Resource getAudioFileResource(String videoId) throws AudioDownloadException {
        try {
            JobDB job = jobRepository.findByVideoId(videoId);
            
            if (job == null || job.getStatus() != JobStatus.FINISHED || job.getOutputPath() == null) {
                throw new AudioDownloadException("Audio file not ready for videoId: " + videoId);
            }
            
            Path filePath = Paths.get(job.getOutputPath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                throw new AudioDownloadException("Audio file not accessible: " + job.getOutputPath());
            }
            
            return resource;
            
        } catch (Exception e) {
            log.error("Failed to get audio file resource for videoId: {}", videoId, e);
            throw new AudioDownloadException("Failed to access audio file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get job status for monitoring
     */
    public JobStatus getJobStatus(String videoId) {
        JobDB job = jobRepository.findByVideoId(videoId);
        return job != null ? job.getStatus() : null;
    }
    
    /**
     * Cancel job
     */
    @Transactional
    public boolean cancelJob(String videoId) {
        try {
            JobDB job = jobRepository.findByVideoId(videoId);
            
            if (job == null) {
                return false;
            }
            
            if (!job.canBeCanceled()) {
                log.warn("Job cannot be canceled. Current status: {}", job.getStatus());
                return false;
            }
            
            SongDlClient.SongDlJobResponse response = songDlClient.cancelJob(job.getId());
            
            return response.isSuccess();
            
        } catch (Exception e) {
            log.error("Failed to cancel job for videoId: {}", videoId, e);
            return false;
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public AudioDownloadResponse downloadAudio(String videoId) {
        return handleAudioRequest(videoId);
    }
}