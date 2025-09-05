package org.silkroadpartnership.theway_noti.transposition.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class YouTubeUtils {
    
    // YouTube URL patterns
    private static final Pattern[] YOUTUBE_PATTERNS = {
        // Standard YouTube URLs
        Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})"),
        Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/watch\\?.*v=([a-zA-Z0-9_-]{11})"),
        
        // Short YouTube URLs
        Pattern.compile("(?:https?://)?(?:www\\.)?youtu\\.be/([a-zA-Z0-9_-]{11})"),
        
        // Mobile YouTube URLs
        Pattern.compile("(?:https?://)?(?:m\\.)?youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})"),
        Pattern.compile("(?:https?://)?(?:m\\.)?youtube\\.com/watch\\?.*v=([a-zA-Z0-9_-]{11})"),
        
        // Embedded YouTube URLs
        Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
        
        // YouTube Music URLs
        Pattern.compile("(?:https?://)?music\\.youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})"),
        Pattern.compile("(?:https?://)?music\\.youtube\\.com/watch\\?.*v=([a-zA-Z0-9_-]{11})")
    };
    
    // Video ID validation pattern
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{11}$");
    
    /**
     * Extract video ID from various YouTube URL formats
     */
    public static String extractVideoId(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        String trimmedInput = input.trim();
        
        // First check if input is already a valid video ID
        if (isValidVideoId(trimmedInput)) {
            return trimmedInput;
        }
        
        // Try to extract from URL patterns
        for (Pattern pattern : YOUTUBE_PATTERNS) {
            Matcher matcher = pattern.matcher(trimmedInput);
            if (matcher.find()) {
                String videoId = matcher.group(1);
                return videoId;
            }
        }
        
        return null;
    }
    
    /**
     * Validate if string is a valid YouTube video ID
     */
    public static boolean isValidVideoId(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return false;
        }
        
        return VIDEO_ID_PATTERN.matcher(videoId.trim()).matches();
    }
    
    /**
     * Check if input looks like a YouTube URL
     */
    public static boolean isYouTubeUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String lowerInput = input.trim().toLowerCase();
        return lowerInput.contains("youtube.com") || 
               lowerInput.contains("youtu.be") || 
               lowerInput.contains("music.youtube.com");
    }
    
    /**
     * Construct standard YouTube URL from video ID
     */
    public static String constructYouTubeUrl(String videoId) {
        if (!isValidVideoId(videoId)) {
            throw new IllegalArgumentException("Invalid video ID: " + videoId);
        }
        
        return "https://www.youtube.com/watch?v=" + videoId;
    }
    
    /**
     * Construct YouTube thumbnail URL from video ID
     */
    public static String constructThumbnailUrl(String videoId, ThumbnailQuality quality) {
        if (!isValidVideoId(videoId)) {
            throw new IllegalArgumentException("Invalid video ID: " + videoId);
        }
        
        return String.format("https://img.youtube.com/vi/%s/%s.jpg", videoId, quality.getCode());
    }
    
    /**
     * Get video info URL for debugging
     */
    public static String getVideoInfoUrl(String videoId) {
        if (!isValidVideoId(videoId)) {
            throw new IllegalArgumentException("Invalid video ID: " + videoId);
        }
        
        return "https://www.googleapis.com/youtube/v3/videos?id=" + videoId + "&part=snippet";
    }
    
    // Thumbnail quality enum
    public enum ThumbnailQuality {
        DEFAULT("default"),          // 120x90
        MEDIUM("mqdefault"),         // 320x180  
        HIGH("hqdefault"),          // 480x360
        STANDARD("sddefault"),       // 640x480
        MAX_RES("maxresdefault");    // 1280x720
        
        private final String code;
        
        ThumbnailQuality(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    /**
     * Sanitize input for logging (hide full URLs in logs for privacy)
     */
    public static String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }
        
        if (input.length() > 50) {
            return input.substring(0, 47) + "...";
        }
        
        return input;
    }
    
    /**
     * Extract playlist ID if present in URL
     */
    public static String extractPlaylistId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        Pattern playlistPattern = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)");
        Matcher matcher = playlistPattern.matcher(url);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Check if URL contains playlist
     */
    public static boolean hasPlaylist(String url) {
        return extractPlaylistId(url) != null;
    }
}