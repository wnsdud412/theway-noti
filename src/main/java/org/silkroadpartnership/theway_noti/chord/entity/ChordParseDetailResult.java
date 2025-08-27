package org.silkroadpartnership.theway_noti.chord.entity;

import java.util.List;
import java.util.Set;

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
public class ChordParseDetailResult {
    
    private String originalSymbol;
    private String root;        // 실제 루트음
    private String bass;        // 베이스음 (분수코드의 경우만, null 가능)
    private Set<String> intervals;
    private List<Integer> semitones;
    private List<String> noteNames;
    private String unparsedRemainder; // 파싱되지 않은 나머지 부분
    
}