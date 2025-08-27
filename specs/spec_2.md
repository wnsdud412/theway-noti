# 기타 코드 운지법 추천 기능

## 개요
기존 코드 분석기에 추가하여 화음 기호를 입력하면 기타 운지법을 시각적으로 표시하는 기능

## 전제 조건

### 기타 줄 번호 체계
- **1번줄 (가장 얇은 줄)**: e (높은 미)
- **2번줄**: B (시)
- **3번줄**: G (솔)
- **4번줄**: D (레)
- **5번줄**: A (라)  
- **6번줄 (가장 굵은 줄)**: E (낮은 미)

### 데이터 표현 방식
- 배열 인덱스 0 = 1번줄(e), 인덱스 5 = 6번줄(E)
- 모든 코드 연주법과 손가락 프렛 관리는 1번줄부터 순서대로 처리
- 튜닝 순서: e-B-G-D-A-E

### 현 표현 용어 정의
- **높은현**: 6번줄에 가까운 줄 (굵은 줄, 낮은 음을 연주)
- **낮은현**: 1번줄에 가까운 줄 (얇은 줄, 높은 음을 연주)

### 각 줄의 0~4프렛 구성음 참조
```
1번줄 (e): e - f - f# - g - g#
2번줄 (B): B - c - c# - d - d#  
3번줄 (G): G - g# - a - a# - b
4번줄 (D): D - d# - e - f - f#
5번줄 (A): A - a# - b - c - c#
6번줄 (E): E - f - f# - g - g#
```

## ChordParser 개선 사항 (추가 작업)

### 문제점
- 현재 `ChordParser.parseChord()`에서 root와 bass를 구분하지 않고 합쳐서 반환
- 피아노 건반 표시에는 적합하지만, 기타 연주 계산에는 root와 bass 구분 필요

### 해결방안

#### 1. 새로운 Entity 클래스 생성
```java
// 새로운 클래스: ChordParseDetailResult
private String originalSymbol;
private String root;       // 실제 루트음
private String bass;       // 베이스음 (분수코드의 경우만, null 가능)
private Set<String> intervals;
private List<Integer> semitones;
private List<String> noteNames;
private String unparsedRemainder; // 파싱되지 않은 나머지 부분

// 기존 클래스: ChordParseResult (호환성 유지하며 확장)
private String originalSymbol;
private String bass;       // root+bass 합쳐진 값 (기존 동작)
private Set<String> intervals;
private List<Integer> semitones;
private List<String> noteNames;
private String unparsedRemainder; // 파싱되지 않은 나머지 부분
```

#### 2. ChordParser.parseChord() 수정
```java
// 기존: ChordParseResult parseChord(String symbol)
// 수정: ChordParseDetailResult parseChord(String symbol)

return ChordParseDetailResult.builder()
    .originalSymbol(symbol)
    .root(root)              // 실제 루트음
    .bass(bass)              // 분수코드 베이스음 (null 가능)
    .intervals(intervals)
    .semitones(semitones)
    .noteNames(noteNames)
    .unparsedRemainder(unparsedRemainder.isEmpty() ? null : unparsedRemainder)
    .build();
```

#### 3. ChordService 호환성 유지
```java
public ChordParseResult parseChordSymbol(String symbol) {
    ChordParseDetailResult detailResult = chordParser.parseChord(symbol);
    
    // 기존 클라이언트 호환성: bass 필드에 root+bass 합친 로직
    String compatibleBass = detailResult.getBass() != null ? 
        detailResult.getBass() : detailResult.getRoot();
    
    return ChordParseResult.builder()
        .originalSymbol(detailResult.getOriginalSymbol())
        .bass(compatibleBass)  // 기존과 동일한 로직
        .intervals(detailResult.getIntervals())
        .semitones(detailResult.getSemitones())
        .noteNames(detailResult.getNoteNames())
        .unparsedRemainder(detailResult.getUnparsedRemainder())
        .build();
}
```

#### 4. 분수코드 판별 조건
```java
// 기타 연주 계산 시 분수코드 처리 필요 여부 판별
ChordParseDetailResult detailResult = chordParser.parseChord(symbol);

boolean needsSlashChordProcessing = (detailResult.getBass() != null) && 
                                   (!detailResult.getBass().equals(detailResult.getRoot()));

if (needsSlashChordProcessing) {
    // 분수코드 처리 알고리즘 적용
} else {
    // 일반 코드 처리 알고리즘
}
```

## 오픈코드 찾기 알고리즘

### 1. 루트음 배치 알고리즘

#### 탐색 순서 (4,5,6번줄에서 0~4프렛)
1. **6번줄 우선 탐색**: E - F - F# - G - G#
2. **5번줄 차순위**: A - A# - B - C - C#  
3. **4번줄 최후**: D - D# - E - F - F#

#### 베이스 규칙 및 뮤트 제약사항
- **기본 원칙**: 루트음보다 더 높은현(굵은 줄)에서는 더 낮은 음을 연주하면 안됨
- **뮤트 가능 범위**: 6번줄만 또는 5,6번줄 동시 뮤트만 가능
- **오픈코드 불가능 조건**: 4~1번줄에서 뮤트가 필요한 경우

**허용되는 뮤트 패턴:**
- 6번줄에서 루트음 연주: 뮤트 없음
- 5번줄에서 루트음 연주: 6번줄만 뮤트 가능  
- 4번줄에서 루트음 연주: 5,6번줄 동시 뮤트 가능

#### 알고리즘 흐름
```
for 줄 in [6, 5, 4]:
    for 프렛 in [0, 1, 2, 3, 4]:
        if 해당위치음 == 루트음:
            루트줄 = 줄
            루트프렛 = 프렛
            
            // 뮤트 제약사항 검증
            if 루트줄 == 6:
                // 6번줄 루트: 뮤트 없음
                break
            elif 루트줄 == 5:
                // 5번줄 루트: 6번줄만 뮤트
                줄[6] = -1
                break
            elif 루트줄 == 4:
                // 4번줄 루트: 5,6번줄 뮤트
                줄[5] = -1
                줄[6] = -1
                break
    
    if 루트줄 찾음:
        break

// 루트음을 찾지 못한 경우 오픈코드 불가능
if 루트줄 없음:
    return null
```

### 2. 구성음 배치 알고리즘

#### 루트음 배치 완료 후 진행
```
for 각_구성음 in 남은_구성음:
    found = false
    for 줄 in 사용가능한_줄들:
        for 프렛 in [0,1,2,3,4]:
            if 해당위치음 == 구성음:
                패턴[줄] = 프렛
                found = true
                break
        if found: break
    
    if not found:
        return null  // 해당 루트 위치로는 오픈코드 불가능
```

#### 배치 우선순위
1. 개방현(0프렛) 최우선
2. 낮은 프렛 번호 우선
3. 물리적 손가락 배치 가능성 검증

#### 물리적 제약 검증
```java
// 사용된 프렛들의 범위가 4프렛 이내인지 확인
사용된프렛들 = 0이 아닌 프렛들
최대프렛 - 최소프렛 <= 3  // 손가락 스팬 제한
```

### 3. 완전한 예시
```
C코드 계산 과정:
1. 루트음 C를 5번줄 3프렛에 배치 → [?, ?, ?, ?, 3, -1]
2. 구성음 E: 1번줄 0프렛 (개방현) → [0, ?, ?, ?, 3, -1]
3. 구성음 G: 3번줄 0프렛 (개방현) → [0, ?, 0, ?, 3, -1]
4. 추가 C: 2번줄 1프렛 → [0, 1, 0, ?, 3, -1]
5. 4번줄: 구성음 추가 또는 뮤트 → [0, 1, 0, 2, 3, -1]
최종 결과: [0, 1, 0, 2, 3, -1]
```

### 4. 분수 코드 처리 알고리즘

#### 분수 코드 개념
- **정의**: 루트음과 베이스음이 다른 코드 (예: C/E, G/B)
- **원칙**: 베이스음이 가장 낮은음(가장 높은현)에 위치해야 함
- **조건**: `bass != root` 인 경우에만 적용

#### 방법 1: 뮤트현 활용
```
// 이미 뮤트된 현에서 베이스음 연주 시도
for 뮤트된현 in 뮤트현리스트:
    for 프렛 in [0, 1, 2, 3, 4]:
        if 해당위치음 == 베이스음:
            뮤트된현[프렛] = 베이스음_프렛
            return 성공

예: C/E 코드
- 루트 C를 5번줄 3프렛 배치 → 6번줄 뮤트
- 베이스 E를 6번줄 0프렛(개방현)에서 연주 가능
- 결과: 6번줄 뮤트 해제하고 E 연주
```

#### 방법 2: 루트음 재배치 (기존 연주법 보존)
```
for 현 in [6, 5, 4, 3, 2, 1]:
    if 베이스음을 해당현에서 연주가능:
        베이스현 = 현
        
        // 핵심: 기존 배치에서 루트음 커버 확인
        루트음_이미_커버됨 = false
        for 기존_연주현 in 베이스현보다_낮은현들:
            if 기존_패턴[기존_연주현] != -1:  // 이미 연주중
                해당현_연주음 = 계산된_음(기존_연주현, 기존_패턴[기존_연주현])
                if 해당현_연주음 == 루트음:
                    루트음_이미_커버됨 = true
                    break
        
        if not 루트음_이미_커버됨:
            continue  // 이 베이스 위치는 불가능
        
        // 베이스현보다 높은현들 뮤트
        for i in range(베이스현+1, 7):
            줄[i] = -1
        줄[베이스현] = 베이스음_프렛
        return 성공

예: G/B 코드
- 기존: 루트 G를 6번줄, 구성음들을 1,2,3번줄에 배치
- 베이스 B를 5번줄 2프렛에 배치 시도
- 확인: 기존 3번줄 0프렛에서 이미 G(루트음) 연주중
- 결과: 6번줄 뮤트, 5번줄에 B 배치
```

### 5. 실패 시 대안
- 다른 루트음 위치 시도 (6번줄 → 5번줄 → 4번줄)
- 구성음 배치 시 4~1번줄에서 뮤트 필요한 경우 오픈코드 불가능 판정
- 분수 코드에서 두 방법 모두 실패시 오픈코드 불가능 판정
- 모든 루트 위치에서 실패시 오픈코드 불가능 판정
- **오픈코드 실패시**: 바레코드 알고리즘으로 연결

## 바레코드 찾기 알고리즘

### 개요
- **적용 조건**: 오픈코드 알고리즘 실패시 대안으로 사용
- **CAGED 시스템**: A 셰이프와 E 셰이프만 활용
- **선택 기준**: 루트음 범위에 따른 셰이프 자동 선택

### 셰이프 선택 기준
- **F~G# 루트음**: E 셰이프 사용 (6번줄 루트)
- **A#~D# 루트음**: A 셰이프 사용 (5번줄 루트)
- **기타 루트음**: 가장 가까운 범위의 셰이프 선택

### 평행이동 알고리즘

#### 1단계: 구성음 평행이동 (루트현 개방음 기준)
```
offset = 루트현_개방음 - 목표_루트음

E 셰이프: offset = E(0) - 목표_루트음
A 셰이프: offset = A(9) - 목표_루트음

// 모든 구성음을 offset만큼 평행이동
for 구성음 in 코드_구성음들:
    이동된_구성음 = (구성음 + offset) % 12
```

#### 2단계: 이동된 구성음으로 연주 패턴 찾기
```
// 선택된 셰이프의 루트현에 루트음 배치
루트현[루트프렛] = 이동된_루트음

// 나머지 현들에서 이동된 구성음들 찾아 배치
for 현 in 사용가능한_현들:
    for 프렛 in [0, 1, 2, 3, 4]:
        해당위치음 = 계산된_음(현, 프렛)
        if 해당위치음 in 이동된_구성음들:
            패턴[현] = 프렛

// 바레 적용: 최소 프렛을 바레 프렛으로 설정
바레프렛 = min(패턴에서_0이_아닌_값들)
```

#### 3단계: 연주 패턴 역평행이동
```
// 찾은 패턴을 원래 위치로 역이동
for 현 in range(6):
    if 패턴[현] != -1:
        최종패턴[현] = 패턴[현] - 바레프렛 + 목표바레프렛

목표바레프렛 = 바레프렛 - offset
```

### 바레코드 계산 예시

#### 예시 1: F 메이저 → E 셰이프
```
반음계 번호: C=0, C#=1, D=2, D#=3, E=4, F=5, F#=6, G=7, G#=8, A=9, A#=10, B=11

1단계: 구성음 평행이동
- F 메이저 구성음: F(5), A(9), C(0)
- offset = E(4) - F(5) = -1
- 이동된 구성음: F(5)→E(4), A(9)→G#(8), C(0)→B(11)

2단계: 이동된 구성음으로 패턴 찾기 (E, G#, B로 패턴 생성)
- 6번줄 0프렛: E (루트)
- 4번줄 2프렛: E 
- 3번줄 1프렛: G#
- 2번줄 0프렛: B
- 1번줄 0프렛: E (옥타브)
- 바레프렛 = 0 (개방현들이 있으므로)

3단계: 역평행이동 (+1 반음)
- 모든 프렛에 +1 적용 (F 위치로 이동)
- 6번줄: 0→1프렛 (F), 4번줄: 2→3프렛, 3번줄: 1→2프렛
- 2번줄: 0→1프렛, 1번줄: 0→1프렛
- 최종 패턴: [1, 1, 2, 3, 1, 1] (바레코드)
```

#### 예시 2: Bb 메이저 → A 셰이프  
```
1단계: 구성음 평행이동
- Bb 메이저 구성음: Bb(10), D(2), F(5)
- offset = A(9) - Bb(10) = -1
- 이동된 구성음: Bb(10)→A(9), D(2)→C#(1), F(5)→E(4)

2단계: 이동된 구성음으로 패턴 찾기 (A, C#, E로 패턴 생성)
- 5번줄 0프렛: A (루트)
- 4번줄 2프렛: E
- 3번줄 2프렛: A (옥타브)
- 2번줄 2프렛: C#
- 1번줄 0프렛: E (옥타브)
- 6번줄: 뮤트 (-1)
- 바레프렛 = 0 (개방현 존재)

3단계: 역평행이동 (+1 반음)
- 모든 프렛에 +1 적용 (Bb 위치로 이동)
- 5번줄: 0→1프렛 (Bb), 4번줄: 2→3프렛, 3번줄: 2→3프렛
- 2번줄: 2→3프렛, 1번줄: 0→1프렛
- 최종 패턴: [1, 3, 3, 3, 1, -1] (A 셰이프 바레코드)
```

### 바레코드 제약사항 및 검증

#### 물리적 제약사항
- **3프렛 스팬 제한**: 바레프렛 + 3프렛 이내에서만 연주 가능
- **바레 가능성**: 모든 현에서 바레프렛 이상의 프렛 사용
- **손가락 배치**: 실제 연주 가능한 운지법인지 검증

#### 검증 과정
```
// 물리적 제약 검증
최대프렛 = max(패턴에서_바레프렛이_아닌_값들)
if 최대프렛 - 바레프렛 > 3:
    return 실패  // 손가락 스팬 초과

// 바레 일관성 검증  
for 현 in range(6):
    if 패턴[현] != -1 and 패턴[현] < 바레프렛:
        return 실패  // 바레 규칙 위반
```

#### 실패시 처리 방안
1. **다른 셰이프 시도**: E 셰이프 실패시 A 셰이프로 재시도
2. **바레 위치 조정**: 더 높은 프렛에서 바레코드 시도
3. **최종 실패**: 모든 방법 실패시 "연주 불가능" 반환

#### 우선순위 정책
- **연주 용이성**: 낮은 프렛 위치 우선
- **음향 품질**: 더 많은 구성음 포함 우선  
- **일반성**: 일반적으로 사용되는 패턴 우선

## API 설계

### 기존 API
```
POST /chord/api/parse (화음 분석)
Request: String (코드 기호)
Response: ChordParseResult {
    originalSymbol: String,
    bass: String,
    intervals: Set<String>,
    semitones: List<Integer>,
    noteNames: List<String>,
    unparsedRemainder: String
}

POST /chord/api/fingering (운지법 조회)
Request: ChordParseResult
Response: GuitarFingeringResult {
    chord: String,
    patterns: List<GuitarFingering>
}

GET /chord (웹 페이지)
Response: HTML 페이지
```

## 프론트엔드 고급 기능

### 1. 동적 프렛 범위 표시
```javascript
// 운지법에 따라 프렛보드 범위 자동 조정 (0~12프렛)
function calculateOptimalFretRange(frets) {
    const activeFrets = frets.filter(f => f > 0);
    if (activeFrets.length === 0) return [0, 5];
    
    const minFret = Math.min(...activeFrets);
    const maxFret = Math.max(...activeFrets);
    let startFret = Math.max(0, minFret - 2);
    let endFret = startFret + 5;
    
    if (maxFret > endFret) {
        endFret = maxFret;
        startFret = Math.max(0, endFret - 5);
    }
    return [startFret, endFret];
}

// 프렛 범위 표시 업데이트
function updateFretboardRange(startFret, endFret) {
    const rangeIndicator = document.getElementById('fret-range-indicator');
    if (startFret === 0) {
        rangeIndicator.textContent = `(Open - ${endFret}프렛)`;
    } else {
        rangeIndicator.textContent = `(${startFret+1}-${endFret}프렛)`;
    }
}
```

### 2. unparsedRemainder 경고 표시
```javascript
// 파싱되지 않은 부분이 있으면 입력 예시 영역에 경고 표시
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

### 3. 오디오 재생 기능 (Tone.js)
```javascript
// 아르페지오 → 화음 순서로 재생
async function playChord(noteNames, bassNote) {
    if (!synth || Tone.context.state !== 'running') {
        await Tone.start();
    }
    
    const allNotes = bassNote && !noteNames.includes(bassNote) 
        ? [bassNote, ...noteNames] : [...noteNames];
    const notesWithOctave = allNotes.map(note => note + '4');
    
    // 1. 아르페지오 재생 (각 음을 0.3초씩 순차적으로)
    for (let i = 0; i < notesWithOctave.length; i++) {
        synth.triggerAttackRelease(notesWithOctave[i], '0.5', Tone.now() + i * 0.3);
    }
    
    // 2. 아르페지오 완료 후 화음 재생
    const chordDelay = notesWithOctave.length * 0.3 + 0.3;
    synth.triggerAttackRelease(notesWithOctave, '2', Tone.now() + chordDelay);
}
```

### 4. 실시간 UI 업데이트
```javascript
// 입력과 동시에 Enter 키 지원
document.getElementById('chordSymbol').addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        parseChord();
    }
});

// CSRF 토큰을 이용한 보안 통신
const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

fetch('/chord/api/parse', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        [header]: token
    },
    body: JSON.stringify(symbol)
});
```

### 5. 반응형 시각화
- **피아노 건반**: 61키 완전 지원, 모바일 스크롤 가능
- **기타 프렛보드**: 동적 프렛 범위 조정으로 모든 운지법 표시 가능
- **에러 처리**: 친화적인 에러 메시지와 시각적 피드백

## 화음기호 예시

### 오픈코드

- A : [0, 2, 2, 2, 0,-1]
- C : [0, 1, 0, 2, 3,-1]
- D : [3, 2, 3, 0,-1,-1]
- G : [3, 0, 0, 0, 2, 3]
- E : [0, 0, 1, 2, 2, 0]

### 바레코드

- B : [2, 4, 4, 4, 2, 2]
- F : [1, 1, 2, 3, 3, 1]

e-B-G-D-A-E