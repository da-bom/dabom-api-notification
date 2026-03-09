self.addEventListener("push", (event) => {
  let body = "no payload";
  if (event.data) {
    try {
      body = event.data.text();
    } catch (e) {
      body = "payload parse error";
    }
  }

  event.waitUntil(
    self.registration.showNotification("Web Push", {
      body,
      tag: "web-push-test",
      renotify: true,
    })
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: "window", includeUncontrolled: true }).then((clientList) => {
      if (clientList.length > 0) {
        return clientList[0].focus();
      }
      return clients.openWindow("./");
    })
  );
});
