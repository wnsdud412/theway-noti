package org.silkroadpartnership.theway_noti.transposition.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistVideo {
    private String videoId;
    private String title;
    private Integer duration; // Duration in seconds
}