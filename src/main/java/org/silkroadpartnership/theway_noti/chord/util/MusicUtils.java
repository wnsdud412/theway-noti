package org.silkroadpartnership.theway_noti.chord.util;

public class MusicUtils {
    
    // 반음계 번호 (C=0, C#=1, ..., B=11)
    public static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };
    
    // 기타 표준 튜닝 (1번줄부터 6번줄까지)
    public static final int[] GUITAR_TUNING = {
        4,  // 1번줄: E
        11, // 2번줄: B  
        7,  // 3번줄: G
        2,  // 4번줄: D
        9,  // 5번줄: A
        4   // 6번줄: E
    };
    
    /**
     * 음명을 반음계 번호로 변환
     */
    public static int noteToSemitone(String note) {
        if (note == null || note.isEmpty()) {
            throw new IllegalArgumentException("Note cannot be null or empty");
        }
        
        String cleanNote = note.trim().toUpperCase();
        
        for (int i = 0; i < NOTE_NAMES.length; i++) {
            if (NOTE_NAMES[i].equals(cleanNote)) {
                return i;
            }
        }
        
        throw new IllegalArgumentException("Invalid note: " + note);
    }
    
    /**
     * 반음계 번호를 음명으로 변환
     */
    public static String semitoneToNote(int semitone) {
        if (semitone < 0 || semitone > 11) {
            semitone = ((semitone % 12) + 12) % 12; // 음수 처리
        }
        return NOTE_NAMES[semitone];
    }
    
    /**
     * 특정 줄의 특정 프렛에서 나는 음의 반음계 번호 계산
     */
    public static int calculateSemitoneAt(int stringNumber, int fret) {
        if (stringNumber < 1 || stringNumber > 6) {
            throw new IllegalArgumentException("String number must be between 1-6");
        }
        if (fret < 0) {
            throw new IllegalArgumentException("Fret number cannot be negative");
        }
        
        int openStringSemitone = GUITAR_TUNING[stringNumber - 1];
        return (openStringSemitone + fret) % 12;
    }
    
    /**
     * 특정 줄의 특정 프렛에서 나는 음명 계산
     */
    public static String calculateNoteAt(int stringNumber, int fret) {
        int semitone = calculateSemitoneAt(stringNumber, fret);
        return semitoneToNote(semitone);
    }
    
    /**
     * 특정 줄에서 특정 음을 연주할 수 있는 프렛들 찾기 (0~12프렛)
     */
    public static int[] findFretsForNote(int stringNumber, String note) {
        int targetSemitone = noteToSemitone(note);
        int openStringSemitone = GUITAR_TUNING[stringNumber - 1];
        
        java.util.List<Integer> frets = new java.util.ArrayList<>();
        
        for (int fret = 0; fret <= 12; fret++) {
            int currentSemitone = (openStringSemitone + fret) % 12;
            if (currentSemitone == targetSemitone) {
                frets.add(fret);
            }
        }
        
        return frets.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * 반음계 차이 계산 (offset)
     */
    public static int calculateOffset(String fromNote, String toNote) {
        int fromSemitone = noteToSemitone(fromNote);
        int toSemitone = noteToSemitone(toNote);
        return toSemitone - fromSemitone;
    }
}