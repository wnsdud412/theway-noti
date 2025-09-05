package org.silkroadpartnership.theway_noti.chord.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.silkroadpartnership.theway_noti.chord.entity.BarreInfo;
import org.silkroadpartnership.theway_noti.chord.entity.GuitarFingering;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GenerateBarreChordFingeringsTest {

    @InjectMocks
    private GuitarFingeringCalculator calculator;

    @Test
    void testEShapeBarreChords() {
        System.out.println("=== E 셰이프 바레코드 테스트 ===\n");
        
        // E~G# 범위 테스트 (E 셰이프 사용)
        testBarreChord("E", Arrays.asList("E", "G#", "B"), "E");
        testBarreChord("F", Arrays.asList("F", "A", "C"), "E");
        testBarreChord("F#", Arrays.asList("F#", "A#", "C#"), "E");
        testBarreChord("G", Arrays.asList("G", "B", "D"), "E");
        testBarreChord("G#", Arrays.asList("G#", "C", "D#"), "E");
        
        System.out.println("=== E 셰이프 바레코드 테스트 완료 ===\n");
    }

    @Test
    void testAShapeBarreChords() {
        System.out.println("=== A 셰이프 바레코드 테스트 ===\n");
        
        // A 셰이프 사용 범위 테스트
        testBarreChord("A", Arrays.asList("A", "C#", "E"), "A");
        testBarreChord("A#", Arrays.asList("A#", "D", "F"), "A");
        testBarreChord("B", Arrays.asList("B", "D#", "F#"), "A");
        testBarreChord("C", Arrays.asList("C", "E", "G"), "A");
        testBarreChord("C#", Arrays.asList("C#", "F", "G#"), "A");
        testBarreChord("D", Arrays.asList("D", "F#", "A"), "A");
        testBarreChord("D#", Arrays.asList("D#", "G", "A#"), "A");
        
        System.out.println("=== A 셰이프 바레코드 테스트 완료 ===\n");
    }

    @Test
    void testShapeSelection() {
        System.out.println("=== CAGED 셰이프 선택 로직 테스트 ===\n");
        
        // E 셰이프 선택 확인 (E~G#)
        assertShapeSelection("E", "E");
        assertShapeSelection("F", "E");
        assertShapeSelection("F#", "E");
        assertShapeSelection("G", "E");
        assertShapeSelection("G#", "E");
        
        // A 셰이프 선택 확인 (나머지)
        assertShapeSelection("A", "A");
        assertShapeSelection("A#", "A");
        assertShapeSelection("B", "A");
        assertShapeSelection("C", "A");
        assertShapeSelection("C#", "A");
        assertShapeSelection("D", "A");
        assertShapeSelection("D#", "A");
        
        System.out.println("=== 셰이프 선택 로직 테스트 완료 ===\n");
    }

    @Test
    void testSlashChordBarreChords() {
        System.out.println("=== 분수코드 바레코드 테스트 ===\n");
        
        // 분수코드 테스트
        testSlashChord("C", "E", Arrays.asList("C", "E", "G"), "C/E");
        testSlashChord("F", "A", Arrays.asList("F", "A", "C"), "F/A");
        testSlashChord("G", "B", Arrays.asList("G", "B", "D"), "G/B");
        
        System.out.println("=== 분수코드 바레코드 테스트 완료 ===\n");
    }

    @Test
    void testMinorChordBarreChords() {
        System.out.println("=== 마이너 코드 바레코드 테스트 ===\n");
        
        // 마이너 코드 테스트
        testBarreChord("Em", Arrays.asList("E", "G", "B"), "E");
        testBarreChord("Am", Arrays.asList("A", "C", "E"), "A");
        testBarreChord("Dm", Arrays.asList("D", "F", "A"), "A");
        testBarreChord("Fm", Arrays.asList("F", "G#", "C"), "E");
        
        System.out.println("=== 마이너 코드 바레코드 테스트 완료 ===\n");
    }

    @Test
    void testSeventhChordBarreChords() {
        System.out.println("=== 7th 코드 바레코드 테스트 ===\n");
        
        // 7th 코드 테스트
        testBarreChord("E7", Arrays.asList("E", "G#", "B", "D"), "E");
        testBarreChord("A7", Arrays.asList("A", "C#", "E", "G"), "A");
        testBarreChord("F7", Arrays.asList("F", "A", "C", "D#"), "E");
        testBarreChord("C7", Arrays.asList("C", "E", "G", "A#"), "A");
        
        System.out.println("=== 7th 코드 바레코드 테스트 완료 ===\n");
    }

    @Test
    void testEdgeCases() {
        System.out.println("=== 예외상황 테스트 ===\n");
        
        // 빈 구성음 리스트
        List<GuitarFingering> emptyResult = calculator.generateBarreChordFingerings(
            Arrays.asList(), "C", null);
        assertNotNull(emptyResult);
        
        // null 루트음 처리
        assertThrows(Exception.class, () -> {
            calculator.generateBarreChordFingerings(Arrays.asList("C", "E", "G"), null, null);
        });
        
        System.out.println("=== 예외상황 테스트 완료 ===\n");
    }

    private void testBarreChord(String chordSymbol, List<String> chordNotes, String expectedShape) {
        System.out.println("--- " + chordSymbol + " 바레코드 테스트 (" + expectedShape + " 셰이프) ---");
        System.out.println("구성음: " + chordNotes);
        
        String root = extractRoot(chordSymbol);
        List<GuitarFingering> fingerings = calculator.generateBarreChordFingerings(
            chordNotes, root, null);
        
        // 기본 검증
        assertNotNull(fingerings, chordSymbol + " 바레코드 결과가 null이면 안됨");
        
        if (!fingerings.isEmpty()) {
            System.out.println("생성된 바레코드 운지법 수: " + fingerings.size());
            
            for (int i = 0; i < fingerings.size(); i++) {
                GuitarFingering fingering = fingerings.get(i);
                
                // 바레코드 기본 검증
                verifyBarreChordStructure(fingering, chordSymbol, i);
                
                // 바레 정보 검증
                assertNotNull(fingering.getBarre(), 
                    String.format("%s의 %d번째 운지법에 바레 정보가 없음", chordSymbol, i+1));
                
                BarreInfo barre = fingering.getBarre();
                assertTrue(barre.getFret() >= 1 && barre.getFret() <= 12, 
                    String.format("%s의 바레 프렛(%d)이 유효 범위를 벗어남", chordSymbol, barre.getFret()));
                
                // A 셰이프의 경우 6번줄 뮤트 확인
                if ("A".equals(expectedShape)) {
                    assertEquals(-1, fingering.getFrets()[5], 
                        String.format("%s A셰이프에서 6번줄이 뮤트되지 않음", chordSymbol));
                }
                
                // 난이도는 바레코드이므로 최소 intermediate 이상
                assertTrue(Arrays.asList("intermediate", "advanced").contains(fingering.getDifficulty()),
                    String.format("%s 바레코드의 난이도(%s)가 적절하지 않음", chordSymbol, fingering.getDifficulty()));
                
                // 운지법 정보 출력
                System.out.printf("  바레코드 %d: %s (바레: %d프렛, 난이도: %s, 포지션: %d)\n", 
                    i+1, 
                    Arrays.toString(fingering.getFrets()), 
                    barre.getFret(),
                    fingering.getDifficulty(),
                    fingering.getPosition()
                );
            }
        } else {
            System.out.println("바레코드 운지법이 생성되지 않음 (정상적인 경우일 수 있음)");
        }
        
        System.out.println(chordSymbol + " 바레코드 테스트 완료!\n");
    }

    private void testSlashChord(String root, String bass, List<String> chordNotes, String chordSymbol) {
        System.out.println("--- " + chordSymbol + " 분수코드 바레코드 테스트 ---");
        System.out.println("구성음: " + chordNotes + ", 베이스: " + bass);
        
        List<GuitarFingering> fingerings = calculator.generateBarreChordFingerings(
            chordNotes, root, bass);
        
        assertNotNull(fingerings, chordSymbol + " 분수코드 바레코드 결과가 null이면 안됨");
        
        if (!fingerings.isEmpty()) {
            System.out.println("생성된 분수코드 바레코드 운지법 수: " + fingerings.size());
            
            for (int i = 0; i < fingerings.size(); i++) {
                GuitarFingering fingering = fingerings.get(i);
                verifyBarreChordStructure(fingering, chordSymbol, i);
                
                System.out.printf("  분수코드 바레코드 %d: %s (바레: %d프렛, 난이도: %s)\n", 
                    i+1, 
                    Arrays.toString(fingering.getFrets()), 
                    fingering.getBarre().getFret(),
                    fingering.getDifficulty()
                );
            }
        }
        
        System.out.println(chordSymbol + " 분수코드 바레코드 테스트 완료!\n");
    }

    private void assertShapeSelection(String root, String expectedShape) {
        List<String> majorTriad = getMajorTriad(root);
        List<GuitarFingering> fingerings = calculator.generateBarreChordFingerings(
            majorTriad, root, null);
        
        if (!fingerings.isEmpty()) {
            // 셰이프별 특성 확인
            GuitarFingering fingering = fingerings.get(0);
            if ("A".equals(expectedShape)) {
                assertEquals(-1, fingering.getFrets()[5], 
                    String.format("%s는 A셰이프여야 하는데 6번줄이 뮤트되지 않음", root));
            }
            System.out.println(root + " -> " + expectedShape + " 셰이프 선택 확인");
        }
    }

    private void verifyBarreChordStructure(GuitarFingering fingering, String chordSymbol, int index) {
        assertNotNull(fingering, String.format("%s의 %d번째 바레코드 운지법이 null", chordSymbol, index+1));
        assertNotNull(fingering.getFrets(), String.format("%s의 %d번째 바레코드에 프렛 정보가 없음", chordSymbol, index+1));
        assertEquals(6, fingering.getFrets().length, String.format("%s의 %d번째 바레코드가 6개 줄 정보를 가져야 함", chordSymbol, index+1));
        assertNotNull(fingering.getDifficulty(), String.format("%s의 %d번째 바레코드에 난이도 정보가 없음", chordSymbol, index+1));
        assertTrue(fingering.getPosition() >= 1, String.format("%s의 %d번째 바레코드 포지션이 1 이상이어야 함", chordSymbol, index+1));
    }

    private String extractRoot(String chordSymbol) {
        // 코드 기호에서 루트음 추출 (예: "Em" -> "E", "F#7" -> "F#")
        if (chordSymbol.length() > 1 && chordSymbol.charAt(1) == '#') {
            return chordSymbol.substring(0, 2);
        } else if (chordSymbol.length() > 1 && chordSymbol.charAt(1) == 'b') {
            return chordSymbol.substring(0, 2);
        } else {
            return chordSymbol.substring(0, 1);
        }
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