package org.silkroadpartnership.theway_noti.transposition.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.silkroadpartnership.theway_noti.transposition.entity.PlaylistResponse;
import org.silkroadpartnership.theway_noti.transposition.entity.PlaylistVideo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongDlClient {
    
    private static final String SONG_DL_BASE_URL = "http://song-dl:8082";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Create a new download job
     */
    public SongDlJobResponse createJob(String videoId, String audioFormat) {
        try {
            String url = SONG_DL_BASE_URL + "/yt/jobs";
            
            // Request body
            SongDlJobRequest request = new SongDlJobRequest(videoId, audioFormat);
            
            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<SongDlJobRequest> entity = new HttpEntity<>(request, headers);
            
            log.info("Creating song-dl job for videoId: {}", videoId);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                SongDlJobResponse jobResponse = SongDlJobResponse.builder()
                    .id(jsonNode.get("id").asText())
                    .status(jsonNode.get("status").asText())
                    .progress(jsonNode.get("progress").asDouble())
                    .message(jsonNode.get("message").asText())
                    .outputPath(jsonNode.has("output_path") && !jsonNode.get("output_path").isNull() 
                        ? jsonNode.get("output_path").asText() : null)
                    .build();
                    
                log.info("Successfully created job: {}", jobResponse.getId());
                return jobResponse;
            } else {
                log.error("Failed to create job. Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
                return SongDlJobResponse.error("Failed to create job: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            log.error("REST client error while creating job for videoId: {}", videoId, e);
            return SongDlJobResponse.error("Communication error with song-dl service: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while creating job for videoId: {}", videoId, e);
            return SongDlJobResponse.error("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Get job status
     */
    public SongDlJobResponse getJobStatus(String jobId) {
        try {
            String url = SONG_DL_BASE_URL + "/yt/jobs/" + jobId;
            
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return SongDlJobResponse.builder()
                    .id(jsonNode.get("id").asText())
                    .status(jsonNode.get("status").asText())
                    .progress(jsonNode.get("progress").asDouble())
                    .message(jsonNode.get("message").asText())
                    .outputPath(jsonNode.has("output_path") && !jsonNode.get("output_path").isNull() 
                        ? jsonNode.get("output_path").asText() : null)
                    .build();
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                return SongDlJobResponse.error("Job not found");
            } else {
                log.error("Failed to get job status. Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
                return SongDlJobResponse.error("Failed to get job status: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            log.error("REST client error while getting job status for: {}", jobId, e);
            return SongDlJobResponse.error("Communication error with song-dl service: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while getting job status for: {}", jobId, e);
            return SongDlJobResponse.error("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Cancel a job
     */
    public SongDlJobResponse cancelJob(String jobId) {
        try {
            String url = SONG_DL_BASE_URL + "/yt/jobs/" + jobId + "/cancel";
            
            log.info("Canceling job: {}", jobId);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return SongDlJobResponse.builder()
                    .id(jsonNode.get("id").asText())
                    .status(jsonNode.get("status").asText())
                    .progress(0.0)
                    .message("Job canceled")
                    .outputPath(null)
                    .build();
            } else {
                log.error("Failed to cancel job. Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
                return SongDlJobResponse.error("Failed to cancel job: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            log.error("REST client error while canceling job: {}", jobId, e);
            return SongDlJobResponse.error("Communication error with song-dl service: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while canceling job: {}", jobId, e);
            return SongDlJobResponse.error("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Get playlist information
     */
    public PlaylistResponse getPlaylistInfo(String playlistId) {
        try {
            String url = SONG_DL_BASE_URL + "/yt/playlist/" + playlistId;
            
            log.info("Getting playlist info for: {}", playlistId);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                
                // Parse playlist basic info
                String title = jsonNode.get("title").asText();
                
                // Parse videos list
                List<PlaylistVideo> videos = new ArrayList<>();
                JsonNode videosArray = jsonNode.get("videos");
                if (videosArray.isArray()) {
                    for (JsonNode videoNode : videosArray) {
                        PlaylistVideo video = PlaylistVideo.builder()
                                .videoId(videoNode.get("video_id").asText())
                                .title(videoNode.get("title").asText())
                                .duration(videoNode.has("duration") && !videoNode.get("duration").isNull() 
                                    ? videoNode.get("duration").asInt() : null)
                                .build();
                        videos.add(video);
                    }
                }
                
                log.info("Successfully retrieved playlist info: {} videos", videos.size());
                return PlaylistResponse.success(playlistId, title, videos);
                
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                return PlaylistResponse.failure("Playlist not found");
            } else {
                log.error("Failed to get playlist info. Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
                return PlaylistResponse.failure("Failed to get playlist info: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            log.error("REST client error while getting playlist info for: {}", playlistId, e);
            return PlaylistResponse.failure("Communication error with song-dl service: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while getting playlist info for: {}", playlistId, e);
            return PlaylistResponse.failure("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Health check
     */
    public boolean isHealthy() {
        try {
            String url = SONG_DL_BASE_URL + "/healthz";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("song-dl health check failed", e);
            return false;
        }
    }
    
    // DTOs
    public static class SongDlJobRequest {
        private final String video_id;
        private final String audio_format;
        
        public SongDlJobRequest(String videoId, String audioFormat) {
            this.video_id = videoId;
            this.audio_format = audioFormat;
        }
        
        public String getVideo_id() { return video_id; }
        public String getAudio_format() { return audio_format; }
    }
    
    public static class SongDlJobResponse {
        private final String id;
        private final String status;
        private final Double progress;
        private final String message;
        private final String outputPath;
        private final String error;
        
        private SongDlJobResponse(String id, String status, Double progress, 
                                 String message, String outputPath, String error) {
            this.id = id;
            this.status = status;
            this.progress = progress;
            this.message = message;
            this.outputPath = outputPath;
            this.error = error;
        }
        
        public static SongDlJobResponseBuilder builder() {
            return new SongDlJobResponseBuilder();
        }
        
        public static SongDlJobResponse error(String error) {
            return new SongDlJobResponse(null, null, null, null, null, error);
        }
        
        // Getters
        public String getId() { return id; }
        public String getStatus() { return status; }
        public Double getProgress() { return progress; }
        public String getMessage() { return message; }
        public String getOutputPath() { return outputPath; }
        public String getError() { return error; }
        
        public boolean isSuccess() { return error == null; }
        public boolean isError() { return error != null; }
        
        // Builder class
        public static class SongDlJobResponseBuilder {
            private String id;
            private String status;
            private Double progress;
            private String message;
            private String outputPath;
            
            public SongDlJobResponseBuilder id(String id) { this.id = id; return this; }
            public SongDlJobResponseBuilder status(String status) { this.status = status; return this; }
            public SongDlJobResponseBuilder progress(Double progress) { this.progress = progress; return this; }
            public SongDlJobResponseBuilder message(String message) { this.message = message; return this; }
            public SongDlJobResponseBuilder outputPath(String outputPath) { this.outputPath = outputPath; return this; }
            
            public SongDlJobResponse build() {
                return new SongDlJobResponse(id, status, progress, message, outputPath, null);
            }
        }
    }
}