package org.silkroadpartnership.theway_noti.chord.service;

import java.util.List;

import org.silkroadpartnership.theway_noti.chord.component.ChordParser;
import org.silkroadpartnership.theway_noti.chord.component.GuitarFingeringCalculator;
import org.silkroadpartnership.theway_noti.chord.entity.ChordParseResult;
import org.silkroadpartnership.theway_noti.chord.entity.ChordParseDetailResult;
import org.silkroadpartnership.theway_noti.chord.entity.GuitarFingering;
import org.silkroadpartnership.theway_noti.chord.entity.GuitarFingeringResult;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChordService {

    private final ChordParser chordParser;
    private final GuitarFingeringCalculator guitarFingeringCalculator;

    public ChordParseResult parseChordSymbol(String symbol) {
        try {
            log.debug("Parsing chord symbol: {}", symbol);
            ChordParseDetailResult detailResult = chordParser.parseChord(symbol);
            
            // 기존 클라이언트 호환성: bass 필드에 root+bass 합친 로직
            String compatibleBass = detailResult.getBass() != null ? 
                detailResult.getBass() : detailResult.getRoot();
            
            ChordParseResult result = ChordParseResult.builder()
                .originalSymbol(detailResult.getOriginalSymbol())
                .bass(compatibleBass)  // 기존과 동일한 로직
                .intervals(detailResult.getIntervals())
                .semitones(detailResult.getSemitones())
                .noteNames(detailResult.getNoteNames())
                .unparsedRemainder(detailResult.getUnparsedRemainder())
                .build();
                
            log.debug("Parse result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Error parsing chord symbol: {}", symbol, e);
            throw new IllegalArgumentException("Invalid chord symbol: " + symbol, e);
        }
    }

    public GuitarFingeringResult getGuitarFingerings(ChordParseResult chordResult) {
        try {
            log.debug("Calculating guitar fingerings for chord: {}", chordResult.getOriginalSymbol());
            
            // 기타 운지법 계산을 위해 detail result 다시 파싱 (root, bass 구분 필요)
            ChordParseDetailResult detailResult = chordParser.parseChord(chordResult.getOriginalSymbol());
            List<GuitarFingering> fingerings = guitarFingeringCalculator.calculateFingerings(detailResult);
            
            GuitarFingeringResult result = GuitarFingeringResult.builder()
                    .chord(chordResult.getOriginalSymbol())
                    .patterns(fingerings)
                    .build();
            
            if (fingerings.size() == 1) {
                log.debug("Generated fingering pattern");
            } else {
                log.debug("No fingering pattern generated");
            }
            return result;
        } catch (Exception e) {
            log.error("Error calculating guitar fingerings: {}", chordResult.getOriginalSymbol(), e);
            throw new IllegalArgumentException("Error calculating guitar fingerings", e);
        }
    }
}