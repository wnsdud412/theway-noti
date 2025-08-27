function parseChord() {
  const symbol = document.getElementById('chordSymbol').value.trim();
  const resultDiv = document.getElementById('chordResult');
  const errorDiv = document.getElementById('errorMessage');
  
  // 초기화
  resultDiv.style.display = 'none';
  errorDiv.style.display = 'none';
  
  if (!symbol) {
    showError('코드 기호를 입력해주세요.');
    return;
  }

  // CSRF 토큰 가져오기
  const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
  const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

  fetch('/chord/api/parse', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [header]: token
    },
    body: JSON.stringify(symbol)
  })
  .then(response => {
    if (!response.ok) {
      throw new Error('유효하지 않은 코드 기호입니다.');
    }
    return response.json();
  })
  .then(data => {
    displayResult(data);
    getGuitarFingering(data);
  })
  .catch(error => {
    console.error('Error:', error);
    showError(error.message || '코드 분석 중 오류가 발생했습니다.');
  });
}

function displayResult(data) {
  // 현재 코드 데이터 저장
  currentChordData = data;
  
  document.getElementById('originalSymbol').textContent = data.originalSymbol;
  document.getElementById('bassNote').textContent = data.bass || '없음';
  
  // 구성음 표시
  const notesDiv = document.getElementById('noteNames');
  notesDiv.innerHTML = '';
  data.noteNames.forEach(note => {
    const span = document.createElement('span');
    span.className = 'note-display';
    span.textContent = note;
    notesDiv.appendChild(span);
  });
  
  // 음정 표시
  const intervalsDiv = document.getElementById('intervals');
  intervalsDiv.innerHTML = '';
  data.intervals.forEach(interval => {
    const span = document.createElement('span');
    span.className = 'interval-display';
    span.textContent = interval;
    intervalsDiv.appendChild(span);
  });
  
  // 반음 간격 표시
  document.getElementById('semitones').textContent = data.semitones.join(', ');
  
  // 파싱되지 않은 부분이 있으면 예시 영역에 경고 표시
  const exampleText = document.querySelector('.form-text.text-muted');
  if (data.unparsedRemainder && data.unparsedRemainder.trim() !== '') {
    exampleText.innerHTML = `<span style="color: #dc3545;"><i class="bi bi-exclamation-triangle"></i> 파싱되지 않은 부분: "${data.unparsedRemainder}"</span>`;
  } else {
    exampleText.innerHTML = '예시: C, Dm, Gmaj7, Am7, Fsus4, C#dim, Baug, Em7b5, D/F#';
  }
  
  // 피아노 건반 하이라이트
  clearPianoHighlight();
  highlightLeftHand(data.bass);
  highlightRightHand(data.noteNames);
  
  // 기타 프렛보드 초기화
  clearGuitarHighlight();
  
  // 재생 버튼 활성화
  const playBtn = document.getElementById('playChordBtn');
  if (playBtn) {
    playBtn.disabled = false;
  }
  
  document.getElementById('chordResult').style.display = 'block';
}

function showError(message) {
  const errorDiv = document.getElementById('errorMessage');
  errorDiv.textContent = message;
  errorDiv.style.display = 'block';
  
  // 현재 코드 데이터 초기화
  currentChordData = null;
  
  // 재생 버튼 비활성화
  const playBtn = document.getElementById('playChordBtn');
  if (playBtn) {
    playBtn.disabled = true;
  }
  
  // 에러 시에도 하이라이트 제거
  clearPianoHighlight();
  initializeFretboard();
}

function getGuitarFingering(chordData) {
  
  // CSRF 토큰 가져오기
  const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
  const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

  fetch('/chord/api/fingering', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [header]: token
    },
    body: JSON.stringify(chordData)
  })
  .then(response => {
    if (!response.ok) {
      throw new Error('운지법 조회 실패');
    }
    return response.json();
  })
  .then(fingeringData => {
    
    if (fingeringData.patterns.length > 0) {
      const pattern = fingeringData.patterns[0];
      
      // 프렛보드에 운지법 시각화
      displayGuitarFingering(fingeringData);
    } else {
    }
  })
  .catch(error => {
    console.error('기타 운지법 API 에러:', error);
  });
}

// 피아노 건반 하이라이트 관리 함수들
function clearPianoHighlight() {
  const allKeys = document.querySelectorAll('.piano-key');
  allKeys.forEach(key => {
    key.classList.remove('left-hand', 'right-hand');
  });
}

function highlightLeftHand(bassNote) {
  if (!bassNote) return;
  
  // 플랫을 샤프로 변환
  function flatToSharp(note) {
    const flatToSharpMap = {
      'Bb': 'A#', 'Db': 'C#', 'Eb': 'D#', 'Gb': 'F#', 'Ab': 'G#'
    };
    return flatToSharpMap[note] || note;
  }
  
  const convertedBassNote = flatToSharp(bassNote);
  
  // 왼손 영역: A1~G#2에서 베이스 노트 찾기
  const possibleOctaves = ['1', '2'];
  
  for (const octave of possibleOctaves) {
    const noteWithOctave = convertedBassNote + octave;
    const bassKey = document.querySelector(`.piano-key[data-note="${noteWithOctave}"]`);
    
    if (bassKey) {
      // A1~G#2 범위인지 확인
      const leftHandRange = ['A1', 'A#1', 'B1', 'C2', 'C#2', 'D2', 'D#2', 'E2', 'F2', 'F#2', 'G2', 'G#2'];
      if (leftHandRange.includes(noteWithOctave)) {
        bassKey.classList.add('left-hand');
        break; // 첫 번째로 찾은 것만 하이라이트
      }
    }
  }
}

function highlightRightHand(noteNames) {
  if (!noteNames || noteNames.length === 0) return;
  
  // 오른손 영역: C3~C6, 구성음 순서대로 연속 배치
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

// 프렛보드 초기화 함수
function initializeFretboard() {
  
  // 기본 0-5프렛 범위로 설정
  updateFretboardRange(0, 5);
  
  // 하이라이트 제거
  clearGuitarHighlight();
}

// 기타 프렛보드 하이라이트 관리 함수들
function clearGuitarHighlight() {
  const allCells = document.querySelectorAll('.fret-cell');
  allCells.forEach(cell => {
    cell.classList.remove('pressed', 'open', 'muted');
  });
  
  // 프렛 범위 표시 초기화
  const rangeIndicator = document.getElementById('fret-range-indicator');
  if (rangeIndicator) {
    rangeIndicator.textContent = '';
  }
}

// 프렛 범위 계산 함수
function calculateOptimalFretRange(frets) {
  // 유효한 프렛 번호만 필터링 (0보다 큰 값)
  const activeFrets = frets.filter(f => f > 0);
  
  // 개방현만 있거나 활성 프렛이 없으면 기본 0-5 범위
  if (activeFrets.length === 0) {
    return [0, 5];
  }
  
  const minFret = Math.min(...activeFrets);
  const maxFret = Math.max(...activeFrets);
  
  // 5프렛 윈도우 계산
  let startFret = Math.max(0, minFret - 2); // 여유 공간 1프렛
  let endFret = startFret + 5;
  
  // 만약 maxFret가 윈도우를 벗어나면 윈도우를 우측으로 이동
  if (maxFret > endFret) {
    endFret = maxFret;
    startFret = Math.max(0, endFret - 5);
  }
  
  return [startFret, endFret];
}

// 프렛보드 범위 동적 업데이트 함수
function updateFretboardRange(startFret, endFret) {
  
  // 프렛 범위 표시 업데이트
  const rangeIndicator = document.getElementById('fret-range-indicator');
  if (rangeIndicator) {
    if (startFret === 0) {
      rangeIndicator.textContent = `(Open - ${endFret}프렛)`;
    } else {
      rangeIndicator.textContent = `(${startFret+1}-${endFret}프렛)`;
    }
  }
  
  // 프렛 번호 헤더 업데이트
  const headers = document.querySelectorAll('.fret-header');
  headers.forEach((header, index) => {
    if (index === 0) return; // 첫 번째는 빈 헤더
    if (index === 1) {
      header.textContent = startFret.toString();
    } else if (index <= 6) { // 나머지 5개 헤더
      header.textContent = (startFret + index - 1).toString();
    }
  });
  
  // 이전 last-fret 클래스 제거
  document.querySelectorAll('.last-fret').forEach(cell => {
    cell.classList.remove('last-fret');
  });
  
  // 모든 프렛 셀의 data-fret 속성 업데이트
  for (let stringNum = 1; stringNum <= 6; stringNum++) {
    const cells = document.querySelectorAll(`[data-string="${stringNum}"]`);
    cells.forEach((cell, index) => {
      if (index === 0) {
        // 첫 번째는 개방현 (0프렛)
        cell.setAttribute('data-fret', '0');
      } else if (index <= 5) {
        // 나머지 5개는 startFret부터 순서대로
        cell.setAttribute('data-fret', (startFret + index).toString());
        
        // 마지막 프렛(5번째 인덱스)에 last-fret 클래스 추가
        if (index === 5) {
          cell.classList.add('last-fret');
        }
      }
    });
  }
  
  // 너트 표시 조건부 적용 (0프렛이 포함될 때만)
  const fretboard = document.getElementById('guitar-fretboard');
  if (startFret === 0) {
    fretboard.classList.add('show-nut');
  } else {
    fretboard.classList.remove('show-nut');
  }
}

function displayGuitarFingering(fingeringData) {
  clearGuitarHighlight();
  
  if (!fingeringData || !fingeringData.patterns || fingeringData.patterns.length === 0) {
    console.error('운지법 패턴이 없습니다.');
    return;
  }
  
  const pattern = fingeringData.patterns[0]; // 첫 번째 패턴 사용
  const frets = pattern.frets;
  
  // 최적 프렛 범위 계산 및 프렛보드 업데이트
  const [startFret, endFret] = calculateOptimalFretRange(frets);
  updateFretboardRange(startFret, endFret);
  
  // frets 배열은 [1번줄, 2번줄, 3번줄, 4번줄, 5번줄, 6번줄] 순서
  for (let stringIndex = 0; stringIndex < frets.length; stringIndex++) {
    const stringNumber = stringIndex + 1; // 1~6번줄
    const fretNumber = frets[stringIndex];
    
    const cell = document.querySelector(`[data-string="${stringNumber}"][data-fret="${fretNumber >= 0 ? fretNumber : 0}"]`);
    
    if (cell) {
      if (fretNumber === -1) {
        // 뮤트
        cell.classList.add('muted');
      } else if (fretNumber === 0) {
        // 개방현
        cell.classList.add('open');
      } else {
        // 프렛 누름
        cell.classList.add('pressed');
      }
    }
  }
}

// Tone.js 신디사이저 설정
let synth = null;
let currentChordData = null;

// 오디오 초기화
function initializeAudio() {
  if (typeof Tone !== 'undefined' && !synth) {
    synth = new Tone.PolySynth().toDestination();
  }
}

// 코드 재생 함수 (아르페지오 → 화음)
async function playChord(noteNames, bassNote) {
  if (!synth) {
    console.error('신디사이저가 초기화되지 않았습니다.');
    return;
  }
  
  try {
    // Tone.js 오디오 컨텍스트 시작
    if (Tone.context.state !== 'running') {
      await Tone.start();
    }
    
    const playBtn = document.getElementById('playChordBtn');
    playBtn.disabled = true;
    playBtn.innerHTML = '<i class="bi bi-stop-fill"></i> 재생 중...';
    
    // 베이스음이 있으면 맨 앞에 추가, 없으면 구성음만 사용
    const allNotes = bassNote && !noteNames.includes(bassNote) 
      ? [bassNote, ...noteNames] 
      : [...noteNames];
    
    // 옥타브 추가 (4옥타브 기준)
    const notesWithOctave = allNotes.map(note => note + '4');
    
    // 1. 아르페지오 재생 (각 음을 0.3초씩 순차적으로)
    for (let i = 0; i < notesWithOctave.length; i++) {
      synth.triggerAttackRelease(notesWithOctave[i], '0.5', Tone.now() + i * 0.3);
    }
    
    // 아르페지오 완료 후 화음 재생 (1.5초 후)
    const chordDelay = notesWithOctave.length * 0.3 + 0.3;
    synth.triggerAttackRelease(notesWithOctave, '2', Tone.now() + chordDelay);
    
    // 재생 완료 후 버튼 복원 (총 재생 시간 계산)
    const totalDuration = (chordDelay + 2) * 1000;
    setTimeout(() => {
      playBtn.disabled = false;
      playBtn.innerHTML = '<i class="bi bi-play-fill"></i> 코드 듣기';
    }, totalDuration);
    
  } catch (error) {
    console.error('오디오 재생 오류:', error);
    const playBtn = document.getElementById('playChordBtn');
    playBtn.disabled = false;
    playBtn.innerHTML = '<i class="bi bi-play-fill"></i> 코드 듣기';
  }
}

// Enter 키 지원
document.addEventListener('DOMContentLoaded', function() {
  document.getElementById('chordSymbol').addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
      parseChord();
    }
  });
  
  // 재생 버튼 이벤트 리스너
  document.getElementById('playChordBtn').addEventListener('click', function() {
    const noteNames = currentChordData?.noteNames;
    const bassNote = currentChordData?.bass;
    
    if (noteNames && noteNames.length > 0) {
      playChord(noteNames, bassNote);
    }
  });
  
  // 페이지 로드 시 프렛보드 초기화 및 오디오 초기화
  initializeFretboard();
  initializeAudio();
});