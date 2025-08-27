package org.silkroadpartnership.theway_noti.chord.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuitarFingeringResult {
    
    private String chord;
    private List<GuitarFingering> patterns;
    
}