package org.silkroadpartnership.theway_noti.chord.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.silkroadpartnership.theway_noti.chord.entity.ChordParseDetailResult;
import org.springframework.stereotype.Component;

@Component
public class ChordParser {

    private static final Map<String, Integer> NOTE_TO_PITCH_CLASS = Map.of(
        "C", 0, "D", 2, "E", 4, "F", 5, "G", 7, "A", 9, "B", 11
    );

    private static final Map<String, Integer> INTERVAL_TO_SEMITONE = Map.ofEntries(
        Map.entry("1", 0), Map.entry("b2", 1), Map.entry("2", 2), Map.entry("b3", 3), 
        Map.entry("3", 4), Map.entry("4", 5), Map.entry("#4", 6), Map.entry("b5", 6), 
        Map.entry("5", 7), Map.entry("#5", 8), Map.entry("b6", 8), Map.entry("6", 9),
        Map.entry("b7", 10), Map.entry("7", 11), Map.entry("9", 2), Map.entry("b9", 1), 
        Map.entry("#9", 3), Map.entry("11", 5), Map.entry("#11", 6), Map.entry("13", 9), 
        Map.entry("b13", 8), Map.entry("bb7", 9)
    );

    private static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    public ChordParseDetailResult parseChord(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Chord symbol cannot be empty");
        }

        String normalized = normalize(symbol);
        String[] parts = splitOnSlash(normalized);
        String core = parts[0];
        String bass = parts.length > 1 ? parts[1] : null;

        String root = parseRoot(core);
        String rest = core.substring(root.length());

        Set<String> intervals = new LinkedHashSet<>();
        String unparsedRemainder = parseChordStructure(rest, intervals);

        List<Integer> semitones = calculateSemitones(root, intervals);
        List<String> noteNames = calculateNoteNames(root, intervals);

        return ChordParseDetailResult.builder()
                .originalSymbol(symbol)
                .root(root)
                .bass(bass)  // 분수코드인 경우만 값이 있음, 일반 코드는 null
                .intervals(intervals)
                .semitones(semitones)
                .noteNames(noteNames)
                .unparsedRemainder(unparsedRemainder.isEmpty() ? null : unparsedRemainder)
                .build();
    }

    private String normalize(String symbol) {
        String normalized = symbol.trim()
                .replace("Δ", "maj")
                .replace("M", "maj")
                .replace("°", "dim")
                .replace("ø", "m7b5")
                .replace("+", "aug")
                .replace("-", "m")
                .replaceAll("\\s+", "");
        
        // 괄호 내용을 별도로 처리
        return processParentheses(normalized);
    }
    
    private String processParentheses(String symbol) {
        // m(maj7) 패턴 처리
        symbol = symbol.replaceAll("m\\(maj7\\)", "mmaj7");
        // m(add9) 등 다른 패턴들도 처리
        symbol = symbol.replaceAll("m\\(([^)]+)\\)", "m$1");
        
        // 일반적인 괄호 제거 (위에서 처리되지 않은 경우)
        return symbol.replaceAll("[()]", "");
    }

    private String[] splitOnSlash(String symbol) {
        return symbol.split("/", 2);
    }

    private String parseRoot(String core) {
        Pattern rootPattern = Pattern.compile("^([A-G][#b]?)");
        Matcher matcher = rootPattern.matcher(core);
        if (matcher.find()) {
            String root = matcher.group(1);
            
            // 플랫을 샵으로 변환하여 NOTE_NAMES와 일치시킴
            if (root.contains("b")) {
                Map<String, String> flatToSharp = Map.of(
                    "Db", "C#",
                    "Eb", "D#", 
                    "Fb", "E",
                    "Gb", "F#",
                    "Ab", "G#",
                    "Bb", "A#",
                    "Cb", "B"
                );
                return flatToSharp.getOrDefault(root, root);
            }
            
            return root;
        }
        throw new IllegalArgumentException("Invalid root note: " + core);
    }

    private String parseChordStructure(String rest, Set<String> intervals) {
        intervals.add("1");
        String remaining = rest;

        if (rest.isEmpty()) {
            intervals.addAll(Arrays.asList("3", "5"));
            return "";
        }

        // 파워코드 처리 (C5, F5 등)
        boolean isPowerChord = rest.equals("5");
        if (isPowerChord) {
            intervals.add("5"); // 루트(1)와 5도만
            return "";
        }

        boolean isMinor = rest.contains("m") && !rest.startsWith("maj");
        boolean isDim = rest.contains("dim");
        boolean isAug = rest.contains("aug");
        boolean hasSus2 = rest.contains("sus2");
        boolean hasSus4 = rest.contains("sus4") || (rest.contains("sus") && !rest.contains("sus2"));

        if (isDim) {
            intervals.addAll(Arrays.asList("b3", "b5"));
            remaining = remaining.replace("dim", "");
            if (rest.contains("7")) {
                intervals.add("bb7");
            }
        } else if (isAug) {
            intervals.addAll(Arrays.asList("3", "#5"));
            remaining = remaining.replace("aug", "");
        } else if (isMinor) {
            intervals.addAll(Arrays.asList("b3", "5"));
            remaining = remaining.replaceFirst("m(?!aj)", "");
        } else {
            intervals.addAll(Arrays.asList("3", "5"));
        }

        if (hasSus2) {
            intervals.remove("3");
            intervals.remove("b3");
            intervals.add("2");
            remaining = remaining.replace("sus2", "");
        } else if (hasSus4) {
            intervals.remove("3");
            intervals.remove("b3");
            intervals.add("4");
            remaining = remaining.replaceAll("sus4?", "");
        }

        if (rest.contains("6") && !rest.contains("6/9")) {
            intervals.add("6");
            remaining = remaining.replace("6", "");
        } else if (rest.contains("6/9")) {
            intervals.addAll(Arrays.asList("6", "9"));
            remaining = remaining.replace("6/9", "");
        }

        if (rest.contains("maj7")) {
            intervals.add("7");
            remaining = remaining.replace("maj7", "");
        } else if (rest.contains("7") && !rest.contains("maj7")) {
            intervals.add("b7");
            remaining = remaining.replace("7", "");
        }

        Pattern extensionPattern = Pattern.compile("(?:add)?(9|11|13)");
        Matcher extensionMatcher = extensionPattern.matcher(rest);
        while (extensionMatcher.find()) {
            String extension = extensionMatcher.group(1);
            boolean isAdd = rest.substring(Math.max(0, extensionMatcher.start() - 3), extensionMatcher.start()).contains("add");
            
            if (!isAdd && !intervals.contains("b7") && !intervals.contains("7")) {
                if (isMinor || rest.contains("m")) {
                    intervals.add("b7");
                } else {
                    intervals.add("b7");
                }
            }
            intervals.add(extension);
            remaining = remaining.replace(extensionMatcher.group(0), "");
        }

        Pattern alterPattern = Pattern.compile("([b#])(9|5|11|13)");
        Matcher alterMatcher = alterPattern.matcher(rest);
        while (alterMatcher.find()) {
            String alteration = alterMatcher.group(1) + alterMatcher.group(2);
            String natural = alterMatcher.group(2);
            intervals.remove(natural);
            intervals.add(alteration);
            remaining = remaining.replace(alterMatcher.group(0), "");
        }

        Pattern noPattern = Pattern.compile("no([35])");
        Matcher noMatcher = noPattern.matcher(rest);
        while (noMatcher.find()) {
            String degree = noMatcher.group(1);
            intervals.remove(degree);
            if (degree.equals("3")) {
                intervals.remove("b3");
            }
            remaining = remaining.replace(noMatcher.group(0), "");
        }
        
        return remaining;
    }

    private List<Integer> calculateSemitones(String root, Set<String> intervals) {
        int rootPc = getRootPitchClass(root);
        List<Integer> semitones = new ArrayList<>();

        for (String interval : intervals) {
            Integer offset = INTERVAL_TO_SEMITONE.get(interval);
            if (offset != null) {
                semitones.add((rootPc + offset) % 12);
            }
        }

        return semitones.stream().distinct().sorted().toList();
    }

    private List<String> calculateNoteNames(String root, Set<String> intervals) {
        int rootPc = getRootPitchClass(root);
        List<String> notes = new ArrayList<>();

        // 순서대로 정렬하기 위해 intervals를 정렬
        List<String> sortedIntervals = intervals.stream()
            .sorted(this::compareIntervals)
            .toList();

        for (String interval : sortedIntervals) {
            Integer offset = INTERVAL_TO_SEMITONE.get(interval);
            if (offset != null) {
                int notePc = (rootPc + offset) % 12;
                notes.add(NOTE_NAMES[notePc]);
            }
        }

        return notes.stream().distinct().toList();
    }

    private int compareIntervals(String a, String b) {
        // 인터벌을 숫자 순서로 정렬하기 위한 우선순위 매핑
        Map<String, Integer> intervalOrder = Map.ofEntries(
            Map.entry("1", 1), Map.entry("b2", 2), Map.entry("2", 3), Map.entry("b3", 4), 
            Map.entry("3", 5), Map.entry("4", 6), Map.entry("#4", 7), Map.entry("b5", 8), 
            Map.entry("5", 9), Map.entry("#5", 10), Map.entry("b6", 11), Map.entry("6", 12),
            Map.entry("bb7", 13), Map.entry("b7", 14), Map.entry("7", 15), Map.entry("b9", 16), 
            Map.entry("9", 17), Map.entry("#9", 18), Map.entry("11", 19), Map.entry("#11", 20), 
            Map.entry("b13", 21), Map.entry("13", 22)
        );
        
        Integer orderA = intervalOrder.get(a);
        Integer orderB = intervalOrder.get(b);
        
        if (orderA == null) orderA = 999;
        if (orderB == null) orderB = 999;
        
        return Integer.compare(orderA, orderB);
    }

    private int getRootPitchClass(String root) {
        String noteName = root.substring(0, 1);
        int pc = NOTE_TO_PITCH_CLASS.get(noteName);

        if (root.length() > 1) {
            String accidental = root.substring(1);
            if (accidental.equals("#")) {
                pc = (pc + 1) % 12;
            } else if (accidental.equals("b")) {
                pc = (pc - 1 + 12) % 12;
            }
        }

        return pc;
    }
}