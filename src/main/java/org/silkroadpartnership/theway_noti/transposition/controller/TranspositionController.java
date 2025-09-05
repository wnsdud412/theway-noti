package org.silkroadpartnership.theway_noti.transposition.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.silkroadpartnership.theway_noti.transposition.entity.AudioDownloadResponse;
import org.silkroadpartnership.theway_noti.transposition.entity.PlaylistResponse;
import org.silkroadpartnership.theway_noti.transposition.exception.AudioDownloadException;
import org.silkroadpartnership.theway_noti.transposition.service.AudioDownloadService;
import org.silkroadpartnership.theway_noti.transposition.service.SongDlClient;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/transposition")
@RequiredArgsConstructor
public class TranspositionController {
    
    private final AudioDownloadService audioDownloadService;
    private final SongDlClient songDlClient;
    
    @GetMapping("/download")
    public ResponseEntity<AudioDownloadResponse> downloadAudio(@RequestParam String videoId) {
        try {
            log.info("Received audio download request for videoId: {}", videoId);
            
            // 기본 파라미터 검증
            if (videoId == null || videoId.trim().isEmpty()) {
                AudioDownloadResponse errorResponse = AudioDownloadResponse.failure("VideoId parameter is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            AudioDownloadResponse response = audioDownloadService.downloadAudio(videoId.trim());
            
            if (response.isSuccess()) {
                log.info("Audio download request completed successfully for videoId: {}", videoId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Audio download request failed for videoId: {}, error: {}", videoId, response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Unexpected error in downloadAudio controller for videoId: {}", videoId, e);
            AudioDownloadResponse errorResponse = AudioDownloadResponse.failure("Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/stream/{videoId}")
    public ResponseEntity<Resource> streamAudio(@PathVariable String videoId) {
        try {
            log.info("Received audio streaming request for videoId: {}", videoId);
            
            // 파라미터 검증
            if (videoId == null || videoId.trim().isEmpty()) {
                log.warn("VideoId parameter is missing or empty");
                return ResponseEntity.badRequest().build();
            }
            
            // 오디오 파일 리소스 가져오기
            Resource resource = audioDownloadService.getAudioFileResource(videoId.trim());
            
            // 파일명 및 MIME 타입 결정
            String filename = resource.getFilename() != null ? resource.getFilename() : videoId + ".mp3";
            MediaType mediaType = getMediaTypeForFile(filename);
            
            log.info("Streaming audio file: {} for videoId: {}", filename, videoId);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .header("Accept-Ranges", "bytes")
                .header("Cache-Control", "public, max-age=3600")
                .body(resource);
                
        } catch (AudioDownloadException e) {
            log.warn("Audio file not available for videoId: {}, reason: {}", videoId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error in streamAudio controller for videoId: {}", videoId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/playlist/{playlistId}")
    public ResponseEntity<PlaylistResponse> getPlaylistInfo(@PathVariable String playlistId) {
        try {
            log.info("Received playlist info request for playlistId: {}", playlistId);
            
            // 기본 파라미터 검증
            if (playlistId == null || playlistId.trim().isEmpty()) {
                PlaylistResponse errorResponse = PlaylistResponse.failure("PlaylistId parameter is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Playlist ID 형식 검증 (YouTube playlist ID 형식)
            String trimmedPlaylistId = playlistId.trim();
            if (!isValidPlaylistId(trimmedPlaylistId)) {
                PlaylistResponse errorResponse = PlaylistResponse.failure("Invalid YouTube playlist ID format");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // SongDl client를 통해 playlist 정보 조회
            PlaylistResponse response = songDlClient.getPlaylistInfo(trimmedPlaylistId);
            
            if (response.isSuccess()) {
                log.info("Playlist info request completed successfully for playlistId: {}, {} videos", 
                    playlistId, response.getVideoCount());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Playlist info request failed for playlistId: {}, error: {}", 
                    playlistId, response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error in getPlaylistInfo controller for playlistId: {}", playlistId, e);
            PlaylistResponse errorResponse = PlaylistResponse.failure("Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * YouTube playlist ID 형식 검증
     */
    private boolean isValidPlaylistId(String playlistId) {
        if (playlistId == null || playlistId.isEmpty()) {
            return false;
        }
        
        // YouTube playlist ID는 일반적으로 PL로 시작하고 34자리이거나, 다른 형식의 34자리 ID
        return playlistId.matches("^PL[a-zA-Z0-9_-]{32}$|^[a-zA-Z0-9_-]{34}$");
    }
    
    /**
     * 파일 확장자에 따른 MIME 타입 결정
     */
    private MediaType getMediaTypeForFile(String filename) {
        if (filename == null) {
            return MediaType.valueOf("audio/mpeg");
        }
        
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".mp3")) {
            return MediaType.valueOf("audio/mpeg");
        } else if (lowerFilename.endsWith(".m4a") || lowerFilename.endsWith(".aac")) {
            return MediaType.valueOf("audio/mp4");
        } else if (lowerFilename.endsWith(".wav")) {
            return MediaType.valueOf("audio/wav");
        } else if (lowerFilename.endsWith(".ogg")) {
            return MediaType.valueOf("audio/ogg");
        } else if (lowerFilename.endsWith(".opus")) {
            return MediaType.valueOf("audio/opus");
        }
        
        // 기본값
        return MediaType.valueOf("audio/mpeg");
    }
}