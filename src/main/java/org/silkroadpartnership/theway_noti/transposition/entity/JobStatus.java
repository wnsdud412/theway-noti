package org.silkroadpartnership.theway_noti.transposition.entity;

public enum JobStatus {
    QUEUED("queued"),
    RUNNING("running"), 
    FINISHED("finished"),
    ERROR("error"),
    CANCELED("canceled");
    
    private final String value;
    
    JobStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}