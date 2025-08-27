package org.silkroadpartnership.theway_noti.chord.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.silkroadpartnership.theway_noti.chord.entity.GuitarFingering;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GenerateOpenChordFingeringsTest {

    @InjectMocks
    private GuitarFingeringCalculator calculator;

    @Test
    void testGenerateOpenChordFingeringsForAllBasicChords() {
        System.out.println("=== 기본 화음기호(A~G) generateOpenChordFingerings 테스트 ===\n");
        
        // A 메이저 코드 테스트
        testChord("A", Arrays.asList("A", "C#", "E"));
        
        // B 메이저 코드 테스트
        //testChord("B", Arrays.asList("B", "D#", "F#"));
        
        // C 메이저 코드 테스트
        testChord("C", Arrays.asList("C", "E", "G"));
        
        // D 메이저 코드 테스트
        testChord("D", Arrays.asList("D", "F#", "A"));
        
        // E 메이저 코드 테스트
        testChord("E", Arrays.asList("E", "G#", "B"));
        
        // F 메이저 코드 테스트
        //testChord("F", Arrays.asList("F", "A", "C"));
        
        // G 메이저 코드 테스트
        testChord("G", Arrays.asList("G", "B", "D"));
        
        System.out.println("=== 모든 기본 화음기호 테스트 완료 ===");
    }
    
    private void testChord(String chordSymbol, List<String> chordNotes) {
        System.out.println("--- " + chordSymbol + " 메이저 코드 테스트 ---");
        System.out.println("구성음: " + chordNotes);
        
        // generateOpenChordFingerings 메소드 호출
        List<GuitarFingering> fingerings = calculator.generateOpenChordFingerings(
            chordNotes, 
            chordSymbol,  // root
            null,         // bass (분수코드 아님)
            false,         // isSlashChord
            4
        );
        
        // 기본 검증
        assertNotNull(fingerings, chordSymbol + " 코드의 결과가 null이면 안됨");
        assertFalse(fingerings.isEmpty(), chordSymbol + " 코드의 운지법이 최소 1개는 생성되어야 함");
        
        System.out.println("생성된 운지법 수: " + fingerings.size());
        
        // 각 운지법 검증
        for (int i = 0; i < fingerings.size(); i++) {
            GuitarFingering fingering = fingerings.get(i);
            
            // 기본 구조 검증
            assertNotNull(fingering, String.format("%s 코드의 %d번째 운지법이 null", chordSymbol, i+1));
            assertNotNull(fingering.getFrets(), String.format("%s 코드의 %d번째 운지법에 프렛 정보가 없음", chordSymbol, i+1));
            assertEquals(6, fingering.getFrets().length, String.format("%s 코드의 %d번째 운지법이 6개 줄 정보를 가져야 함", chordSymbol, i+1));
            assertNotNull(fingering.getDifficulty(), String.format("%s 코드의 %d번째 운지법에 난이도 정보가 없음", chordSymbol, i+1));
            
            // 운지법 정보 출력
            System.out.printf("  운지법 %d: %s (난이도: %s, 포지션: %d)\n", 
                i+1, 
                Arrays.toString(fingering.getFrets()), 
                fingering.getDifficulty(),
                fingering.getPosition()
            );
        }
        
        System.out.println(chordSymbol + " 코드 테스트 성공!\n");
    }
}