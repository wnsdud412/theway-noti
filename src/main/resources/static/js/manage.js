const form = document.getElementById('myForm');

form.addEventListener('submit', function (event) {
  event.preventDefault();

  if (confirm('해당 사용자의 비밀번호가 [12345]로 초기화 됩니다')) {
    form.submit(); // 사용자가 확인 눌렀을 때만 제출
  }
});