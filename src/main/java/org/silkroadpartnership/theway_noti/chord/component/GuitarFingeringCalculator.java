package org.silkroadpartnership.theway_noti.chord.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.silkroadpartnership.theway_noti.chord.entity.BarreInfo;
import org.silkroadpartnership.theway_noti.chord.entity.ChordParseDetailResult;
import org.silkroadpartnership.theway_noti.chord.entity.GuitarFingering;
import org.silkroadpartnership.theway_noti.chord.util.MusicUtils;
import org.springframework.stereotype.Component;

@Component
public class GuitarFingeringCalculator {
    
    // 개방현 음계 (인덱스 1-6에 맞춤, 0번 인덱스는 미사용)
    private static final String[] OPEN_STRINGS = {"", "E", "B", "G", "D", "A", "E"};
    
    public List<GuitarFingering> calculateFingerings(ChordParseDetailResult chordResult) {
        List<String> chordNotes = chordResult.getNoteNames();
        String root = chordResult.getRoot();
        String bass = chordResult.getBass();
        
        // 분수코드 판별
        boolean needsSlashChordProcessing = (bass != null) && (!bass.equals(root));
        
        List<GuitarFingering> allFingerings = new ArrayList<>();
        
        // spec_2.md에 따른 오픈코드 → 바레코드 순서로 시도
        allFingerings.addAll(generateOpenChordFingerings(chordNotes, root, bass, needsSlashChordProcessing, 4));
        
        // 오픈코드가 하나도 없을 때만 바레코드 시도
        if (allFingerings.isEmpty()) {
            allFingerings.addAll(generateBarreChordFingerings(chordNotes, root, bass));
        }
        
        return allFingerings;
    }
    
    /**
     * spec_2.md 기반 오픈코드 찾기 알고리즘
     */
    List<GuitarFingering> generateOpenChordFingerings(List<String> chordNotes, String root, String bass, boolean isSlashChord, int maxFingers) {
        List<GuitarFingering> fingerings = new ArrayList<>();
        
        // 1. 루트음 배치 시도 (4,5,6번줄에서 0~4프렛)
        for (int rootString = 6; rootString >= 4; rootString--) {
            int[] rootFrets = MusicUtils.findFretsForNote(rootString, root);
            
            for (int rootFret : rootFrets) {
                if (rootFret > 3) continue; // 3프렛 제한
                
                // 4번줄(D현) 특별 제한: D(0프렛)와 D#(1프렛)만 허용
                if (rootString == 4 && rootFret > 1) continue;
                
                int[] pattern = new int[7]; // 0번 인덱스 미사용, 1~6번줄
                Arrays.fill(pattern, -2); // -2: 미정 상태, -1: 뮤트, 0: 개방현, 1~: 프렛
                pattern[rootString] = rootFret;
                
                // 베이스 규칙 적용
                boolean validMute = applyMuteRules(pattern, rootString);
                if (!validMute) continue;
                
                // 구성음 배치
                boolean success = placeChordNotes(pattern, chordNotes, rootString);
                if (!success) continue;
                
                // 분수코드 처리
                if (isSlashChord) {
                    success = handleSlashChord(pattern, root, bass, rootString);
                    if (!success) continue;
                }
                
                // 물리적 제약 검증
                if (isPhysicallyPlayable(Arrays.copyOfRange(pattern, 1, 7), maxFingers)) {
                    GuitarFingering fingering = createFingeringFromPattern(pattern);
                    if (fingering != null) {
                        fingerings.add(fingering);
                        return fingerings; // 첫 번째 운지법 생성 시 즉시 반환
                    }
                }
            }
        }
        
        return fingerings;
    }
    
    /**
     * spec_2.md 기반 바레코드 찾기 알고리즘 (CAGED 시스템)
     */
    List<GuitarFingering> generateBarreChordFingerings(List<String> chordNotes, String root, String bass) {
        List<GuitarFingering> fingerings = new ArrayList<>();
        
        // 셰이프 선택
        String shapeType = selectCAGEDShape(root);
        
        if ("E".equals(shapeType)) {
            fingerings.addAll(generateEShapeBarre(chordNotes, root, bass));
        } else if ("A".equals(shapeType)) {
            fingerings.addAll(generateAShapeBarre(chordNotes, root, bass));
        }
        
        return fingerings;
    }
    
    /**
     * CAGED 셰이프 선택
     */
    private String selectCAGEDShape(String root) {
        int rootSemitone = MusicUtils.noteToSemitone(root);
        
        // E~G# (4~8): E 셰이프
        if (rootSemitone >= 4 && rootSemitone <= 8) {
            return "E";
        }
        
        // 나머지는 A 셰이프
        return "A";
    }
    
    /**
     * E 셰이프 바레코드 생성 (평행이동 알고리즘)
     */
    private List<GuitarFingering> generateEShapeBarre(List<String> chordNotes, String root, String bass) {
        List<GuitarFingering> fingerings = new ArrayList<>();
        
        // 1단계: 구성음 평행이동
        int offset = MusicUtils.calculateOffset(root, "E"); // E(4) - root
        List<String> shiftedNotes = new ArrayList<>();
        for (String note : chordNotes) {
            int semitone = MusicUtils.noteToSemitone(note);
            int shiftedSemitone = (semitone + offset + 12) % 12;
            shiftedNotes.add(MusicUtils.semitoneToNote(shiftedSemitone));
        }
        
        // 2단계: 이동된 구성음으로 패턴 찾기 (E 메이저 기본 형태 활용)
        List<GuitarFingering> baseOpenFingerings = generateOpenChordFingerings(shiftedNotes, "E", null, false, 3);
        if (baseOpenFingerings.isEmpty()) return fingerings;
        
        int[] basePattern = baseOpenFingerings.get(0).getFrets(); // 첫 번째 패턴 사용
        // 1~6번줄을 0~7 인덱스로 변환
        int[] extendedPattern = new int[7];
        extendedPattern[0] = -1; // 미사용
        System.arraycopy(basePattern, 0, extendedPattern, 1, 6);
        
        // 3단계: 역평행이동
        int barrePosition = ((-offset % 12) + 12) % 12; // 12프렛 옥타브 범위로 조정
        if (barrePosition == 0) barrePosition = 12; // 0프렛은 12프렛으로 조정
        if (barrePosition < 1 || barrePosition > 12) return fingerings;
        
        int[] finalPattern = new int[7];
        int minFret = Integer.MAX_VALUE;
        
        for (int string = 1; string <= 6; string++) {
            if (extendedPattern[string] >= 0) {
                int fretPosition = extendedPattern[string] - offset;
                // 음수 프렛을 12프렛 옥타브 범위로 조정
                finalPattern[string] = ((fretPosition % 12) + 12) % 12;
                if (finalPattern[string] == 0) finalPattern[string] = 12;
                if (finalPattern[string] > 0) {
                    minFret = Math.min(minFret, finalPattern[string]);
                }
            } else {
                finalPattern[string] = -1;
            }
        }
        
        // minFret가 여전히 MAX_VALUE이면 바레 포지션을 사용
        if (minFret == Integer.MAX_VALUE) {
            minFret = barrePosition;
        }
        
        // 물리적 제약 검증
        if (isValidBarrePattern(finalPattern, minFret)) {
            GuitarFingering fingering = createBarreFingeringFromPattern(finalPattern, minFret);
            if (fingering != null) {
                fingerings.add(fingering);
            }
        }
        
        return fingerings;
    }
    
    /**
     * A 셰이프 바레코드 생성 (평행이동 알고리즘)
     */
    private List<GuitarFingering> generateAShapeBarre(List<String> chordNotes, String root, String bass) {
        List<GuitarFingering> fingerings = new ArrayList<>();
        
        // 1단계: 구성음 평행이동
        int offset = MusicUtils.calculateOffset(root, "A"); // A(9) - root
        List<String> shiftedNotes = new ArrayList<>();
        for (String note : chordNotes) {
            int semitone = MusicUtils.noteToSemitone(note);
            int shiftedSemitone = (semitone + offset + 12) % 12;
            shiftedNotes.add(MusicUtils.semitoneToNote(shiftedSemitone));
        }
        
        // 2단계: 이동된 구성음으로 패턴 찾기 (A 메이저 기본 형태 활용)
        List<GuitarFingering> baseOpenFingerings = generateOpenChordFingerings(shiftedNotes, "A", null, false, 3);
        if (baseOpenFingerings.isEmpty()) return fingerings;
        
        int[] basePattern = baseOpenFingerings.get(0).getFrets(); // 첫 번째 패턴 사용
        // 1~6번줄을 0~7 인덱스로 변환
        int[] extendedPattern = new int[7];
        extendedPattern[0] = -1; // 미사용
        System.arraycopy(basePattern, 0, extendedPattern, 1, 6);
        
        // 3단계: 역평행이동
        int barrePosition = ((-offset % 12) + 12) % 12; // 12프렛 옥타브 범위로 조정
        if (barrePosition == 0) barrePosition = 12; // 0프렛은 12프렛으로 조정
        if (barrePosition < 1 || barrePosition > 12) return fingerings;
        
        int[] finalPattern = new int[7];
        int minFret = Integer.MAX_VALUE;
        
        for (int string = 1; string <= 6; string++) {
            if (extendedPattern[string] >= 0) {
                int fretPosition = extendedPattern[string] - offset;
                // 음수 프렛을 12프렛 옥타브 범위로 조정
                finalPattern[string] = ((fretPosition % 12) + 12) % 12;
                if (finalPattern[string] == 0) finalPattern[string] = 12;
                if (finalPattern[string] > 0) {
                    minFret = Math.min(minFret, finalPattern[string]);
                }
            } else {
                finalPattern[string] = -1;
            }
        }
        finalPattern[6] = -1; // A 셰이프는 6번줄 뮤트
        
        // minFret가 여전히 MAX_VALUE이면 바레 포지션을 사용
        if (minFret == Integer.MAX_VALUE) {
            minFret = barrePosition;
        }
        
        // 물리적 제약 검증
        if (isValidBarrePattern(finalPattern, minFret)) {
            GuitarFingering fingering = createBarreFingeringFromPattern(finalPattern, minFret);
            if (fingering != null) {
                fingerings.add(fingering);
            }
        }
        
        return fingerings;
    }
    
    
    /**
     * 바레 패턴 유효성 검증
     */
    private boolean isValidBarrePattern(int[] pattern, int barreFret) {
        // 3프렛 스팬 제한
        int maxFret = 0;
        for (int string = 1; string <= 6; string++) {
            if (pattern[string] > 0) {
                maxFret = Math.max(maxFret, pattern[string]);
            }
        }
        
        return (maxFret - barreFret) <= 3;
    }
    
    /**
     * 바레 패턴으로부터 GuitarFingering 생성
     */
    private GuitarFingering createBarreFingeringFromPattern(int[] pattern, int barreFret) {
        int[] frets = Arrays.copyOfRange(pattern, 1, 7);
        
        BarreInfo barre = BarreInfo.builder()
                .fret(barreFret)
                .fromString(1)
                .toString(6)
                .build();
        
        String difficulty = "intermediate"; // 바레코드는 기본적으로 intermediate
        
        return GuitarFingering.builder()
                .frets(frets)
                .barre(barre)
                .difficulty(difficulty)
                .position(barreFret)
                .build();
    }
    
    // ===== spec_2.md 기반 오픈코드 알고리즘 헬퍼 메소드들 =====
    
    /**
     * 베이스 규칙 적용 (뮤트 제약사항)
     */
    boolean applyMuteRules(int[] pattern, int rootString) {
        // 6번줄에서 루트음 연주: 뮤트 없음
        if (rootString == 6) {
            return true;
        }
        
        // 5번줄에서 루트음 연주: 6번줄만 뮤트
        if (rootString == 5) {
            pattern[6] = -1;
            return true;
        }
        
        // 4번줄에서 루트음 연주: 5,6번줄 뮤트
        if (rootString == 4) {
            pattern[5] = -1;
            pattern[6] = -1;
            return true;
        }
        
        return false;
    }
    
    /**
     * 개선된 구성음 배치 (오픈코드 최적화)
     */
    boolean placeChordNotes(int[] pattern, List<String> chordNotes, int rootString) {
        Set<String> placedNotes = new HashSet<>();
        
        // 루트음 추가
        String rootNote = MusicUtils.calculateNoteAt(rootString, pattern[rootString]);
        placedNotes.add(rootNote);
        
        // 1단계: 개방현 우선 배치
        placeOpenStrings(pattern, chordNotes, placedNotes);
        
        // 2단계: 부족한 구성음 찾기
        List<String> missingNotes = findMissingNotes(chordNotes, placedNotes);
        
        // 3단계: 미정 상태 현에서 부족한 구성음 배치
        placeMissingNotes(pattern, missingNotes, placedNotes);
        
        // 4단계: 미정 상태 현에서 구성음 재탐색 후 실패 처리
        for (int string = 1; string <= 6; string++) {
            if (pattern[string] == -2) { // 미정 상태인 줄
                boolean found = false;
                
                // 구성음 중 아무거나 배치 가능한지 확인
                for (String note : chordNotes) {
                    int[] possibleFrets = MusicUtils.findFretsForNote(string, note);
                    for (int fret : possibleFrets) {
                        if (fret >= 0 && fret <= 3) { // 0~3프렛 이내 (개방현 포함)
                            pattern[string] = fret;
                            placedNotes.add(note);
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                
                // 구성음을 찾지 못하면 코드 생성 실패
                if (!found) {
                    return false;
                }
            }
        }
        
        // 파워코드는 2개, 일반 코드는 최소 3개 구성음 필요
        int minimumNotes = chordNotes.size() == 2 ? 2 : 3;
        return placedNotes.size() >= Math.min(minimumNotes, chordNotes.size());
    }
    
    /**
     * 분수코드 처리
     */
    boolean handleSlashChord(int[] pattern, String root, String bass, int rootString) {
        if (bass == null || bass.equals(root)) return true;
        
        // 방법 1: 뮤트현 활용
        if (tryMutedStringForBass(pattern, bass)) {
            return true;
        }
        
        // 방법 2: 루트음 재배치 
        return tryRootReplacementForBass(pattern, root, bass, rootString);
    }
    
    /**
     * 방법 1: 뮤트현 활용
     */
    private boolean tryMutedStringForBass(int[] pattern, String bass) {
        for (int string = 6; string >= 4; string--) { // 높은현부터 시도
            if (pattern[string] == -1) { // 뮤트된 현
                int[] possibleFrets = MusicUtils.findFretsForNote(string, bass);
                for (int fret : possibleFrets) {
                    if (fret <= 3) {
                        pattern[string] = fret;
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 방법 2: 루트음 재배치
     */
    private boolean tryRootReplacementForBass(int[] pattern, String root, String bass, int currentRootString) {
        for (int bassString = 6; bassString >= 1; bassString--) {
            int[] possibleFrets = MusicUtils.findFretsForNote(bassString, bass);
            for (int bassFret : possibleFrets) {
                if (bassFret > 3) continue;
                
                // 베이스 배치
                int[] newPattern = pattern.clone();
                newPattern[bassString] = bassFret;
                
                // 베이스현보다 높은현들 뮤트
                for (int i = bassString + 1; i <= 6; i++) {
                    newPattern[i] = -1;
                }
                
                // 루트음이 다른현에서 연주되는지 확인
                boolean rootCovered = false;
                for (int string = 1; string < bassString; string++) {
                    if (newPattern[string] > 0) {
                        String noteAtString = MusicUtils.calculateNoteAt(string, newPattern[string]);
                        if (root.equals(noteAtString)) {
                            rootCovered = true;
                            break;
                        }
                    }
                }
                
                if (rootCovered) {
                    System.arraycopy(newPattern, 0, pattern, 0, newPattern.length);
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 물리적 연주 가능성 검증
     */
    boolean isPhysicallyPlayable(int[] frets, int maxFingers) {
        List<Integer> pressedFrets = Arrays.stream(frets)   
        .filter(fret -> fret > 0)                  
        .boxed()                                   
        .collect(Collectors.toList()); 
        
        if (pressedFrets.isEmpty()) {
            return true;
        }
        
        // 1. 손가락 수 제한
        if (pressedFrets.size() > maxFingers) {
            return false;
        }
        
        // 2. 4프렛 스팬 제한
        int minFret = Collections.min(pressedFrets);
        int maxFret = Collections.max(pressedFrets);
        
        return (maxFret - minFret) <= 3;
    }
    
    /**
     * 패턴으로부터 GuitarFingering 생성
     */
    GuitarFingering createFingeringFromPattern(int[] pattern) {
        int[] frets = Arrays.copyOfRange(pattern, 1, 7); // 1~6번줄만 복사
        
        String difficulty = calculateDifficulty(pattern);
        int position = calculatePosition(pattern);
        
        return GuitarFingering.builder()
                .frets(frets)
                .barre(null)
                .difficulty(difficulty)
                .position(position)
                .build();
    }
    
    
    /**
     * 난이도 계산
     */
    private String calculateDifficulty(int[] pattern) {
        int openStrings = 0;
        int mutedStrings = 0;
        int maxFret = 0;
        
        for (int string = 1; string <= 6; string++) {
            if (pattern[string] == 0) {
                openStrings++;
            } else if (pattern[string] == -1) {
                mutedStrings++;
            } else if (pattern[string] > 0) {
                maxFret = Math.max(maxFret, pattern[string]);
            }
        }
        
        if (openStrings >= 2 && maxFret <= 3 && mutedStrings <= 1) {
            return "beginner";
        } else if (maxFret <= 5 && mutedStrings <= 2) {
            return "intermediate";
        } else {
            return "advanced";
        }
    }
    
    /**
     * 포지션 계산
     */
    private int calculatePosition(int[] pattern) {
        int minFret = Integer.MAX_VALUE;
        for (int string = 1; string <= 6; string++) {
            if (pattern[string] > 0) {
                minFret = Math.min(minFret, pattern[string]);
            }
        }
        return minFret == Integer.MAX_VALUE ? 1 : minFret;
    }
    
    
    /**
     * 1단계: 개방현 우선 배치
     */
    void placeOpenStrings(int[] pattern, List<String> chordNotes, Set<String> placedNotes) {
        for (int string = 1; string <= 6; string++) {
            if (pattern[string] == -2) { // 미정 상태인 줄만 처리
                String openNote = OPEN_STRINGS[string];
                if (chordNotes.contains(openNote)) {
                    pattern[string] = 0; // 개방현으로 설정
                    placedNotes.add(openNote);
                }
            }
        }
    }
    
    /**
     * 2단계: 부족한 구성음 찾기
     */
    List<String> findMissingNotes(List<String> chordNotes, Set<String> placedNotes) {
        return chordNotes.stream()
                .filter(note -> !placedNotes.contains(note))
                .collect(Collectors.toList());
    }
    
    /**
     * 3단계: 미정 상태 현에서 부족한 구성음 배치
     */
    void placeMissingNotes(int[] pattern, List<String> missingNotes, Set<String> placedNotes) {
        for (String note : missingNotes) {
            boolean placed = false;
            
            // 미정 상태인 줄들을 순회하면서 가장 낮은 프렛에서 배치
            for (int string = 1; string <= 6; string++) {
                if (pattern[string] == -2) { // 미정 상태인 줄
                    int[] possibleFrets = MusicUtils.findFretsForNote(string, note);
                    for (int fret : possibleFrets) {
                        if (fret > 0 && fret <= 3) { // 1~3프렛 이내
                            pattern[string] = fret;
                            placedNotes.add(note);
                            placed = true;
                            break;
                        }
                    }
                    if (placed) break;
                }
            }
            
            // 미정 상태 현에서 못 찾았을 경우, 개방현으로 이미 정의된 현에서 구성음 찾기
            if (!placed) {
                for (int string = 1; string <= 6; string++) {
                    if (pattern[string] == 0) { // 개방현으로 정의된 줄
                        int[] possibleFrets = MusicUtils.findFretsForNote(string, note);
                        for (int fret : possibleFrets) {
                            if (fret > 0 && fret <= 3) { // 1~3프렛 이내
                                pattern[string] = fret;
                                placedNotes.add(note);
                                placed = true;
                                break;
                            }
                        }
                        if (placed) break;
                    }
                }
            }
        }
    }
    
    /**
     * 4단계: 미정 상태 현 존재 검사
     */
    boolean hasUndefinedStrings(int[] pattern) {
        for (int string = 1; string <= 6; string++) {
            if (pattern[string] == -2) {
                return true;
            }
        }
        return false;
    }
}