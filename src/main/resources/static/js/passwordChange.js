const password = document.getElementById('after');
const confirm = document.getElementById('confirm');

function validatePasswords() {
  if (!confirm.value) {
    message.textContent = '';
    return;
  }

  if (password.value === confirm.value) {
    message.textContent = '비밀번호가 일치합니다.';
    message.style.color = 'green';
  } else {
    message.textContent = '비밀번호가 일치하지 않습니다.';
    message.style.color = 'red';
  }
}
password.addEventListener('input', validatePasswords);
confirm.addEventListener('input', validatePasswords)


const form = document.getElementById('password-change-form');
form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const formData = new FormData(e.target);
  const res = await fetch("/profile/password", {
    method: "POST",
    body: formData
  });

  const result = await res.text();
  alert(result);
});