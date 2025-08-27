# 피아노 건반 시각화 기능 명세서

## 개요
코드 분석기에 61키 피아노 키보드를 구현하고, 코드 분석 결과를 양손 연주 패턴으로 시각화하는 기능입니다.

## 1. 피아노 키보드 구현

### 1.1 기본 사양
- **건반 수**: 61키 (C1 ~ C6)
- **흰건반**: 36개 (C, D, E, F, G, A, B × 5옥타브 + C6)
- **검은건반**: 25개 (C#, D#, F#, G#, A# × 5옥타브)
- **레이아웃**: 반응형 설계 (% 기반, 픽셀 제거)

### 1.2 구조 설계
#### HTML 구조
```html
<div class="piano-container">
  <div class="piano-keyboard">
    <!-- 흰 건반 레이어 -->
    <div class="white-keys-layer">
      <div class="piano-key white" data-note="C1"></div>
      <!-- ... 36개 흰건반 -->
    </div>
    
    <!-- 검은 건반 레이어 -->
    <div class="black-keys-layer">
      <!-- 스페이서와 검은건반의 패턴 배치 -->
    </div>
  </div>
</div>
```

#### 레이어 시스템
- **흰건반 레이어**: `inline-block` 흐름으로 자연스러운 배치
- **검은건반 레이어**: `absolute` 포지셔닝으로 흰건반 위에 오버레이

### 1.3 수학적 레이아웃 계산

#### 기본 비율
- **흰건반 폭**: `2.78%` (100% ÷ 36개)
- **검은건반 폭**: `2.224%` (흰건반의 4/5)
- **스페이서 폭**: `0.556%` (흰건반의 1/5)

#### 옥타브당 패턴 (20요소)
```
spacer(3) - black - spacer - black - spacer(6) - black - spacer - black - spacer - black - spacer(3)
```

각 옥타브는 정확히 20개 요소로 구성되어 7개 흰건반(19.46%)과 수학적으로 일치합니다.

### 1.4 CSS 구현

#### 흰건반 스타일
```css
.white-keys-layer .piano-key.white {
  width: 2.78%;
  height: 140px;
  display: inline-block;
  background: linear-gradient(to bottom, #ffffff 0%, #f8f8f8 100%);
  border-radius: 0 0 6px 6px;
  border: 1px solid #999;
}
```

#### 검은건반 스타일
```css
.black-keys-layer .piano-key.black {
  width: 2.224%;
  height: 90px;
  display: inline-block;
  background: linear-gradient(to bottom, #2c2c2c 0%, #1a1a1a 100%);
  border-radius: 0 0 4px 4px;
}
```

#### 스페이서 시스템
```css
.black-keys-layer .spacer {
  display: inline-block;
  width: 0.556%;
  height: 1px;
  visibility: hidden;
}
```

## 2. 건반 하이라이트 기능

### 2.1 기능 개요
코드 분석 결과를 받아 피아노 건반에 양손 연주 패턴으로 시각화합니다.

### 2.2 영역 구분
- **왼손 영역**: A1 ~ G#2 (베이스 노트)
- **오른손 영역**: C4 ~ C6 (구성음들, C3 폴백)

### 2.3 색상 구분
- **왼손**: 파란색 (`#4a90e2`)
- **오른손**: 빨간색 (`#e74c3c`)

### 2.4 CSS 하이라이트 스타일

#### 왼손 하이라이트
```css
.piano-key.left-hand {
  background: linear-gradient(to bottom, #4a90e2 0%, #357abd 100%) !important;
  box-shadow: 0 3px 8px rgba(74, 144, 226, 0.4) !important;
  border: 2px solid #2e6da4 !important;
}

.piano-key.left-hand.black {
  background: linear-gradient(to bottom, #2980b9 0%, #1f618d 100%) !important;
}
```

#### 오른손 하이라이트
```css
.piano-key.right-hand {
  background: linear-gradient(to bottom, #e74c3c 0%, #c0392b 100%) !important;
  box-shadow: 0 3px 8px rgba(231, 76, 60, 0.4) !important;
  border: 2px solid #a93226 !important;
}

.piano-key.right-hand.black {
  background: linear-gradient(to bottom, #c0392b 0%, #922b21 100%) !important;
}
```

## 3. 파싱 오류 처리 및 사용자 피드백

### 3.1 unparsedRemainder 기능
코드 파싱 시 인식하지 못한 부분을 사용자에게 알려주는 기능입니다.

#### 3.1.1 백엔드 구현
```java
// ChordParseDetailResult와 ChordParseResult에 필드 추가
private String unparsedRemainder; // 파싱되지 않은 나머지 부분

// parseChordStructure에서 파싱될 때마다 해당 부분 제거
remaining = remaining.replace("dim", "");
remaining = remaining.replace("aug", "");
// ... 각 패턴별로 제거 후 남은 부분 반환
return remaining;
```

#### 3.1.2 프론트엔드 구현
```javascript
// 파싱되지 않은 부분이 있으면 예시 영역에 경고 표시
const exampleText = document.querySelector('small.form-text');
if (exampleText) {
    if (data.unparsedRemainder && data.unparsedRemainder.trim() !== '') {
        exampleText.innerHTML = `<span style="color: #dc3545;"><i class="bi bi-exclamation-triangle"></i> 파싱되지 않은 부분: "${data.unparsedRemainder}"</span>`;
        exampleText.className = 'form-text text-danger';
    } else {
        exampleText.innerHTML = '예시: C, Dm, Gmaj7, Am7, Fsus4, C#dim, Baug, Em7b5, D/F#';
        exampleText.className = 'form-text text-muted';
    }
}
```

#### 3.1.3 사용자 경험
- 파싱 성공시: 기존 예시 텍스트 표시 (회색)
- 파싱 실패 부분 있을시: 빨간색 경고 메시지로 실패 부분 표시
- 사용자가 즉시 어떤 부분이 잘못되었는지 확인 가능

## 4. 파워코드 지원

### 4.1 파워코드 개념
- **정의**: 루트음 + 완전5도만 포함하는 코드 (3도 없음)
- **표기법**: C5, F5, G5 등
- **특징**: 장조/단조 구분이 없어 록/메탈 음악에서 주로 사용

### 4.2 파싱 알고리즘
```java
// parseChordStructure 메소드에서 파워코드 처리
boolean isPowerChord = rest.equals("5");
if (isPowerChord) {
    intervals.add("5"); // 루트(1)와 5도만
    return "";
}
```

### 4.3 지원되는 파워코드 예시
- `C5` → 구성음: C, G
- `F5` → 구성음: F, C
- `G5` → 구성음: G, D
- `A5` → 구성음: A, E
- `D5` → 구성음: D, A
- `E5` → 구성음: E, B

## 5. JavaScript 구현

### 5.1 핵심 함수들

#### 하이라이트 제거
```javascript
function clearPianoHighlight() {
  const allKeys = document.querySelectorAll('.piano-key');
  allKeys.forEach(key => {
    key.classList.remove('left-hand', 'right-hand');
  });
}
```

#### 왼손 하이라이트
```javascript
function highlightLeftHand(bassNote) {
  // 플랫을 샤프로 변환
  function flatToSharp(note) {
    const flatToSharpMap = {
      'Bb': 'A#', 'Db': 'C#', 'Eb': 'D#', 'Gb': 'F#', 'Ab': 'G#'
    };
    return flatToSharpMap[note] || note;
  }
  
  const convertedBassNote = flatToSharp(bassNote);
  const possibleOctaves = ['1', '2'];
  
  // A1~G#2 범위에서 베이스 노트 찾기
  for (const octave of possibleOctaves) {
    const noteWithOctave = convertedBassNote + octave;
    const bassKey = document.querySelector(`.piano-key[data-note="${noteWithOctave}"]`);
    
    if (bassKey) {
      const leftHandRange = ['A1', 'A#1', 'B1', 'C2', 'C#2', 'D2', 'D#2', 'E2', 'F2', 'F#2', 'G2', 'G#2'];
      if (leftHandRange.includes(noteWithOctave)) {
        bassKey.classList.add('left-hand');
        break;
      }
    }
  }
}
```

#### 오른손 하이라이트
```javascript
function highlightRightHand(noteNames) {
  const noteOrder = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];
  let currentOctave = 4;
  let lastNoteIndex = -1;
  
  noteNames.forEach(noteName => {
    const noteIndex = noteOrder.indexOf(noteName);
    
    // 이전 음보다 낮은 음이면 다음 옥타브로
    if (noteIndex <= lastNoteIndex) {
      currentOctave++;
    }
    
    // B5 이후의 모든 음을 C3에 표시
    let octaveToUse = currentOctave > 5 ? 3 : currentOctave;
    
    const noteWithOctave = noteName + octaveToUse;
    const key = document.querySelector(`.piano-key[data-note="${noteWithOctave}"]`);
    
    if (key) {
      key.classList.add('right-hand');
    }
    
    lastNoteIndex = noteIndex;
  });
}
```

### 3.2 통합 함수
```javascript
function displayResult(data) {
  // 기존 UI 업데이트...
  
  // 피아노 건반 하이라이트
  clearPianoHighlight();
  highlightLeftHand(data.bass);
  highlightRightHand(data.noteNames);
  
  document.getElementById('chordResult').style.display = 'block';
}
```

## 4. 알고리즘 설계

### 4.1 음표 배치 알고리즘
1. **시작 옥타브**: C4부터 시작
2. **순차 배치**: 구성음 순서대로 배치
3. **옥타브 진행**: 이전 음보다 낮으면 다음 옥타브로
4. **범위 제한**: C6 초과시 C3으로 폴백

### 4.2 플랫/샤프 변환
서버에서 플랫 표기로 오는 음표를 건반의 샤프 표기로 변환:
- `Bb` → `A#`
- `Db` → `C#`
- `Eb` → `D#`
- `Fb` → `E`
- `Gb` → `F#`
- `Ab` → `G#`
- `Cb` → `B`

**parseRoot 메소드에서의 실시간 변환:**
```java
// 플랫을 샤프으로 변환하여 NOTE_NAMES와 일치시킴
if (root.contains("b")) {
    Map<String, String> flatToSharp = Map.of(
        "Db", "C#", "Eb", "D#", "Fb", "E",
        "Gb", "F#", "Ab", "G#", "Bb", "A#", "Cb", "B"
    );
    return flatToSharp.getOrDefault(root, root);
}
```

## 6. 사용 예시

### 6.1 G 메이저 코드
- **입력**: `G`
- **구성음**: `[G, B, D]`
- **베이스**: `G`
- **결과**:
  - 왼손: `G2` (파란색)
  - 오른손: `G4, B4, D5` (빨간색)

### 6.2 Dm7 코드
- **입력**: `Dm7`
- **구성음**: `[D, F, A, C]`
- **베이스**: `D`
- **결과**:
  - 왼손: `D2` (파란색)
  - 오른손: `D4, F4, A4, C5` (빨간색)

### 6.3 파워코드 (G5)
- **입력**: `G5`
- **구성음**: `[G, D]` (루트 + 완전5도만)
- **베이스**: `G`
- **특징**: 3도가 없어 장조/단조 구분 없음
- **결과**:
  - 왼손: `G2` (파란색)
  - 오른손: `G4, D5` (빨간색)

## 7. 파일 구조

### 7.1 관련 파일
- **HTML**: `/src/main/resources/templates/chord.html`
- **CSS**: `/src/main/resources/static/css/piano.css`
- **JavaScript**: `chord.html` 내부 스크립트

### 7.2 데이터 흐름
1. 사용자 코드 입력
2. 서버 코드 분석 (`/chord/api/parse`)
3. JSON 응답 수신 (`{noteNames, bass, ...}`)
4. 피아노 건반 하이라이트 실행

## 8. 기술적 특징

### 8.1 반응형 설계
- 모든 치수를 % 단위로 구현
- 컨테이너 크기에 따른 자동 조절
- 모바일/데스크톱 호환

### 8.2 성능 최적화
- CSS 클래스 기반 하이라이트
- DOM 쿼리 최소화
- 효율적인 레이어 구조

### 8.3 확장성
- 새로운 하이라이트 색상 추가 용이
- 음역 범위 변경 가능
- 다양한 코드 타입 지원

## 9. 개발 과정 요약

1. **기존 픽셀 기반 → % 기반 변환**
2. **61키 건반 구현** (C1~C6)
3. **레이어 구조 도입** (흰건반/검은건반 분리)
4. **스페이서 시스템 개발** (수학적 정렬)
5. **하이라이트 기능 구현** (양손 구분)
6. **플랫/샤프 변환 처리** (Fb→E, Cb→B 포함)
7. **파워코드 지원 추가** (C5, F5 등)
8. **unparsedRemainder 기능 구현** (파싱 오류 피드백)
9. **범위 초과 폴백 로직**

이 구현을 통해 사용자는 코드 분석 결과를 직관적인 피아노 건반 시각화로 확인할 수 있습니다.