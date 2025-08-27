package org.silkroadpartnership.theway_noti.chord.entity;

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
public class GuitarFingering {
    
    private int[] frets;
    private BarreInfo barre;
    private String difficulty;
    private int position;
    
}