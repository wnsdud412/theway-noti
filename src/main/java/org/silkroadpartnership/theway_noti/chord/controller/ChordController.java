package org.silkroadpartnership.theway_noti.chord.controller;

import org.silkroadpartnership.theway_noti.chord.entity.ChordParseResult;
import org.silkroadpartnership.theway_noti.chord.entity.GuitarFingeringResult;
import org.silkroadpartnership.theway_noti.chord.service.ChordService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/chord")
@RequiredArgsConstructor
@Slf4j
public class ChordController {

    private final ChordService chordService;

    @GetMapping
    public String chordPage() {
        return "chord";
    }

    @PostMapping("/api/parse")
    @ResponseBody
    public ResponseEntity<ChordParseResult> parseChord(@RequestBody String symbol) {
        try {
            // JSON으로 전송된 문자열에서 따옴표 제거
            String cleanSymbol = symbol.replaceAll("^\"|\"$", "").trim();
            ChordParseResult result = chordService.parseChordSymbol(cleanSymbol);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid chord symbol: {}", symbol, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error parsing chord: {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/fingering")
    @ResponseBody
    public ResponseEntity<GuitarFingeringResult> getGuitarFingering(@RequestBody ChordParseResult chordResult) {
        try {
            log.debug("Getting guitar fingering for chord: {}", chordResult.getOriginalSymbol());
            GuitarFingeringResult result = chordService.getGuitarFingerings(chordResult);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid chord parse result: {}", chordResult.getOriginalSymbol(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error getting guitar fingering: {}", chordResult.getOriginalSymbol(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}