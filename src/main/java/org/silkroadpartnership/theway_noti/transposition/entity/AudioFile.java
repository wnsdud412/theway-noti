package org.silkroadpartnership.theway_noti.transposition.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AudioFile {
    private String videoId;
    private String title;
    private String fileName;
    private String filePath;
    private String extension;
    private long fileSize;
    private String format;
}