async function registerPush() {
  if ('serviceWorker' in navigator) {
    try {
      const registration = await navigator.serviceWorker.register('/sw.js');
      await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidPublicKey)
      });

      const subscriptionWithDeviceName = {
        ...subscription.toJSON(),
        deviceName: document.getElementById('deviceName').value
      };

      await fetch('/webpush/subscribe', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          [csrfHeader]: csrfToken 
        },
        body: JSON.stringify(subscriptionWithDeviceName)
      });

      alert('구독 등록 완료!');
    } catch (error) {
      console.error('구독 등록 중 오류 발생:', error);
    }
  } else {
    alert('Service Worker를 지원하지 않는 브라우저입니다.');
  }
  window.location.reload();
};

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - base64String.length % 4) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}
