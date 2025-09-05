package org.silkroadpartnership.theway_noti.transposition.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistResponse {
    private String playlistId;
    private String title;
    private int videoCount;
    private List<PlaylistVideo> videos;
    
    // Success/error handling
    private boolean success;
    private String message;
    
    public static PlaylistResponse success(String playlistId, String title, List<PlaylistVideo> videos) {
        return PlaylistResponse.builder()
                .success(true)
                .playlistId(playlistId)
                .title(title)
                .videoCount(videos.size())
                .videos(videos)
                .build();
    }
    
    public static PlaylistResponse failure(String message) {
        return PlaylistResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}