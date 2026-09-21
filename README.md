# 🎵 NovaPlayer - Reproductor de Música y Video Sin Anuncios

[![Android Build](https://github.com/davidchaparro780-wq/NovaPlayer/actions/workflows/build-apk.yml/badge.svg)](https://github.com/davidchaparro780-wq/NovaPlayer/actions/workflows/build-apk.yml)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Ads](https://img.shields.io/badge/Ads-0%20(Sin%20Anuncios)-brightgreen.svg)]()
[![Performance](https://img.shields.io/badge/UI-120_FPS_Compose-blue.svg)]()

> **NovaPlayer** es una alternativa moderna, ultra ligera y 100% libre de anuncios a reproductores convencionales como Lark Player. Diseñada para ofrecer la máxima fluidez visual, arranque instantáneo y un mínimo consumo de batería y memoria RAM.

---

## 📱 ¿Cómo instalarlo en tu teléfono Android?

1. Ve a la sección **[Releases](https://github.com/davidchaparro780-wq/NovaPlayer/releases)** o **[Actions](https://github.com/davidchaparro780-wq/NovaPlayer/actions)** de este repositorio desde el navegador de tu teléfono.
2. Descarga el archivo **`NovaPlayer-debug.apk`**.
3. Abre el archivo descargado en tu celular y pulsa **Instalar** (si te lo solicita, autoriza "Instalar apps de fuentes desconocidas" para tu navegador).
4. ¡Listo! Disfruta de tu música y videos favoritos sin interrupciones.

---

## ✨ Características Principales

* **🚫 Cero Publicidad y Cero Rastreadores:** Sin anuncios invasivos, pantallas de espera ni servicios de telemetría de fondo.
* **⚡ Ultra Fluidez a 120 FPS:** Interfaz construida íntegramente con **Jetpack Compose** y **Material Design 3** (colores dinámicos adaptados a tu dispositivo).
* **🎧 Reproducción de Audio en Segundo Plano:** Motor impulsado por **AndroidX Media3 (ExoPlayer)**. La música continúa reproduciéndose con la pantalla bloqueada o mientras usas otras apps, con controles en la barra de estado y soporte para auriculares / Bluetooth.
* **🎬 Reproductor de Video con Ventana Flotante (PiP):** Mira tus videos en una pequeña ventana flotante sobre cualquier otra aplicación gracias al soporte nativo de *Picture-in-Picture*.
* **🎛️ Ecualizador Nativo & Bass Boost:** Ecualizador de 5 bandas personalizable con presets (Rock, Pop, Jazz, Clásica) y refuerzo dinámico de graves.
* **📁 Explorador por Carpetas:** Organiza y navega tu música directamente por carpetas de almacenamiento para un acceso sin desorden.
* **🔍 Búsqueda en Tiempo Real:** Filtra al instante canciones por título, artista o álbum.

---

## 🛠️ Stack Tecnológico

* **Lenguaje:** Kotlin 2.0+
* **UI:** Jetpack Compose + Material 3
* **Motor Multimedia:** AndroidX Media3 (ExoPlayer 1.5.1 + MediaSessionService)
* **Gestión de Imágenes y Carátulas:** Coil Compose
* **Compilación y CI/CD:** GitHub Actions (Compilación automática de APKs en cada push)

---

## 🚀 Compilación Local (Para Desarrolladores)

Si deseas clonar y compilar el proyecto en tu máquina local:

```bash
git clone https://github.com/davidchaparro780-wq/NovaPlayer.git
cd NovaPlayer
./gradlew assembleDebug
```

El APK generado se encontrará en:
`app/build/outputs/apk/debug/app-debug.apk`
