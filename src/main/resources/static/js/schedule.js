const newRowBtn = document.getElementById('newRowBtn');
const newRow = document.getElementById('newRow');

const targetType = document.getElementById('targetType');
const role = document.getElementById('role');
const dayType = document.getElementById('dayType');
const targetDay = document.getElementById('targetDay');
const targetHour = document.getElementById('targetHour');
const content = document.getElementById('content');

const saveNewBtn = document.getElementById('saveNewBtn');

function showNewRow() {
  newRowBtn.classList.add('d-none');
  newRow.classList.remove('d-none');
}
newRowBtn.addEventListener('click', showNewRow);

// 예: 08:00 ~ 18:00까지 1시간 간격으로 추가
for (let hour = 0; hour < 24; hour++) {
  const option = document.createElement('option');
  option.value = hour;
  option.textContent = hour + '시';

  targetHour.appendChild(option);
}

const days = [
  { "code": "SUNDAY", "text": "일요일" },
  { "code": "MONDAY", "text": "월요일" },
  { "code": "TUESDAY", "text": "화요일" },
  { "code": "WEDNESDAY", "text": "수요일" },
  { "code": "THURSDAY", "text": "목요일" },
  { "code": "FRIDAY", "text": "금요일" },
  { "code": "SATURDAY", "text": "토요일" }
]

function dayTypeChange() {
  const dayOption = dayType.value;
  targetDay.innerHTML = ''
  if (dayOption == 'WEEK') {
    for (let idx = 0; idx < 7; idx++) {
      const dayOption = document.createElement('option');
      dayOption.value = days[idx]["code"];
      dayOption.textContent = days[idx]["text"];
      targetDay.appendChild(dayOption);
    }
  } else if (dayOption == 'MONTH') {
    for (let idx = 1; idx <= 31; idx++) {
      const dayOption = document.createElement('option');
      dayOption.value = idx;
      dayOption.textContent = idx + '일';
      targetDay.appendChild(dayOption);
    }
  }
}
dayTypeChange()
dayType.addEventListener('change', dayTypeChange);

function targetTypeChange(){
  const targetOption = targetType.value;
  if(targetOption == "LINE_UP"){
    content.disabled = true;
  }else if(targetOption == "ROLE"){
    content.disabled = false;
  }
}
targetTypeChange()
targetType.addEventListener('change', targetTypeChange);

function saveNewSchedule() {
  const newSchedule = {
    targetType: targetType.value,
    role: role.value,
    dayType: dayType.value,
    targetDay: targetDay.value,
    targetHour: targetHour.value,
    content: content.value
  }

  fetch('/schedule/save', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [csrfHeader]: csrfToken
    },
    body: JSON.stringify(newSchedule)
  }).then(() => {
    location.reload(); 
  });

}
saveNewBtn.addEventListener('click', saveNewSchedule);

function deleteSchedule(scheduleId) {
  const data = new URLSearchParams();
  data.append('scheduleId', scheduleId);

  fetch('/schedule/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      [csrfHeader]: csrfToken
    },
    body: data
  }).then(() => {
    location.reload();
  });
}