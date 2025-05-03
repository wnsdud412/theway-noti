

const nameInput = document.getElementById('name-input');
const nameList = document.getElementById('name-list');
const namesHidden = document.getElementById('names-hidden');

const names = new Set(); // 중복 방지를 위해 Set 사용

function addName() {
  const name = nameInput.value.trim();
  if (!name || names.has(name)) return;

  names.add(name);

  const row = document.createElement('div');
  row.className = 'd-flex justify-content-between align-items-center border px-3 py-2 mb-2 w-100 rounded bg-light';

  const label = document.createElement('span');
  label.textContent = name;

  const remove = document.createElement('span');
  remove.textContent = '❌';
  remove.style.cursor = 'pointer';
  remove.style.marginLeft = '6px';
  remove.onclick = () => {
    names.delete(name);
    nameList.removeChild(row);
    namesHidden.value = Array.from(names).join(',');
  };

  row.appendChild(label);
  row.appendChild(remove);
  nameList.appendChild(row);

  // hidden input 업데이트
  namesHidden.value = Array.from(names).join(',');

  // 초기화
  nameInput.value = '';
  nameInput.focus();
}

function addNameInit(nameValue){
  nameInput.value = nameValue;
  addName();
}

nameInput.addEventListener('keydown', function (e) {
  if (e.key === 'Enter') {
    e.preventDefault();
    addName();
  }
});

const roleInput = document.getElementById('role-input');
const roleDropdown = document.getElementById('role-dropdown');
let dropdownHideTimeout;

const availableRoles = [
  { code: 'LEADER', name: '인도자' },
  { code: 'MAIN', name: '메인' },
  { code: 'SECOND', name: '세컨' },
  { code: 'DRUMS', name: '드럼' },
  { code: 'BASS', name: '베이스' },
  { code: 'ELECTRIC_GUITAR', name: '일렉' },
  { code: 'ACOUSTIC_GUITAR', name: '통기타' },
  { code: 'SINGER_MALE', name: '싱어(남)' },
  { code: 'SINGER_FEMALE', name: '싱어(여)' },
  { code: 'SHEET_MUSIC', name: '악보' },
  { code: 'LYRICS_DISPLAY', name: '자막' },
  { code: 'SINGER_MANAGER', name: '싱어 팀장' },
  { code: 'INSTRUMENT_MANAGER', name: '악기 팀장' },
  { code: 'NONE', name: '없음' }
];

const selectedRoles = new Set();

function renderDropdown() {
  roleDropdown.innerHTML = '';
  availableRoles.forEach(role => {
    if (!selectedRoles.has(role.code)) {
      const li = document.createElement('li');
      li.textContent = role.name;
      li.onclick = () => selectRole(role);
      roleDropdown.appendChild(li);
    }
  });
}

function renderSelectedRoles() {
  roleInput.innerHTML = '';
  let roleCodes = [];

  selectedRoles.forEach(code => {
    roleCodes.push(code);
    const role = availableRoles.find(r => r.code === code);
    const span = document.createElement('span');
    span.className = 'selected-role';
    span.textContent = role.name;

    const remove = document.createElement('span');
    remove.className = 'remove-role';
    remove.textContent = '×';
    remove.onclick = (e) => {
      e.stopPropagation();
      selectedRoles.delete(code);
      renderDropdown();
      renderSelectedRoles();
    };

    span.appendChild(remove);
    roleInput.appendChild(span);
  });
  document.getElementById('roles-hidden').value = roleCodes.join(',');
}

function selectRole(role) {
  selectedRoles.add(role.code);
  renderDropdown();
  renderSelectedRoles();
  hideDropdown()
}

function showDropdown() {
  clearTimeout(dropdownHideTimeout);
  roleDropdown.style.display = 'block';
}
function hideDropdown() {
  roleDropdown.style.display = 'none';
}

function checkHideDropdown() {
  dropdownHideTimeout = setTimeout(() => {
    if (!roleInput.matches(':hover') && !roleDropdown.matches(':hover')) {
      hideDropdown();
    }
  }, 200);
}

roleInput.addEventListener('mouseenter', showDropdown);
roleDropdown.addEventListener('mouseenter', showDropdown);

roleInput.addEventListener('mouseleave', checkHideDropdown);
roleDropdown.addEventListener('mouseleave', checkHideDropdown);

// 초기 렌더링
renderDropdown();

nicknames.split(',').forEach(nickname => addNameInit(nickname));
roles.split(',').forEach(role => selectRole({"code":role}));