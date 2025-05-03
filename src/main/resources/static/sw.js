self.addEventListener('push', function(event) {
  const payload = event.data.json();
  const options = {
    body: payload.body,
    data: { 
      pushId: payload.pushId
    },  // 여기에서 전달된 URL 저장
  };
  event.waitUntil(
    self.registration.showNotification(payload.title, options)
  );
});
self.addEventListener('notificationclick', function(event) {
  event.notification.close();

  const pushId = event.notification.data.pushId;
  const url = '/webpush/open-url?pushId='+pushId
  // openUrl로 이동
  event.waitUntil(clients.openWindow(url));
});