package org.silkroadpartnership.theway_noti.chord.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            
            // 1단계: 띄어쓰기만 먼저 정리
            String cleaned = symbol.trim().replaceAll("\\s+", "");
            
            // 2단계: 키옮김 파싱 및 적용 (normalize 이전!)
            String actualChordSymbol = processTranspose(cleaned);
            
            // 3단계: 변환된 코드로 기존 파싱
            ChordParseDetailResult detailResult = chordParser.parseChord(actualChordSymbol);
            
            // 기존 클라이언트 호환성: bass 필드에 root+bass 합친 로직
            String compatibleBass = detailResult.getBass() != null ? 
                detailResult.getBass() : detailResult.getRoot();
            
            ChordParseResult result = ChordParseResult.builder()
                .originalSymbol(actualChordSymbol)  // 변환된 결과 저장
                .bass(compatibleBass)  // 기존과 동일한 로직
                .intervals(detailResult.getIntervals())
                .semitones(detailResult.getSemitones())
                .noteNames(detailResult.getNoteNames())
                .unparsedRemainder(detailResult.getUnparsedRemainder())
                .build();
                
            return result;
        } catch (Exception e) {
            log.error("Error parsing chord symbol: {}", symbol, e);
            throw new IllegalArgumentException("Invalid chord symbol: " + symbol, e);
        }
    }

    public GuitarFingeringResult getGuitarFingerings(ChordParseResult chordResult) {
        try {
            
            // 기타 운지법 계산을 위해 detail result 다시 파싱 (root, bass 구분 필요)
            ChordParseDetailResult detailResult = chordParser.parseChord(chordResult.getOriginalSymbol());
            List<GuitarFingering> fingerings = guitarFingeringCalculator.calculateFingerings(detailResult);
            
            GuitarFingeringResult result = GuitarFingeringResult.builder()
                    .chord(chordResult.getOriginalSymbol())
                    .patterns(fingerings)
                    .build();
            
            return result;
        } catch (Exception e) {
            log.error("Error calculating guitar fingerings: {}", chordResult.getOriginalSymbol(), e);
            throw new IllegalArgumentException("Error calculating guitar fingerings", e);
        }
    }

    // 키옮김 처리 메소드들
    private static final Map<String, Integer> NOTE_TO_PITCH_CLASS = Map.of(
        "C", 0, "D", 2, "E", 4, "F", 5, "G", 7, "A", 9, "B", 11
    );

    private static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private String processTranspose(String symbol) {
        Pattern transposePattern = Pattern.compile("^(.+?)([+-]\\d+)$");
        Matcher matcher = transposePattern.matcher(symbol);
        
        if (!matcher.find()) {
            return symbol; // 키옮김 없으면 원본 그대로
        }
        
        String baseSymbol = matcher.group(1);  // "Cmaj7" 또는 "D/F#"
        int semitones = Integer.parseInt(matcher.group(2));  // +2 또는 -3
        
        // 범위 검사
        if (Math.abs(semitones) > 11) {
            throw new IllegalArgumentException("키옮김 범위는 -11~+11 반음 이내: " + semitones);
        }
        
        // 간단하게 루트음과 베이스음만 추출
        String root = extractRoot(baseSymbol);  
        String bass = extractBass(baseSymbol);  
        
        // 변환
        String transposedRoot = transposeNote(root, semitones);
        String result = baseSymbol.replace(root, transposedRoot);
        
        if (bass != null) {
            String transposedBass = transposeNote(bass, semitones);
            result = result.replace("/" + bass, "/" + transposedBass);
        }
        
        return result;
    }

    private String extractRoot(String symbol) {
        Pattern rootPattern = Pattern.compile("^([A-G][#b]?)");
        Matcher matcher = rootPattern.matcher(symbol);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Invalid root note: " + symbol);
    }

    private String extractBass(String symbol) {
        if (!symbol.contains("/")) return null;
        
        String[] parts = symbol.split("/", 2);
        String bassPart = parts[1];
        
        // 베이스음도 동일한 패턴으로 추출
        Pattern bassPattern = Pattern.compile("^([A-G][#b]?)");
        Matcher matcher = bassPattern.matcher(bassPart);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Invalid bass note: " + bassPart);
    }

    private String transposeNote(String note, int semitones) {
        // 플랫을 샤프로 먼저 변환
        String normalizedNote = note;
        if (note.contains("b")) {
            Map<String, String> flatToSharp = Map.of(
                "Db", "C#", "Eb", "D#", "Fb", "E",
                "Gb", "F#", "Ab", "G#", "Bb", "A#", "Cb", "B"
            );
            normalizedNote = flatToSharp.getOrDefault(note, note);
        }
        
        // 현재 피치 클래스 계산
        String noteName = normalizedNote.substring(0, 1);
        int pc = NOTE_TO_PITCH_CLASS.get(noteName);
        
        if (normalizedNote.length() > 1) {
            String accidental = normalizedNote.substring(1);
            if (accidental.equals("#")) {
                pc = (pc + 1) % 12;
            }
        }
        
        // 키옮김 적용
        int newPc = (pc + semitones + 12) % 12;
        
        return NOTE_NAMES[newPc];
    }
}