package org.silkroadpartnership.theway_noti.transposition.component;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.Config;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import com.github.kiulian.downloader.model.Extension;
import lombok.extern.slf4j.Slf4j;
import org.silkroadpartnership.theway_noti.transposition.entity.AudioFile;
import org.silkroadpartnership.theway_noti.transposition.exception.AudioDownloadException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Slf4j
@Component
public class YouTubeAudioDownloader {
    
    private static final String DOWNLOAD_DIR = "downloads";
    private final Config downloaderConfig;
    
    public YouTubeAudioDownloader() {
        this.downloaderConfig = createDownloaderConfig();
    }
    
    private Config createDownloaderConfig() {
        return new Config.Builder()
                .maxRetries(3) // YouTube 봇 차단 대응을 위한 재시도
                // 더욱 최신 브라우저로 위장 (2024년 최신 버전)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("Sec-CH-UA", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                .header("Sec-CH-UA-Mobile", "?0")
                .header("Sec-CH-UA-Platform", "\"Windows\"")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Upgrade-Insecure-Requests", "1")
                // X-Forwarded-For 헤더 추가 (IP 다양화)
                .header("X-Forwarded-For", "203.248.252." + (1 + (int)(Math.random() * 254)))
                .build();
    }
    
    public AudioFile downloadAudio(String videoId) {
        try {
            log.info("Starting video info retrieval for videoId: {} with custom config", videoId);
            
            // 봇 차단 우회를 위한 랜덤 딜레이 (1-3초)
            long delay = 1000 + (long)(Math.random() * 2000);
            Thread.sleep(delay);
            
            YoutubeDownloader downloader = new YoutubeDownloader(downloaderConfig);
            RequestVideoInfo request = new RequestVideoInfo(videoId);
            
            var videoInfoResponse = downloader.getVideoInfo(request);
            
            VideoInfo videoInfo = videoInfoResponse.data();
            if (videoInfo == null) {
                String errorMessage = videoInfoResponse.error() != null ? videoInfoResponse.error().toString() : "";
                log.error("Failed to get video info. Status: {}, Error: {}", 
                    videoInfoResponse.status(), errorMessage);
                
                // Handle specific YouTube error cases
                if (errorMessage.contains("LOGIN_REQUIRED")) {
                    throw new AudioDownloadException("YouTube requires authentication to access this video. " +
                        "The video may be age-restricted, private, or YouTube is implementing bot protection. " +
                        "Please try again later or use a different video.");
                } else if (errorMessage.contains("UNPLAYABLE")) {
                    throw new AudioDownloadException("This video is not playable. It may be private, deleted, or restricted in your region.");
                } else if (errorMessage.contains("streamingData not found")) {
                    throw new AudioDownloadException("Unable to access video streaming data. " +
                        "This may be due to YouTube's bot protection or the video being restricted.");
                } else {
                    throw new AudioDownloadException("Failed to retrieve video information for videoId: " + videoId + 
                        ". This may be due to YouTube restrictions or the video being unavailable.");
                }
            }
            
            log.info("Successfully retrieved video info. Title: {}", videoInfo.details().title());
            
            // 최적 오디오 포맷 선택
            AudioFormat audioFormat = selectBestAudioFormat(videoInfo);
            
            // 다운로드 디렉토리 생성
            File outputDir = new File(DOWNLOAD_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // 파일 다운로드
            String fileName = sanitizeFileName(videoInfo.details().title()) + "_audio";
            RequestVideoFileDownload fileRequest = new RequestVideoFileDownload(audioFormat)
                    .saveTo(outputDir)
                    .renameTo(fileName)
                    .overwriteIfExists(true);
            
            File downloadedFile = downloader.downloadVideoFile(fileRequest).data();
            
            log.info("Successfully downloaded audio: {}", downloadedFile.getAbsolutePath());
            
            return AudioFile.builder()
                    .videoId(videoId)
                    .title(videoInfo.details().title())
                    .fileName(downloadedFile.getName())
                    .filePath(downloadedFile.getAbsolutePath())
                    .extension(audioFormat.extension().value())
                    .fileSize(downloadedFile.length())
                    .format(audioFormat.mimeType())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to download audio for videoId: {}", videoId, e);
            throw new AudioDownloadException("Failed to download audio: " + e.getMessage(), e);
        }
    }
    
    private AudioFormat selectBestAudioFormat(VideoInfo videoInfo) {
        List<AudioFormat> audioFormats = videoInfo.audioFormats();
        
        if (audioFormats.isEmpty()) {
            throw new AudioDownloadException("No audio formats available for this video");
        }
        
        // M4A(AAC) 선호, 없으면 기본 최적 포맷 사용
        return audioFormats.stream()
                .filter(format -> format.extension() == Extension.M4A)
                .findFirst()
                .orElse(videoInfo.bestAudioFormat());
    }
    
    private String sanitizeFileName(String fileName) {
        // 파일명에서 특수문자 제거
        return fileName.replaceAll("[^a-zA-Z0-9가-힣\\s\\-_]", "")
                      .replaceAll("\\s+", "_")
                      .trim();
    }
}