
async function pushAll() {
  const message = document.getElementById('messageInput').value;
  await fetch(
    `/webpush/push?message=${encodeURIComponent(message)}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        [csrfHeader]: csrfToken
      },
    });
  alert('푸시 전송 완료!');
  document.getElementById('messageInput').value = "";
};

async function pushById(id) {
  const message = document.getElementById('messageInput').value;
  await fetch(
    `/webpush/push/${id}?message=${encodeURIComponent(message)}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        [csrfHeader]: csrfToken
      },
    });
  alert('푸시 전송 완료!');
  document.getElementById('messageInput').value = "";
}