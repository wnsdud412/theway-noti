package org.silkroadpartnership.theway_noti.transposition.exception;

public class AudioDownloadException extends RuntimeException {
    
    public AudioDownloadException(String message) {
        super(message);
    }
    
    public AudioDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}