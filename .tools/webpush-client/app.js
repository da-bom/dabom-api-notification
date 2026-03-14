const state = {
  registration: null,
  subscription: null,
};

const el = {
  baseUrl: document.getElementById("baseUrl"),
  jwtSecret: document.getElementById("jwtSecret"),
  memberId: document.getElementById("memberId"),
  role: document.getElementById("role"),
  expiresMin: document.getElementById("expiresMin"),
  token: document.getElementById("token"),
  message: document.getElementById("message"),
  generateTokenBtn: document.getElementById("generateTokenBtn"),
  registerSwBtn: document.getElementById("registerSwBtn"),
  requestPermissionBtn: document.getElementById("requestPermissionBtn"),
  subscribeBtn: document.getElementById("subscribeBtn"),
  sendBtn: document.getElementById("sendBtn"),
  clearLogBtn: document.getElementById("clearLogBtn"),
  log: document.getElementById("log"),
};

el.generateTokenBtn.addEventListener("click", onGenerateToken);
el.registerSwBtn.addEventListener("click", onRegisterSw);
el.requestPermissionBtn.addEventListener("click", onRequestPermission);
el.subscribeBtn.addEventListener("click", onSubscribe);
el.sendBtn.addEventListener("click", onSendPush);
el.clearLogBtn.addEventListener("click", () => {
  el.log.innerHTML = "";
});

function log(message, isError = false) {
  const p = document.createElement("p");
  p.className = `line ${isError ? "err" : "ok"}`;
  p.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
  el.log.prepend(p);
}

function getBaseUrl() {
  return el.baseUrl.value.trim().replace(/\/+$/, "");
}

function getAuthHeaders() {
  const token = el.token.value.trim();
  if (!token) {
    throw new Error("Token is empty. Generate token first.");
  }
  return { Authorization: `Bearer ${token}` };
}

function bytesToBase64(bytes) {
  let binary = "";
  bytes.forEach((b) => {
    binary += String.fromCharCode(b);
  });
  return btoa(binary);
}

function base64UrlEncodeBytes(bytes) {
  return bytesToBase64(bytes)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function utf8ToBase64Url(str) {
  const bytes = new TextEncoder().encode(str);
  return base64UrlEncodeBytes(bytes);
}

async function signHs256(unsignedToken, secret) {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(unsignedToken)
  );
  return base64UrlEncodeBytes(new Uint8Array(signature));
}

async function onGenerateToken() {
  try {
    const secret = el.jwtSecret.value.trim();
    if (!secret) {
      throw new Error("JWT secret is required.");
    }

    const nowSec = Math.floor(Date.now() / 1000);
    const expSec = nowSec + Number(el.expiresMin.value || "120") * 60;

    const header = { alg: "HS256", typ: "JWT" };
    const payload = {
      sub: String(el.memberId.value || "1"),
      role: String(el.role.value || "MEMBER"),
      iat: nowSec,
      exp: expSec,
    };

    const encodedHeader = utf8ToBase64Url(JSON.stringify(header));
    const encodedPayload = utf8ToBase64Url(JSON.stringify(payload));
    const unsignedToken = `${encodedHeader}.${encodedPayload}`;
    const signature = await signHs256(unsignedToken, secret);
    const token = `${unsignedToken}.${signature}`;

    el.token.value = token;
    log("Token generated.");
  } catch (err) {
    log(`Token generation failed: ${err.message}`, true);
  }
}

async function onRegisterSw() {
  try {
    if (!("serviceWorker" in navigator)) {
      throw new Error("Service Worker is not supported in this browser.");
    }

    state.registration = await navigator.serviceWorker.register("./sw.js");
    await navigator.serviceWorker.ready;
    log("Service worker registered.");
  } catch (err) {
    log(`Service worker registration failed: ${err.message}`, true);
  }
}

async function onRequestPermission() {
  try {
    if (!("Notification" in window)) {
      throw new Error("Notification API is not supported.");
    }
    const permission = await Notification.requestPermission();
    if (permission !== "granted") {
      throw new Error(`Notification permission: ${permission}`);
    }
    log("Notification permission granted.");
  } catch (err) {
    log(`Notification permission failed: ${err.message}`, true);
  }
}

function urlBase64ToUint8Array(base64String) {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = atob(base64);
  const output = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; i += 1) {
    output[i] = rawData.charCodeAt(i);
  }
  return output;
}

async function fetchVapidPublicKey() {
  const response = await fetch(`${getBaseUrl()}/push/vapid-public-key`, {
    method: "GET",
    headers: {
      ...getAuthHeaders(),
    },
  });

  if (!response.ok) {
    throw new Error(`vapidPublicKey failed (${response.status}): ${await response.text()}`);
  }

  const body = await response.json();
  if (!body.data || !body.data.publicKey) {
    throw new Error(`invalid vapidPublicKey response: ${JSON.stringify(body)}`);
  }
  return body.data.publicKey;
}

async function onSubscribe() {
  try {
    if (!state.registration) {
      await onRegisterSw();
      if (!state.registration) {
        throw new Error("service worker is not registered.");
      }
    }

    if (Notification.permission !== "granted") {
      await onRequestPermission();
      if (Notification.permission !== "granted") {
        throw new Error("notification permission was not granted.");
      }
    }

    const vapidPublicKey = await fetchVapidPublicKey();

    state.subscription =
      (await state.registration.pushManager.getSubscription()) ||
      (await state.registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidPublicKey),
      }));

    const serialized = state.subscription.toJSON();
    const subscribeBody = {
      endpoint: serialized.endpoint,
      keys: {
        p256dh: serialized.keys.p256dh,
        auth: serialized.keys.auth,
      },
    };

    const subscribeResponse = await fetch(`${getBaseUrl()}/push/subscribe`, {
      method: "POST",
      headers: {
        ...getAuthHeaders(),
        "Content-Type": "application/json",
      },
      body: JSON.stringify(subscribeBody),
    });

    if (!subscribeResponse.ok) {
      throw new Error(`subscribe failed (${subscribeResponse.status}): ${await subscribeResponse.text()}`);
    }

    log("Subscription saved on backend.");
  } catch (err) {
    log(`Subscribe failed: ${err.message}`, true);
  }
}

async function onSendPush() {
  try {
    const msg = el.message.value.trim();
    if (!msg) {
      throw new Error("Message is empty.");
    }

    const response = await fetch(`${getBaseUrl()}/push/send`, {
      method: "POST",
      headers: {
        ...getAuthHeaders(),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ message: msg }),
    });

    if (!response.ok) {
      throw new Error(`send failed (${response.status}): ${await response.text()}`);
    }

    log(`Push send requested: "${msg}"`);
  } catch (err) {
    log(`Send failed: ${err.message}`, true);
  }
}

log("Ready. Use Generate token -> Register SW -> Request permission -> Subscribe -> Send push.");
