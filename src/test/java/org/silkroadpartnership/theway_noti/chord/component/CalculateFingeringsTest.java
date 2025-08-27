package org.silkroadpartnership.theway_noti.chord.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.silkroadpartnership.theway_noti.chord.entity.ChordParseDetailResult;
import org.silkroadpartnership.theway_noti.chord.entity.GuitarFingering;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CalculateFingeringsTest {

    @InjectMocks
    private GuitarFingeringCalculator calculator;

    @Test
    void testCalculateFingeringsForOpenChords() {
        System.out.println("=== 오픈코드 우선 시도 테스트 ===\n");
        
        // C 메이저 - 오픈코드 가능
        testChordFingering("C", "C", null, Arrays.asList("C", "E", "G"), true);
        
        // G 메이저 - 오픈코드 가능  
        testChordFingering("G", "G", null, Arrays.asList("G", "B", "D"), true);
        
        // A 메이저 - 오픈코드 가능
        testChordFingering("A", "A", null, Arrays.asList("A", "C#", "E"), true);
        
        // D 메이저 - 오픈코드 가능
        testChordFingering("D", "D", null, Arrays.asList("D", "F#", "A"), true);
        
        // E 메이저 - 오픈코드 가능
        testChordFingering("E", "E", null, Arrays.asList("E", "G#", "B"), true);
        
        System.out.println("=== 오픈코드 우선 시도 테스트 완료 ===\n");
    }

    @Test
    void testCalculateFingeringsForBarreChords() {
        System.out.println("=== 바레코드 fallback 테스트 ===\n");
        
        // F 메이저 - 오픈코드 어려움, 바레코드 사용
        testChordFingering("F", "F", null, Arrays.asList("F", "A", "C"), false);
        
        // B 메이저 - 오픈코드 어려움, 바레코드 사용  
        testChordFingering("B", "B", null, Arrays.asList("B", "D#", "F#"), false);
        
        // F# 메이저 - 오픈코드 어려움, 바레코드 사용
        testChordFingering("F#", "F#", null, Arrays.asList("F#", "A#", "C#"), false);
        
        // C# 메이저 - 오픈코드 어려움, 바레코드 사용
        testChordFingering("C#", "C#", null, Arrays.asList("C#", "F", "G#"), false);
        
        System.out.println("=== 바레코드 fallback 테스트 완료 ===\n");
    }

    @Test
    void testCalculateFingeringsForMinorChords() {
        System.out.println("=== 마이너 코드 테스트 ===\n");
        
        // Am - 오픈코드 가능
        testChordFingering("Am", "A", null, Arrays.asList("A", "C", "E"), true);
        
        // Em - 오픈코드 가능
        testChordFingering("Em", "E", null, Arrays.asList("E", "G", "B"), true);
        
        // Dm - 오픈코드 가능
        testChordFingering("Dm", "D", null, Arrays.asList("D", "F", "A"), true);
        
        // Fm - 바레코드 필요
        testChordFingering("Fm", "F", null, Arrays.asList("F", "G#", "C"), false);
        
        System.out.println("=== 마이너 코드 테스트 완료 ===\n");
    }

    @Test
    void testCalculateFingeringsForSeventhChords() {
        System.out.println("=== 7th 코드 테스트 ===\n");
        
        // G7 - 오픈코드 가능
        testChordFingering("G7", "G", null, Arrays.asList("G", "B", "D", "F"), true);
        
        // C7 - 오픈코드 가능
        testChordFingering("C7", "C", null, Arrays.asList("C", "E", "G", "A#"), true);
        
        // D7 - 오픈코드 가능
        testChordFingering("D7", "D", null, Arrays.asList("D", "F#", "A", "C"), true);
        
        // F7 - 바레코드 필요
        testChordFingering("F7", "F", null, Arrays.asList("F", "A", "C", "D#"), false);
        
        System.out.println("=== 7th 코드 테스트 완료 ===\n");
    }

    @Test
    void testCalculateFingeringsForSlashChords() {
        System.out.println("=== 분수코드 테스트 ===\n");
        
        // C/E - 베이스음이 루트와 다름
        testSlashChordFingering("C/E", "C", "E", Arrays.asList("C", "E", "G"));
        
        // F/A - 베이스음이 루트와 다름
        testSlashChordFingering("F/A", "F", "A", Arrays.asList("F", "A", "C"));
        
        // G/B - 베이스음이 루트와 다름  
        testSlashChordFingering("G/B", "G", "B", Arrays.asList("G", "B", "D"));
        
        // Am/C - 마이너 분수코드
        testSlashChordFingering("Am/C", "A", "C", Arrays.asList("A", "C", "E"));
        
        System.out.println("=== 분수코드 테스트 완료 ===\n");
    }

    @Test
    void testCalculateFingeringsResultStructure() {
        System.out.println("=== 결과 구조 테스트 ===\n");
        
        // 기본적인 C 메이저로 결과 구조 검증
        ChordParseDetailResult chordResult = createChordResult("C", "C", null, Arrays.asList("C", "E", "G"));
        List<GuitarFingering> fingerings = calculator.calculateFingerings(chordResult);
        
        // 기본 검증
        assertNotNull(fingerings, "결과가 null이면 안됨");
        assertTrue(fingerings.size() <= 1, "최대 1개의 운지법만 반환해야 함");
        
        if (!fingerings.isEmpty()) {
            GuitarFingering fingering = fingerings.get(0);
            
            // 운지법 구조 검증
            assertNotNull(fingering, "운지법이 null이면 안됨");
            assertNotNull(fingering.getFrets(), "프렛 정보가 없으면 안됨");
            assertEquals(6, fingering.getFrets().length, "6개 줄 정보를 가져야 함");
            assertNotNull(fingering.getDifficulty(), "난이도 정보가 없으면 안됨");
            assertTrue(fingering.getPosition() >= 1, "포지션은 1 이상이어야 함");
            
            // 난이도 값 검증
            assertTrue(Arrays.asList("beginner", "intermediate", "advanced").contains(fingering.getDifficulty()),
                "유효한 난이도 값이어야 함");
            
            System.out.println("C 메이저 운지법 구조: " + Arrays.toString(fingering.getFrets()) + 
                " (난이도: " + fingering.getDifficulty() + ", 포지션: " + fingering.getPosition() + ")");
        }
        
        System.out.println("=== 결과 구조 테스트 완료 ===\n");
    }

    @Test
    void testCalculateFingeringsEdgeCases() {
        System.out.println("=== 예외 케이스 테스트 ===\n");
        
        // null 루트음
        assertThrows(Exception.class, () -> {
            ChordParseDetailResult chordResult = createChordResult("invalid", null, null, Arrays.asList("C", "E", "G"));
            calculator.calculateFingerings(chordResult);
        }, "null 루트음에서 예외 발생해야 함");
        
        // 빈 구성음 리스트
        ChordParseDetailResult emptyChordResult = createChordResult("empty", "C", null, Arrays.asList());
        List<GuitarFingering> emptyResult = calculator.calculateFingerings(emptyChordResult);
        assertNotNull(emptyResult, "빈 구성음에서도 결과는 null이면 안됨");
        
        // 루트음과 베이스음이 같은 경우 (일반 코드)
        testChordFingering("C_same_bass", "C", "C", Arrays.asList("C", "E", "G"), true);
        
        System.out.println("=== 예외 케이스 테스트 완료 ===\n");
    }

    @Test
    void testOpenChordVsBarreChordPriority() {
        System.out.println("=== 오픈코드 vs 바레코드 우선순위 테스트 ===\n");
        
        // 오픈코드가 가능한 코드들은 바레코드를 생성하지 않아야 함
        String[] openChords = {"C", "G", "A", "D", "E"};
        
        for (String chord : openChords) {
            List<String> majorTriad = getMajorTriad(chord);
            ChordParseDetailResult chordResult = createChordResult(chord, chord, null, majorTriad);
            List<GuitarFingering> fingerings = calculator.calculateFingerings(chordResult);
            
            if (!fingerings.isEmpty()) {
                GuitarFingering fingering = fingerings.get(0);
                
                // 오픈코드는 바레 정보가 없어야 함
                assertNull(fingering.getBarre(), chord + " 오픈코드에 바레 정보가 있으면 안됨");
                
                // 오픈코드는 beginner 또는 intermediate 난이도여야 함
                assertTrue(Arrays.asList("beginner", "intermediate").contains(fingering.getDifficulty()),
                    chord + " 오픈코드의 난이도가 적절하지 않음: " + fingering.getDifficulty());
                
                System.out.println(chord + " -> 오픈코드 우선 선택됨 (난이도: " + fingering.getDifficulty() + ")");
            }
        }
        
        System.out.println("=== 오픈코드 vs 바레코드 우선순위 테스트 완료 ===\n");
    }

    // 헬퍼 메소드들
    
    private void testChordFingering(String chordSymbol, String root, String bass, List<String> chordNotes, boolean expectOpenChord) {
        System.out.println("--- " + chordSymbol + " 테스트 ---");
        System.out.println("구성음: " + chordNotes + (bass != null ? ", 베이스: " + bass : ""));
        
        ChordParseDetailResult chordResult = createChordResult(chordSymbol, root, bass, chordNotes);
        List<GuitarFingering> fingerings = calculator.calculateFingerings(chordResult);
        
        // 기본 검증
        assertNotNull(fingerings, chordSymbol + " 결과가 null이면 안됨");
        assertTrue(fingerings.size() <= 1, chordSymbol + "은 최대 1개의 운지법만 반환해야 함");
        
        if (!fingerings.isEmpty()) {
            GuitarFingering fingering = fingerings.get(0);
            
            // 운지법 타입 검증
            if (expectOpenChord) {
                assertNull(fingering.getBarre(), chordSymbol + "은 오픈코드여야 하는데 바레 정보가 있음");
                System.out.println("  -> 오픈코드: " + Arrays.toString(fingering.getFrets()) + 
                    " (난이도: " + fingering.getDifficulty() + ")");
            } else {
                assertNotNull(fingering.getBarre(), chordSymbol + "은 바레코드여야 하는데 바레 정보가 없음");
                assertTrue(Arrays.asList("intermediate", "advanced").contains(fingering.getDifficulty()),
                    chordSymbol + " 바레코드의 난이도가 적절하지 않음");
                System.out.println("  -> 바레코드: " + Arrays.toString(fingering.getFrets()) + 
                    " (바레: " + fingering.getBarre().getFret() + "프렛, 난이도: " + fingering.getDifficulty() + ")");
            }
        } else {
            System.out.println("  -> 운지법 생성 실패");
        }
        
        System.out.println(chordSymbol + " 테스트 완료!\n");
    }

    private void testSlashChordFingering(String chordSymbol, String root, String bass, List<String> chordNotes) {
        System.out.println("--- " + chordSymbol + " 분수코드 테스트 ---");
        System.out.println("루트: " + root + ", 베이스: " + bass + ", 구성음: " + chordNotes);
        
        ChordParseDetailResult chordResult = createChordResult(chordSymbol, root, bass, chordNotes);
        List<GuitarFingering> fingerings = calculator.calculateFingerings(chordResult);
        
        assertNotNull(fingerings, chordSymbol + " 분수코드 결과가 null이면 안됨");
        assertTrue(fingerings.size() <= 1, chordSymbol + " 분수코드는 최대 1개의 운지법만 반환해야 함");
        
        if (!fingerings.isEmpty()) {
            GuitarFingering fingering = fingerings.get(0);
            
            System.out.println("  -> 분수코드 운지법: " + Arrays.toString(fingering.getFrets()) + 
                " (난이도: " + fingering.getDifficulty() + 
                (fingering.getBarre() != null ? ", 바레: " + fingering.getBarre().getFret() + "프렛" : "") + ")");
        } else {
            System.out.println("  -> 분수코드 운지법 생성 실패");
        }
        
        System.out.println(chordSymbol + " 분수코드 테스트 완료!\n");
    }

    private ChordParseDetailResult createChordResult(String originalSymbol, String root, String bass, List<String> noteNames) {
        return ChordParseDetailResult.builder()
                .originalSymbol(originalSymbol)
                .root(root)
                .bass(bass)
                .intervals(Set.of()) // 간단한 테스트를 위해 빈 Set
                .semitones(Arrays.asList()) // 간단한 테스트를 위해 빈 List
                .noteNames(noteNames)
                .build();
    }

    private List<String> getMajorTriad(String root) {
        // 루트음에 따른 메이저 3화음 반환
        switch (root) {
            case "C": return Arrays.asList("C", "E", "G");
            case "C#": return Arrays.asList("C#", "F", "G#");
            case "D": return Arrays.asList("D", "F#", "A");
            case "D#": return Arrays.asList("D#", "G", "A#");
            case "E": return Arrays.asList("E", "G#", "B");
            case "F": return Arrays.asList("F", "A", "C");
            case "F#": return Arrays.asList("F#", "A#", "C#");
            case "G": return Arrays.asList("G", "B", "D");
            case "G#": return Arrays.asList("G#", "C", "D#");
            case "A": return Arrays.asList("A", "C#", "E");
            case "A#": return Arrays.asList("A#", "D", "F");
            case "B": return Arrays.asList("B", "D#", "F#");
            default: return Arrays.asList(root, root, root); // fallback
        }
    }
}