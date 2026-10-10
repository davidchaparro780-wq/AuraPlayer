// DaVE Player Pro — Service Worker v4.0.0
const CACHE_NAME = 'dave-player-v4.0.0';
const STATIC_ASSETS = [
  './index.html',
  './style.css?v=4.0.0',
  './app.js?v=4.0.0',
  './manifest.json'
];

self.addEventListener('install', (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(STATIC_ASSETS).catch((err) => console.log('SW cache err:', err));
    })
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  // Solo manejar solicitudes GET del mismo origen
  if (event.request.method !== 'GET') return;
  
  // No cachear peticiones de streaming de audio o apis locales en el SW
  const url = event.request.url;
  if (url.includes('/music/') || url.includes('/stream/') || url.includes('/api/')) {
    return;
  }

  event.respondWith(
    fetch(event.request).catch(() => {
      return caches.match(event.request);
    })
  );
});
