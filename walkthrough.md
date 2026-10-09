# 👑 DaVE Player v3.0.0 — The Milestone Evolution (LRC Karaoke, Cassette Tape, iPod Classic, DaVE Drive & Audio Studio)

Esta versión histórica (**v3.0.0**, `versionCode = 60`) transforma a **DaVE Player** en el reproductor de música más completo, avanzado y nostálgico del ecosistema Android, incorporando características tomadas y perfeccionadas de los mejores reproductores del mundo (Spotify, Apple Music, Poweramp, Musicolet, Neutron).

---

## 🚀 Novedades y Características Principales de v3.0.0

### 1. 🎤 Letras Sincronizadas LRC en Tiempo Real (`SyncedLyricsManager.kt`, `SyncedLyricsDialog.kt`)
* **Auto-Scroll Fluido & Resaltado Neón:** Visualización en pantalla completa con marquesina dinámica verso a verso, tipografía gigante de 23sp y fondo degradado inmersivo.
* **Tap-to-Seek:** Toca cualquier verso de la canción para saltar instantáneamente a ese segundo exacto de la pista.
* **Descarga Automática Gratuita con LRCLIB:** Busca y guarda archivos con marcas de tiempo `.lrc` sin coste ni registros.
* **Compartir Versos:** Genera tarjetas estilizadas con la frase que estás escuchando para compartirla directamente en WhatsApp o Instagram Stories.

---

### 2. 📼 Skin Retro Vintage: Cassette Tape Interactivo (`CassetteTapeSkin.kt`)
* **Física Mecánica en Vivo:** Convierte la pantalla del reproductor en un casete de audio clásico de los 80/90s (Sony Walkman, TDK).
* **Bobinas Giratorias Reales:** Los dos carretes giran mecánicamente acompasados con el playback.
* **Trasvase Físico de Cinta Magnética:** La cinta marrón de óxido de hierro viaja del carrete izquierdo al derecho según el minuto real de la canción (`currentPosition / duration`).
* **Etiqueta Vintage Rotulada:** Con el título de la canción y artista escritos con estilo marcador.

---

### 3. 🕹️ Skin Retro iPod Classic con Click Wheel Táctil (`IpodClassicSkin.kt`)
* **Nostalgia Pura de los 2000s:** Carcasa plateada y pantalla LCD retro con indicador de progreso.
* **Rueda Táctil Háptica Giratoria (Click Wheel):** Deslizar el pulgar en círculos activa micro-vibraciones hápticas (*clic-clic-clic*) con respuesta mecánica continua para adelantar o retroceder la canción.

---

### 4. 🚗 Modo Coche / DaVE Drive (`DaveDriveScreen.kt`)
* **Conducción Segura sin Distracciones:** Interfaz de ultra-alto contraste sobre negro absoluto con botones táctiles gigantes (Play/Pausa de 94dp).
* **Gestos a Ciegas en Pantalla Completa:** Deslizar horizontalmente con un dedo en cualquier parte para cambiar de canción; doble toque para pausar/reanudar sin apartar la vista del camino.
* **Anuncio por Voz TTS:** DaVE anuncia por los altavoces o Bluetooth del coche el nombre de la canción y el artista al arrancar cada pista.

---

### 5. 🌊 Procesador de Audio Viral en 1 Tap: Slowed+Reverb & Nightcore (`ViralAudioEffectsManager.kt`)
* **🌊 Slowed + Reverb:** Desacelera la pista a 0.85x, activa cámara de reverberación espacial de sala amplia y amplifica los sub-bajos Lo-Fi.
* **⚡ Nightcore / Sped Up:** Acelera la pista a 1.25x con agudos brillantes para ritmos enérgicos y entrenamientos.
* **✨ Modo Normal:** Restablece velocidad 1.0x y balance neutro con 1 solo toque.

---

### 6. 🔊 Normalizador Inteligente de Volumen (`LoudnessNormalizerManager.kt`)
* **Nivelación Dinámica a -14 LUFS:** Iguala el volumen percibido de todas las canciones para evitar que una pista antigua suene muy baja y la siguiente te aturda.

---

### 7. 🎧 Crossfeed Binaural Bauer para Auriculares (`CrossfeedManager.kt`)
* **Eliminación de Fatiga Auditiva:** Simula la acústica natural de altavoces en una habitación inyectando una fracción microscópica del canal opuesto, permitiendo escuchar música durante horas sin dolor de cabeza.

---

### 8. ⚡ Modo ZAP — DJ Escucha Rápida (`ZapPreviewManager.kt`, `ZapDialog.kt`)
* **Exploración Relámpago:** Salta automáticamente por tu biblioteca reproduciendo solo 12 segundos del punto álgido / coro estimado de cada pista. Si te gusta lo que oyes, un toque lo deja sonando completo.

---

### 9. 🎭 Rueda de Estados de Ánimo (`MoodWheelDialog.kt`)
* **Curaduría Emocional en 1 Tap:** Selector de 5 estados (⚡ Fiesta & Euforia, 🧘 Focus & Estudio, 🌧️ Melancolía, 🌴 Chill & Relax, 🔥 Motivación & Gym) que genera colas instantáneas inteligentes afines a tu vibra.

---

### 10. 🎬 Extractor y Conversor de Video a Audio (`VideoToAudioExtractor.kt`, `VideoExtractorDialog.kt`)
* **Conversión sin Pérdida:** Extrae la pista de audio de cualquier video guardado en la galería a MP3/M4A 320k en ~2 segundos sin recodificación pesada y lo añade a tu biblioteca local.

---

### 11. 🎯 Inspector de Calidad Real de Audio (`AudioQualityInspector.kt`, `AudioQualityDialog.kt`)
* **Detector de Fake Hi-Res:** Mide el bitrate real, tasa de muestreo (kHz) y códec para certificar si un archivo es verdaderamente Hi-Res Lossless o un audio comprimido inflado.

---

### 12. 📻 Silent Disco / Party Link por Wi-Fi Local (`PartyLinkManager.kt`, `PartyLinkDialog.kt`)
* **Sincronización Multi-Teléfono:** Conecta múltiples dispositivos en la misma red Wi-Fi para reproducir la misma canción al milisegundo exacto como un sistema de altavoces envolvente sincronizado.

---

### 13. 🌌 Lienzos Cinemáticos en Bucle (`CanvasLoopsOverlay.kt`)
* **Fondos Vivos a 60 fps:** Activa lluvia estética Lo-Fi, olas synthwave o partículas estelares flotantes como fondo dinámico detrás del reproductor.

---

### 14. ✂️ Smart Silence Trimmer (`SilenceTrimmerManager.kt`)
* **Recorte Inteligente de Silencios:** Salta automáticamente los silencios muertos de 2 a 4 segundos al inicio de canciones para un flujo musical sin pausas.

---

## 📦 Artefactos & Descargas de la Versión v3.0.0

* **Versión:** `v3.0.0` (código de versión `60`)
* **Archivo APK:** `DaVE-v3.0.0.apk` / `DaVE.apk` (~25 MB)
* **Ubicaciones en Google Drive:**
  * `G:\Mi unidad\DaVE-v3.0.0.apk` & `G:\Mi unidad\DaVE.apk`
  * `H:\Mi unidad\DaVE-v3.0.0.apk` & `H:\Mi unidad\DaVE.apk`
* **Local:** `build_output\app-debug.apk`
* **GitHub Release Oficial:** [✨ DaVE Player v3.0.0 en GitHub Releases](https://github.com/davidchaparro780-wq/AuraPlayer/releases/tag/v3.0.0)
