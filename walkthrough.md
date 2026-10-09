# 👑 DaVE Player v2.9.0 — Super Pack Pro (Ambilight, Widgets, Folder Shield, Insights, Batch Covers & Karaoke)

Esta colosal actualización lleva a **DaVE Player** al siguiente nivel de sofisticación estética, control desde la pantalla de inicio y herramientas audiófilas de vanguardia.

---

## 🚀 Novedades y Características Principales de v2.9.0

### 1. 🌈 Visual Ambilight Fluido Reactivo (`FluidAmbilightGlow.kt`)
* **Mesh Gradient Dinámico:** Fondo orgánico con esferas de luz pulsantes y desenfoque Gaussiano profundo (`blur(70.dp)`).
* **Adaptación Cromática en Tiempo Real:** Extrae automáticamente la paleta de colores de la carátula en reproducción y genera una atmósfera inmersiva tipo Ambilight / TV OLED detrás del vinilo y los controles.
* **Transiciones Suaves:** Movimiento sutil mediante `infiniteRepeatable` con curvas senoidales para un efecto vivo y relajante.

---

### 2. 📱 Widgets de Escritorio Material 3 (`DaVEAppWidgetProvider.kt`)
* **Diseño Compacto & Cyberpunk (`widget_dave_compact.xml`):**
  - Carátula redondeada del álbum en reproducción con carga asíncrona mediante `ImageDecoder`.
  - Título y artista con recorte elíptico elegante.
  - Controles táctiles inmediatos: **Play / Pausa**, **Siguiente Pista** y **Pista Anterior**.
  - Toque en la tarjeta para abrir y enfocar directamente la app.
* **Sincronización Reactiva:** Cada cambio de canción o estado de reproducción actualiza todos los widgets en la pantalla de inicio al instante.

---

### 3. 🛡️ Folder Shield & Filtro de WhatsApp/Telegram (`FolderShieldManager.kt`, `FolderShieldDialog.kt`)
* **Ocultar Notas de Voz & Audios de Mensajería:** Filtra con 1 toque audios de WhatsApp (`/WhatsApp/`, `/WhatsApp Audio/`, `.opus`) y Telegram (`/Telegram/`, `/Telegram Audio/`).
* **Filtro de Duración Mínima:** Desplaza un control deslizante de 1s a 120s para excluir tonos, efectos cortos y notas de voz.
* **Filtro de Tonos del Sistema:** Excluye carpetas `/Ringtones/`, `/Notifications/` y `/Alarms/`.
* **Contador en Tiempo Real:** Muestra exactamente cuántos audios basura han sido ocultados y cuántas pistas musicales reales quedan en tu biblioteca.

---

### 4. 📊 DaVE Insights & Métricas Musicales (`PlaybackStatsManager.kt`, `DaveInsightsDialog.kt`)
* **Minutos de Reproducción Diarios:** Mide tu tiempo real de escucha del día actual.
* **Horas Totales Históricas:** Contador de por vida de horas de música disfrutadas en DaVE Player.
* **Hora Pico de Escucha:** Algoritmo que detecta tu horario de mayor actividad musical (ej. `21:00 - 22:00`).
* **Top 5 Artistas Más Escuchados:** Barras de progreso visuales con los artistas que más suenan en tus auriculares.

---

### 5. 🖼️ Descargador Masivo de Carátulas Oficiales (`BatchCoverFetcherDialog.kt`)
* **Escaneo Automático de Huérfanas:** Detecta todas las canciones de tu almacenamiento local que carecen de portada.
* **Descarga en Lote en Segundo Plano:** Obtiene portadas oficiales en alta resolución (HD) de iTunes Search API con reintentos inteligentes y barra de progreso paso a paso.
* **Persistencia Inmediata:** Asigna las carátulas descargadas al almacenamiento interno de la app y refresca la biblioteca sin interrumpir la música.

---

### 6. 🎤 Estudio Karaoke & Calibración de Audífonos (`GaplessManager.kt`, `HeadphoneProfileManager.kt`, `KaraokeDjDialog.kt`)
* **Reductor Vocal en Tiempo Real (Modo Karaoke):** Algoritmo de supresión de centro estéreo (Center Channel Attenuation) para cantar sobre cualquier canción en tu biblioteca.
* **Acceso Directo en Reproductor:** Píldora táctil `🎤 KARAOKE` en la barra de herramientas del reproductor principal.
* **Balance Estéreo L/R:** Deslizador preciso para calibrar el volumen entre auricular izquierdo y derecho.
* **Perfiles AutoEQ para Audífonos Populares:**
  - 🍎 Apple AirPods / AirPods Pro
  - 🎧 Sony WH-1000XM4 / XM5
  - 🔊 Samsung Galaxy Buds Pro
  - ⚡ JBL Tune / Live Series
  - 🎯 KZ IEMs (ZSN / ZSX Hi-Fi)
  - 🎙️ Bose QuietComfort Series

---

## 📦 Artefactos & Descargas de la Versión v2.9.0

* **Versión:** `v2.9.0` (código de versión `59`)
* **Archivo APK:** `DaVE-v2.9.0.apk` / `DaVE.apk` (~25 MB)
* **Ubicaciones en Google Drive:**
  * `G:\Mi unidad\DaVE-v2.9.0.apk` & `G:\Mi unidad\DaVE.apk`
  * `H:\Mi unidad\DaVE-v2.9.0.apk` & `H:\Mi unidad\DaVE.apk`
* **Local:** `build_output\app-debug.apk`
* **GitHub Release Oficial:** [✨ DaVE Player v2.9.0 en GitHub Releases](https://github.com/davidchaparro780-wq/AuraPlayer/releases/tag/v2.9.0)
