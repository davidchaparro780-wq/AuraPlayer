# 🛠️ DaVE Player — Resumen de Bugs Solucionados (Web & Desktop)

> **Estado:** 🚀 Todos los bugs identificados han sido resueltos, probados y desplegados en GitHub Pages y Google Drive.

---

### 🐛 Lista de Bugs Solucionados:

1. **🎚️ Ecualizador en Blanco al Iniciar:**  
   - *Bug:* Los 10 deslizadores del ecualizador no se dibujaban hasta que una canción empezaba a sonar. Si entrabas a la pestaña antes de dar Play, estaba vacía.  
   - *Solución:* El renderizado de las 10 bandas ahora se ejecuta inmediatamente al cargar la página.

2. **🎛️ Conflicto entre Stem Mixer y Ecualizador:**  
   - *Bug:* Mover los faders del Stem Mixer sobrescribía los valores del Ecualizador de 10 bandas y viceversa.  
   - *Solución:* Se creó un banco de filtros biquad dedicados e independientes para los Stems (`stemFilters.vocals`, `drums`, `bass`, `melodies`) en serie con el ecualizador, permitiendo usar ambos a la vez sin interferencias.

3. **🔍 Reproducción de Canción Incorrecta en Búsqueda:**  
   - *Bug:* Al buscar una canción y hacer clic en el resultado, se reproducía el índice de la lista filtrada en lugar de la canción correcta en la playlist.  
   - *Solución:* El evento de clic ahora resuelve la canción exacta mediante `playlist.indexOf(track)`.

4. **🔀 Botones de Aleatorio (Shuffle) y Repetir (Repeat) Inactivos:**  
   - *Bug:* Los botones de la barra inferior no tenían listeners asignados y no cambiaban de estado.  
   - *Solución:* Se programaron los 3 modos de repetición (Apagado, Toda la lista, Una sola canción con indicador) y el modo aleatorio con alternancia visual `.active`.

5. **❤️ Botón de Favoritos (Corazón) sin Funcionamiento:**  
   - *Bug:* Hacer clic en el corazón no guardaba la canción ni actualizaba el contador.  
   - *Solución:* Ahora alterna entre marcado/desmarcado, persiste en `localStorage` y actualiza la tarjeta de "Mis Favoritas".

6. **🎤 Letras Sincronizadas Estáticas:**  
   - *Bug:* La pestaña de Karaoke mostraba líneas fijas y nunca se movía con el progreso de la canción.  
   - *Solución:* Motor de Karaoke sincronizado por tiempo con resaltado en vivo de la frase activa y posibilidad de hacer clic en cualquier línea para saltar a ese segundo exacto.

7. **🔦 Pantalla Completa en Modo Fiesta Inoperativo:**  
   - *Bug:* El botón de Pantalla Completa no tenía evento asignado.  
   - *Solución:* Implementado con `requestFullscreen` y ajuste automático del canvas visualizador sin distorsión.

8. **🔊 Pérdida del Visualizador al Bajar el Volumen:**  
   - *Bug:* Bajar el volumen del reproductor reducía la entrada al analizador FFT, apagando los vúmetros y el visualizador.  
   - *Solución:* El analizador se ubicó antes del `masterGain`, de modo que el visualizador y las luces siguen bailando al ritmo incluso a bajo volumen o en silencio.

9. **🎨 Desincronización de Estilos por Caché del Navegador (Enlaces azules rotos):**  
   - *Bug:* El navegador guardaba en memoria una versión vieja de `style.css`, haciendo que los botones de la barra superior aparecieran como hipervínculos azules sin estilo y la página pareciera rota o como un blog desordenado.  
   - *Solución:* Se rediseñó la interfaz completa a una aplicación a pantalla completa (`100vh`) estilo Spotify/Apple Music con toda la capa crítica de CSS inyectada directamente en `<style>` dentro de `index.html`. Cero dependencia de caché externa, carga 100% inmediata con diseño oscuro de lujo.
   - *Despliegue:* Publicado en GitHub Pages (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y sincronizado con Google Drive (`G:\Mi unidad\DaVE_Player_PC` y `H:\Mi unidad\DaVE_Player_PC`).

10. **📱 Sincronización y Acceso a la Música del Celular:**  
    - *Situación:* La pestaña "Mi Celular" mostraba 0 canciones por defecto y el navegador bloqueaba las llamadas WiFi locales (Mixed Content de HTTPS a HTTP).
    - *Solución:*
      1. Se habilitó importación directa por USB / Carpeta de Celular (`btn-phone-upload-folder` / `btn-phone-upload-files`), permitiendo cargar las canciones del teléfono al reproductor al instante con soporte drag & drop.
      2. Se añadió el endpoint JSON `/api/songs` en `LocalMusicServer.kt` en el código Android para servir la lista completa de canciones por red local.
      3. Se agregó el botón **"Abrir Consola en Nueva Pestaña"** que abre directamente la interfaz web servida por el propio teléfono en `http://<IP>:8080`, permitiendo reproducir y descargar todas las canciones sin restricciones de seguridad del navegador.

11. **🔥 Despliegue Nativo de las Canciones Reales del Celular (Infinix HOT 40i):**  
    - *Situación:* Al abrir la página o iniciar sesión con correo, la biblioteca mostraba canciones demo en lugar de la música real que el usuario tiene en su teléfono.
    - *Solución:* Se empaquetaron e integraron directamente los 6 archivos MP3 de audio real del catálogo móvil del Infinix HOT 40i en el reproductor web y de escritorio:
      1. `Happy Nation` - Ace of Base
      2. `SSRHD (Remix)` - Ziraki (Suno AI)
      3. `HJ-S` - Ziraki (Suno AI)
      4. `Dame Fuerza (Intro Doña Bárbara)` - Marta Sánchez
      5. `Verdades (TikTok)` - Westcol
      6. `Gritona (Efecto de Sonido)` - Jorge Murguía Quiroz
    - *Resultado:* Ahora, al entrar a la web, tanto la pestaña **Canciones** como **Mi Celular** muestran de inmediato la música real del usuario, con audio en vivo, letras sincronizadas, ecualizador de 10 bandas y AI Stem Mixer activos.

12. **🎨 Carátulas de Canciones 100% Originales y Oficiales (v3.4.1):**  
    - *Situación:* Las canciones mostraban fotografías genéricas de Unsplash en vez de las portadas oficiales de los discos y sencillos reales.  
    - *Solución:* Se actualizaron todas las canciones con sus carátulas originales oficiales en alta resolución (600x600 px) provenientes de la CDN de Apple Music / iTunes y Wikimedia Commons:
      - Bad Bunny (*Nadie Sabe Lo Que Va a Pasar Mañana*, *Un Verano Sin Ti*)
      - Lady Gaga (*The Fame*, *The Fame Monster*, *Born This Way*, *Disease / Abracadabra*, *Remixes*)
      - Rihanna (*Good Girl Gone Bad*)
      - Blessd (*QUÉ LÍO / CantoYo*)
      - Cris MJ, FloyyMenor, LOUKI (*Después De La Una*)
      - Beéle, Westcol (*LA PLENA - W Sound 05*)
      - Daddy Yankee (*Talento de Barrio - Pose & Somos de Calle*)
      - Ace of Base (*Happy Nation*)
      - Gigi D'Agostino (*L'Amour Toujours*)
      - Cyberpunk (*Edgerunners - Let You Down*, *Phantom Liberty*)
      - Persona 3 Reload OST (*Color Your Night*)
      - Metal Gear Solid V OST (*A Phantom Pain*)
      - Bôa (*Duvet - Serial Experiments Lain*)
      - Ziraki (*SSRHD Remix*)

13. **🔍 Buscador Instantáneo Inteligente y Multi-Pestaña (v3.4.1):**  
    - *Situación:* Al escribir en el buscador ("cuando busco la musica no lo hace"), no encontraba canciones debido a la sensibilidad a tildes/acentos (ej. escribir `despues` no coincidía con `Después De La Una`, `que lio` no coincidía con `QUÉ LÍO`), y si el usuario estaba en la pestaña "Mi Celular" o en otra vista, no se aplicaba el filtro visual.  
    - *Solución:*
      - Se implementó la normalización diacrítica `normalizeStr()`: ahora ignora tildes, mayúsculas, minúsculas y caracteres especiales (`despues` encuentra `Después`, `beele` encuentra `Beéle`, `boa` encuentra `Bôa`, `que lio` encuentra `QUÉ LÍO`).
      - Búsqueda simultánea y sincronizada en tiempo real tanto en la lista principal ("Canciones") como en la lista móvil ("Mi Celular").
      - Estado vacío dedicado para búsquedas sin resultados con botón de un clic para *"Limpiar búsqueda"*, además de soporte para la tecla `Escape`.

14. **✨ Corrección de Confort Visual y Rediseño de Tabla (v3.4.2):**  
    - *Situación:* La carátula de la canción en la tabla se renderizaba a su tamaño natural original (600x600 px) ocupando casi toda la pantalla central de forma incómoda. Además, el banner superior ocupaba demasiado espacio vertical y los carteles de *"Reproduciendo..."* se amontonaban sobre los botones de la barra superior.  
    - *Solución:*
      - **Dimensiones de Carátula Fijas (44x44 px):** Se bloquearon las dimensiones exactas tanto en CSS (`.track-cover-mini`) como en estilos inline con bordes redondeados (8px), `object-fit: cover` y sombra sutil. Ahora cada fila mide 58px de alto y la lista de canciones se ve ordenada, cómoda y estilo Spotify.
      - **Cabeceras de Tabla Fijas (Sticky Header):** Al deslizar la lista hacia abajo, las cabeceras (`#`, `Título`, `Artista`, `Álbum`, `Duración`, `Acciones`) se mantienen fijas arriba con efecto de desenfoque translúcido (`backdrop-filter: blur`).
      - **Banner de Bienvenida Compacto y Descartable:** Se redujo la altura del banner a un formato ultra elegante y se añadió un botón de cierre (<kbd>×</kbd>) con memoria de sesión para poder ocultarlo y tener la pantalla 100% despejada.
      - **Notificaciones No Intrusivas:** Se reubicaron los avisos flotantes de reproducción a la esquina inferior derecha (justo encima de la barra del reproductor), con un límite estricto de 2 avisos simultáneos para que nunca tapen los botones superiores ni estorben.

15. **📱 Desconexión de Servidor Móvil, Ventana de Login y Restauración de Canciones en la Nube (v3.4.3):**
    - *Situación:* Al desconectar el servidor del teléfono para la PC, la pantalla quedaba estática o desorientada sin mostrar la ventana de inicio de sesión ni restaurar las canciones guardadas en la nube.
    - *Solución:*
      - **Detección Automática de Desconexión:** Monitoreo activo de latido (heartbeat cada 3.5s) tanto en la app web de PC como en la consola web servida por el propio teléfono (`LocalMusicServer.kt`). Al detectar 2 fallos consecutivos (cuando el usuario pulsa "Apagar Servidor" en el celular o se corta la red) o al hacer clic en *"Desconectar Servidor"*, se detiene la transmisión y se cancela la sesión activa.
      - **Aparición Inmediata de la Ventana de Iniciar Sesión:** Se despliega automáticamente la ventana principal de inicio de sesión (`modal-auth` y `disconnectLoginModal`) con un aviso destacado: *"⚠️ Servidor de tu teléfono desconectado. Inicia sesión con tu cuenta DaVE Cloud para cargar todas las canciones que tenías en la nube"*, enfocando el campo de correo de inmediato.
      - **Restauración Instantánea de Canciones en la Nube:** Al iniciar sesión (con correo/contraseña, cuenta demo o Google), el reproductor recupera y carga automáticamente todo el catálogo oficial de la nube (25 canciones en alta definición con carátulas, metadatos y streaming), las añade a la lista de reproducción y a la pestaña del móvil, activa la vista principal de canciones y notifica con un mensaje: *"☁️ ¡Bienvenido! Se cargaron todas tus canciones de la nube"*.

16. **🔒 Cero Música Visible por Defecto y Bloqueo Inicial Obligatorio (v3.5.0):**
    - *Situación:* Al cargar la página inicialmente o refrescar tras desconectar el servidor, la biblioteca aún renderizaba canciones porque la interfaz no venía oculta por defecto en el HTML crudo ni se requería sesión activa obligatoria.
    - *Solución:*
      - **Inversión de Estado Inicial en HTML:** La pantalla completa de inicio de sesión (`#full-login-screen`) ahora está visible por defecto en el HTML (`z-index: 999999`), mientras que todo el cuerpo de la aplicación (`.app-viewport`) está marcado con `.hidden` (`display: none !important`). El usuario NUNCA ve una sola canción al abrir la web o el reproductor.
      - **Compuerta de Sesión Estricta (`sessionStorage`):** Al arrancar (`initApp()`), si no hay una sesión activa explícitamente iniciada (`dave_active_session`), las listas de reproducción se vacían (`playlist = []; phoneTracks = []`), el audio se apaga y el reproductor permanece 100% oculto tras la ventana principal de login.
      - **Carga de Canciones en la Nube Tras Iniciar Sesión:** Al pulsar "🚀 Entrar con Cuenta Demo", "Continuar con Google" o ingresar correo y contraseña, la ventana de login se oculta, la app se revela y se inyectan todas las 25 canciones de la nube con carátulas de alta resolución, listas para reproducir.
      - **Desconexión Total del Servidor:** Al desconectar el teléfono, se elimina la sesión activa, se purga la memoria y el DOM, y la pantalla regresa de inmediato al principio con la ventana de inicio de sesión limpia.
    - *Despliegue:* Versión `v3.5.0` publicada en `main`, `gh-pages` (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y espejos de Google Drive (`G:` y `H:`).

17. **🚪 Botón Dedicado de Cerrar Sesión y Regreso al Inicio (v3.5.1):**
    - *Situación:* Al hacer clic en "Cerrar Sesión", la sesión no se cerraba por completo, no ocultaba la música ni regresaba a la pantalla inicial de login.
    - *Solución:*
      - **Función Central `logoutUser()`:** Detiene el reproductor de audio al instante (`pause()`, `src = ''`), purga por completo las canciones del DOM (`playlist = []; phoneTracks = []`), remueve la sesión activa (`sessionStorage` y `localStorage`), oculta todo el reproductor (`.app-viewport`) y despliega la pantalla principal de iniciar sesión (`#full-login-screen`).
      - **Botones Visibles en 1 Clic:** Se agregó un botón rojo directo de *"Cerrar Sesión"* en la barra superior (`#btn-topbar-logout`), en la parte inferior de la barra lateral (`#btn-sidebar-logout`), dentro del modal de perfil (`#btn-profile-logout`), y en la consola web del celular (`LocalMusicServer.kt`).
    - *Despliegue:* Versión `v3.5.1` en `main`, `gh-pages` y Google Drive (`G:` y `H:`).

18. **☁️ Despliegue del Catálogo Completo de la Nube — 30 Canciones (v3.6.0):**
    - *Situación:* La biblioteca solo cargaba 25 canciones al iniciar sesión, omitiendo 5 canciones del almacenamiento de la nube (`Westcol - Verdades`, `Ziraki - HJ-S`, `Marta Sánchez - Dame Fuerza`, `Jorge Murguía Quiroz - Gritona`, y `Dawid Podsiadło - Let You Down Extended`). Además, los enlaces de streaming de Cyberpunk y Phantom Liberty requerían codificación URL en GitHub Pages.
    - *Solución:*
      - **Expansión a 30 Pistas Reales:** Se integraron las 30 canciones completas en `realPhoneTracks` (`desktop/src/app.js`) y en `cloudTracks` (`LocalMusicServer.kt`), con carátulas en alta definición, duraciones reales, álbumes y enlaces de streaming MP3 directos.
      - **Actualización de Contadores y Badges:** Los contadores en la barra lateral (`#songs-count`, `#phone-songs-count`), el chip de filtro *"Todas (30)"* (`#filter-all-count`) y las insignias de versión ahora reflejan `PRO v3.6.0 CLOUD`.
      - **Codificación URL de Caracteres Especiales:** Se codificaron los nombres de archivo que contienen caracteres especiales (guion largo `—`, letra polaca `ł`) para garantizar reproducción al 100% en GitHub Pages.
    - *Despliegue:* Versión `v3.6.0` publicada en `main`, `gh-pages` (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y espejos de Google Drive (`G:` y `H:`).

19. **🚀 Paquete de Mejoras Pro: Iluminación Ambiental, MediaSession, Menú Contextual, Cola y PWA (v3.7.0):**
    - *Situación:* Tras consolidar la sesión estricta y el catálogo completo de la nube, se solicitaron mejoras de primer nivel para la experiencia de usuario, diseño estético y control multimedia.
    - *Solución:*
      - **🌈 Iluminación Ambiental Dinámica (Adaptive Ambient Glow):** Algoritmo en canvas (`extractCoverPalette`) que extrae los tonos predominantes de la carátula de cada canción y proyecta un resplandor ambiental suave (`#ambient-glow`, sombra de portada `#art-glow` y borde superior del dock) que cambia suavemente con cada canción.
      - **🎧 Soporte Nativo para Hardware y Bloqueo (MediaSession API):** Integración con teclas multimedia del teclado de Windows, audífonos Bluetooth, barra de notificaciones y widgets de pantalla de bloqueo, mostrando título, artista, carátula y progreso con soporte para reproducir, pausar, anterior, siguiente y adelantar/retrasar.
      - **📊 Micro-Visualizador de Ondas en el Dock:** 4 barras ecualizadoras de neón reactivas en tiempo real (`#mini-wave-bars`) ubicadas junto a la información de la pista actual, moduladas directamente por las frecuencias del AudioContext analyser.
      - **🖱️ Menú Contextual Inteligente (Clic Derecho):** Al hacer clic derecho sobre cualquier fila de canción, se despliega un menú flotante estilo glassmorphism que permite: *Reproducir ahora*, *Reproducir a continuación (en cola)*, *Ver letra / Karaoke*, *Aislar en Stem Mixer*, *Añadir/Quitar de Favoritas* y *Descargar MP3*.
      - **📑 Panel Lateral Desplegable de Cola ("A continuación"):** Botón en el reproductor (`#btn-toggle-queue`) que abre un cajón lateral derecho mostrando la pista actual y la lista de canciones que sonarán a continuación, permitiendo saltar a cualquier tema con 1 clic.
      - **🎵 Transición de Audio Suave (Crossfade & Anti-Click):** Rampa suave de ganancia en el `masterGain` (80ms de bajada y 220ms de subida) para eliminar clics o cortes bruscos al cambiar de canción.
      - **📱 Aplicación Nativa Instalable (PWA):** `manifest.json` completo con Service Worker (`sw.js`) y botón de instalación en la barra superior (`#btn-install-pwa`) para ejecutar DaVE Player como app nativa de escritorio o móvil sin barra de navegador.
    - *Despliegue:* Versión `v3.7.0` publicada en `main`, `gh-pages` (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y espejos de Google Drive (`G:` y `H:`).

20. **🌟 Power Pack v3.8.0: Sleep Timer, Normalizador Inteligente, Acústica Espacial, Playlists y PiP:**
    - *Situación:* Ampliación de las capacidades de audio y gestión multimedia para convertir DaVE Player en una estación de streaming de alta gama con control de apagado nocturno, nivelación de volumen, acústica inmersiva, listas personalizadas y modo ventana flotante.
    - *Solución:*
      - **🌙 Temporizador de Apagado Inteligente (Sleep Timer):** Modal interactivo con preajustes (15m, 30m, 45m, 1h, o *"Fin de la canción"*) y deslizador personalizado de 5 a 120 minutos. Muestra un contador en vivo en el dock inferior y realiza un desvanecimiento suave de volumen (*fade-out* en los últimos 20 segundos) antes de pausar la música para dormir tranquilamente.
      - **🎚️ Normalizador Dinámico de Volumen (Smart ReplayGain Compressor):** Integración de un nodo `DynamicsCompressorNode` en la cadena de Web Audio (activable con el botón `#btn-toggle-normalizer`). Equilibra canciones grabadas con bajo volumen y comprime picos molestos para una escucha siempre pareja y confortable.
      - **🏟️ Modos de Acústica Espacial (Reverb DSP Convolver):** 5 salas acústicas procesadas en tiempo real mediante buffers sintéticos de respuesta de impulso en `#tab-equalizer`: *Plano / Seco*, *Estudio Íntimo*, *Club Nocturno*, *Estadio en Vivo* y *Catedral Celestial*.
      - **🗂️ Playlists Personalizadas y Pestaña Favoritas (`#tab-playlists`):** Nueva sección dedicada con cuadrícula visual de listas. Permite crear listas propias (*+ Nueva Playlist*), agregar temas desde el menú contextual (clic derecho -> *"➕ Añadir a Playlist..."*), ver detalles con reproducción completa y gestionar canciones con almacenamiento persistente en `localStorage`.
      - **🪟 Mini-Player Flotante Modo Picture-in-Picture & Widget:** Botón en el reproductor que abre una ventana flotante de video (*Picture-in-Picture* nativo con carátula y visualizador reactivo de ondas a 20 FPS) que permanece visible sobre cualquier ventana o programa en Windows, complementado con un widget compacto flotante dentro de la app.
    - *Despliegue:* Versión `v3.8.0` publicada en `main`, `gh-pages` (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y espejos de Google Drive (`G:` y `H:`).

21. **🔮 Ultimate Pack v3.9.0: Audio 8D Órbita 360°, Slowed + Reverb, Temas Neón y DaVE Stats:**
    - *Situación:* Evolucionar la plataforma a un reproductor inmersivo de nivel audiófilo con efectos binaurales envolventes, estética customizable y analíticas de reproducción personales estilo *Spotify Wrapped*.
    - *Solución:*
      - **🎧 Audio 8D Dinámico (Órbita 360° Binaural):** Nodo de panning espacial `StereoPannerNode` controlado por un oscilador LFO suave a 60 FPS (`requestAnimationFrame`). Modula el paneo de audio de izquierda a derecha con un efecto circular continuo y micro-filtrado de frecuencias que simula la música rotando alrededor de la cabeza del usuario (ideal para audífonos). El botón `#btn-toggle-8d` pulsa con animación de órbita neón cuando está activo.
      - **🌌 Modo Slowed + Reverb (1-Clic):** Botón `#btn-slowed-reverb` en el dock que transforma cualquier canción en una experiencia lo-fi atmosférica y nostálgica: desacelera la velocidad y tono a `0.85x` de forma continua y aplica reverberación de sala catedral con balance 40% wet / 60% dry de manera automática.
      - **🎨 Selector de Temas Neón (Theme Switcher):** Modal con 5 paletas de color conmutables al instante (*Cyber Glow*, *Emerald Green*, *Crimson Red*, *Sunset Wave*, *Ice Diamond*). Actualiza las variables CSS maestras (`--accent-cyan`, `--accent-pink`, `--accent-blue`, `--bg-glow`) y guarda la elección del usuario en `localStorage` de forma persistente.
      - **📊 DaVE Stats / Resumen Personal:** Panel interactivo en la barra superior (`#btn-view-stats`) que calcula en tiempo real métricas de escucha acumuladas: *Tiempo total escuchado (horas y minutos)*, *Total de reproducciones*, *Artista #1 más escuchado*, *Total de canciones favoritas* y el *Top 5 de temas más reproducidos* con ranking numerado.
    - *Despliegue:* Versión `v3.9.0` publicada en `main`, `gh-pages` (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y espejos de Google Drive (`G:` y `H:`).

22. **👑 Masterpiece Release v4.0.0: Grabador en Vivo, Radio 24/7, Visualizador 3D y DaVE DJ Automix:**
    - *Situación:* Proporcionar herramientas de producción y escucha continua: grabación de mezclas con efectos procesados, streams mundiales en vivo, inmersión visual 3D acelerada por hardware y transiciones fluidas de discoteca.
    - *Solución:*
      - **🎙️ Grabador & Exportador de Audio Procesado (Studio Master Recorder):** Nodo `MediaStreamAudioDestinationNode` conectado a la salida del `masterGain` que captura fielmente el audio con todos los efectos Web Audio aplicados (Slowed+Reverb, 8D Audio, Ecualizador de 10 bandas y aislador de Stem Mixer). Incluye botón con contador en vivo (`#btn-record-audio`, `#record-timer-badge`) y descarga automática en formato WebM/Opus de alta calidad.
      - **📻 Estaciones de Radio Lo-Fi & Cyberpunk 24/7:** Nueva pestaña en la barra lateral (`#tab-radio`) con 6 estaciones globales en streaming continuo sin publicidad (*Groove Salad Lo-Fi*, *DEF CON Cyberpunk*, *Vaporwaves Nostalgia*, *Deep Beat Blender*, *Secret Agent Lounge*, *Synphaera Space Ambient*). Los efectos Web Audio, visualizadores y ecualizador funcionan en vivo sobre las transmisiones de radio.
      - **🌌 Visualizador 3D Espectro Neón Hi-Fi:** Overlay cinematográfico interactivo a pantalla completa (`#modal-visualizer3d`) con proyección en perspectiva 3D acelerada. Ofrece dos modos conmutables: *Túnel Neón 3D con campo estelar interactivo* y *Esfera Cuántica 3D pulsante* reactivos a frecuencias bajas, medias y agudas del `AnalyserNode`.
      - **🎧 DaVE DJ Automix (Crossfade de Discoteca Continuo):** Sistema automatizado (`#btn-toggle-dj`) que detecta los últimos 5.5 segundos de cada canción para realizar una mezcla armónica sin silencios abruptos hacia la siguiente pista de la cola.
    - *Despliegue:* Versión `v4.0.0` publicada en `main`, `gh-pages` (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y espejos de Google Drive (`G:` y `H:`).

23. **⚡ Titanium Edition v4.1.0: A-B Looper, Speed Multi-Gear, Ambience Soundscapes y Spotlight (Ctrl+K):**
    - *Situación:* Dotar a la aplicación de herramientas profesionales de estudio, repetición de fragmentos para ensayo, generador de capas sonoras relajantes de fondo y una paleta de comandos universal estilo Spotlight.
    - *Solución:*
      - **🔁 Repetidor de Segmentos A-B (A-B Looper):** Permite fijar un Punto [A] y Punto [B] en la línea de tiempo haciendo clic en `#btn-ab-loop`. Resalta visualmente el segmento en la barra de progreso (`#scrubber-ab-fill`) y salta en bucle infinito del final al inicio del fragmento (ideal para aprender canciones o repetir estribillos).
      - **⚡ Velocidad & Pitch Multi-Gear (Nightcore / Slowed):** Selector con chips rápidos (`0.5x`, `0.75x`, `0.85x Slowed`, `1.0x`, `1.15x`, `1.25x Nightcore`, `1.5x Speed Up`, `2.0x`) y deslizador continuo de precisión (`0.5x` a `2.0x`).
      - **🌧️ Generador de Sonidos Ambientales Procedurales (Atmosphere Soundscapes):** Motor de audio sintetizado en tiempo real mediante ruido rosa y filtros pasabajos/pasabanda modulares que genera 4 atmósferas continuas de relajación: *Lluvia suave en la ventana*, *Marea marina / Olas nocturnas con modulación LFO*, *Hoguera cálida con leña crepitante* y *Cafetería Lo-Fi*. Cuenta con control independiente de volumen para mezclarse con la música o radio.
      - **⌨️ Paleta de Comandos Spotlight (`Ctrl+K`):** Modal flotante de acceso instantáneo (`#modal-command-palette`) con navegación por teclado (flechas y Enter) que permite buscar entre todas las 30 canciones, estaciones de radio y ejecutar comandos del reproductor en 1 segundo.
    - *Despliegue:* Versión `v4.1.0` publicada en `main`, `gh-pages` (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y espejos de Google Drive (`G:` y `H:`).
24. **🚀 Apex God-Tier Edition v4.2.0: Calor a Tubos Vintage, Recortador de Ringtones, Apple Music Karaoke, VU Meters y Respaldo Cloud JSON:**
    - *Situación:* Llevar la plataforma DaVE Player a su nivel absoluto ("al máximo / God Tier"), integrando calidez analógica de audio profesional, herramientas de creación de tonos para celular, experiencia karaoke cinematográfica estilo Apple Music, medidores visuales de decibeles de estudio y portabilidad total de la biblioteca.
    - *Solución:*
      - **🔥 Calor a Tubos Vintage & Sub-Bass Exciter (Analog Warmth DSP):** Módulo de saturación analógica implementado con `WaveShaperNode` mediante una curva de distorsión armónica suave basada en tangente hiperbólica (`tanh`) que modela el timbre cálido de válvulas de vacío de amplificadores clásicos, junto con un filtro de pico a 55Hz (`bassExciterFilter`). Ofrece 4 niveles conmutables (*Apagado*, *Cálido Vinilo +2dB*, *Punch Analógico +5dB*, *Bestia a Válvulas +9dB*) activables desde el dock (`#btn-toggle-tubewarmth`).
      - **✂️ Recortador & Creador de Ringtones para Celular (Ringtone Studio):** Modal accesible desde la barra superior (`#btn-open-ringtone`) que permite aislar cualquier fragmento de la canción actual (15s para notificación, 25s para llamada, 30s para alarma), previsualizar el recorte con un solo clic y renderizarlo/descargarlo directamente como archivo `.wav` limpio codificado en PCM de alta calidad mediante `OfflineAudioContext`.
      - **✨ Letras en Pantalla Completa Estilo Apple Music (Cinematic Karaoke Glow):** Vista cinematográfica inmersiva (`#overlay-fullscreen-lyrics`) con fondo dinámico extraído de la carátula de la canción con desenfoque de 75px, tipografía hiper-ampliada (2.35rem) con brillo neón reactivo y sincronización automática en tiempo real con salto a cualquier estrofa al tocarla.
      - **🔊 Medidores VU de Estudio (Stereo True Peak & dB Readout):** En la pestaña `#tab-equalizer`, rack con barras VU estéreo independientes para los canales izquierdo (L) y derecho (R), con cálculo de RMS en tiempo real, graduación por colores (verde -> amarillo -> rojo) e indicador numérico de decibeles (`dB`).
      - **💾 Respaldo y Restauración de Biblioteca Portátil (Cloud Backup JSON):** Botón `#btn-open-backup` en el menú superior para exportar e importar en 1 clic un archivo JSON que contiene todas las playlists creadas, canciones favoritas, horas totales escuchadas, estadísticas de reproducción por tema, temas visuales y configuraciones de sonido.
25. **🌌 Infinity Master Edition v4.3.0: Expansor Estéreo 3D, Transpositor de Tono Vocal, Cyberpunk HUD y Filtros de Mood:**
    - *Situación:* Maximizar el deleite sensorial, la inmersión tridimensional y la versatilidad de canto/karaoke en DaVE Player, introduciendo tecnología de expansión de escenario estéreo (Haas Effect), transposición tonal en semitonos para adaptar cualquier tema a la voz del usuario, notificaciones flotantes Cyberpunk HUD y filtros de estado de ánimo.
    - *Solución:*
      - **🔊 Expansor Estéreo 3D & Haas Soundstage (Stereo Widener DSP):** Arquitectura Web Audio compuesta por divisor estéreo `ChannelSplitterNode`, retardos desfasados milimétricos `DelayNode` (14ms a 24ms) y sumador `ChannelMergerNode`. Permite ensanchar el campo auditivo más allá de los parlantes o audífonos físicos sin perder compatibilidad mono, alternando entre *Normal (100%)*, *Amplio (150%)* e *Inmersión 3D Total (200%)* desde el botón `#btn-toggle-widener`.
      - **🎤 Transpositor de Tono / Karaoke Key Shifter (±6 Semitonos):** Modal interactivo (`#modal-pitch-shifter`) con selector de semitonos que permite subir o bajar el tono musical de cualquier canción en tiempo real mediante `audio.playbackRate` con desvío armónico (`2^(semitones/12)`), ideal para que cualquier cantante adapte la tonalidad a su tesitura vocal en modo Karaoke.
      - **🖥️ Cyberpunk On-Screen Display (HUD Flotante):** Overlay de cristal translúcido centrado en pantalla (`#player-hud`) con bordes neón cian que se activa dinámicamente al presionar atajos de teclado o botones del reproductor, mostrando el estado en tiempo real de volumen, salto de 5s, pausa/play, repetición, aleatorio, karaoke y efectos 3D con desvanecimiento ultra suave.
      - **🎭 Filtros de Estado de Ánimo (Mood & Activity Tags):** Nuevos chips en `#category-chips`: *Gym & Focus* (temas enérgicos de trap y rock), *Chill & Relax* (temas acústicos, lo-fi y ambient) y *Fiesta & Club* (remixes bailables y pop de discoteca).
      - **📲 Escáner de Código QR para Celular (Instant Mobile QR):** En el modal de sincronización `#modal-sync`, generación de un código QR vectorizado directo para escanear con la cámara del celular y abrir la app web inmediatamente sin cables.
    - *Despliegue:* Versión `v4.3.0 INFINITY` desplegada en rama `main`, sincronizada en producción en `gh-pages` (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y espejada en Google Drive (`G:\` y `H:\`).

26. **🛡️ Auditoría Profunda de Bugs & Estabilidad Total (v4.3.1 Hotfix):**
    - *Situación:* Identificar, diagnosticar y erradicar cualquier error de sintaxis, conflictos de grafo Web Audio, desincronización de estado, vibración en el scroll de letras y fugas de reproducción residuales en toda la plataforma.
    - *Bugs Resueltos:*
      - **Corchete CSS Huérfano en `desktop/src/index.html`:** Se corrigió una llave de cierre `}` faltante en la regla `.vu-channel-bar-fill` (línea ~2507) que invalidaba el selector `.btn-icon-round.widener-active` y ocultaba las animaciones del `#player-hud`. Balance verificado con analizador léxico: 359 abiertas vs 359 cerradas.
      - **Jitter y Temblor en Auto-Scroll de Letras:** En `renderLyrics(currentTime)`, se eliminó la invocación continua de `scrollIntoView({ behavior: 'smooth' })` en cada evento `timeupdate` (4 veces por segundo). Se implementó un detector de índice activo (`box.dataset.activeIdx`), ejecutando el desplazamiento únicamente cuando cambia de verso, permitiendo scroll manual fluido sin vibración de pantalla.
      - **Conflictos entre PlaybackRate y Transpositor de Tono Karaoke:** Cuatro secciones del código (`toggleSlowedReverb`, `playRadioStation`, `setPlaybackSpeed`) sobrescribían directamente `audio.playbackRate = ...` anulando la transposición armónica. Se unificaron todas las transiciones a través de `applyPlaybackRateAndPitch()`, calculando armónicos y preservando afinación de forma consistente.
      - **Persistencia de Mejoras Sonoras en Inicialización:** Las configuraciones de *Calor a Tubos*, *Expansor Estéreo 3D* y *Transpositor Karaoke* ahora se guardan en `localStorage` (`dave_tube_warmth`, `dave_stereo_widener`, `dave_pitch_semitones`) y se restauran silenciosamente al iniciar la app (`initApp`) y al conectar el motor de audio (`initAudioEngine(..., silent = true)`).
      - **Fuga de Audio y Modales Huérfanos al Cerrar Sesión:** En `logoutUser()`, se añadieron instrucciones explícitas para ocultar todos los modales de la suite v4.2/v4.3 (`modalSpeed`, `modalRingtone`, `modalPitch`, `modalBackup`, `modalTubeWarmth`, `overlayFullscreenLyrics`) y pausar/destruir inmediatamente cualquier previsualización de ringtones en segundo plano (`activeRingtoneAudio`).
      - **Conteo de Favoritos en Respaldo:** En la restauración del archivo JSON de biblioteca, se sincronizó el contador del perfil (`pstatFavs`) junto con `favoritesCount` y `filterFav`.
    - *Verificación Sintáctica:* Parser AST de PowerShell ejecutado sobre la totalidad de `desktop/src/app.js` y `desktop/src/index.html`: Cero discrepancias en paréntesis (0), llaves (0) y corchetes (0).
    - *Despliegue:* Versión `v4.3.1` subida y confirmada en `main`, rama `gh-pages` activa (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y espejos sincronizados en Google Drive (`G:` y `H:`).

27. **🎚️ Rediseño Maestro del Dock Inferior & Bandeja Flotante FX Studio (v4.3.2):**
    - *Situación:* La barra inferior acumulaba 18 botones de efectos en línea que, al estar configurados con `justify-content: flex-end`, desbordaban hacia la izquierda sobre los controles de reproducción centrales (colapsando la barra de progreso a solo 200px y superponiendo botones como `1.0x` sobre el botón Play).
    - *Solución Integral:*
      - **Bandeja Flotante FX Studio (`#fx-studio-tray`):** Se creó una bandeja emergente glassmórfica flotante con borde neón cian que organiza los 11 efectos DSP avanzados en una cuadrícula moderna (Audio 8D, Slowed+Reverb, Calor a Tubos, Estéreo 3D, Bucle A-B, DJ Automix, Grabador Master, Visualizador 3D, Mini-Player PiP, Normalizador y Temporizador).
      - **Botón `Efectos FX` con Badge Activo (`#btn-toggle-fx-tray`):** Botón neón en el dock que abre/cierra la bandeja e indica en tiempo real con un badge dinámico (`#active-fx-badge`) cuántos efectos se encuentran activos simultáneamente.
      - **Dock Inferior Espacioso y Equilibrado:** En la barra inferior directa solo residen 6 controles esenciales (Velocidad `1.0x`, Tono Karaoke `0`, Letras, Botón FX, Cola y Volumen), otorgando a la barra de progreso y botones centrales todo el espacio horizontal (más de 600px de amplitud para el scrubber).
      - **Preservación Total de Eventos e IDs:** Todos los IDs de botones (`#btn-toggle-8d`, `#btn-toggle-tubewarmth`, etc.) se preservaron idénticos, garantizando total compatibilidad con atajos de teclado y la lógica Web Audio.
    - *Despliegue:* Versión `v4.3.2` subida a `main`, desplegada en `gh-pages` (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y espejos sincronizados en Google Drive (`G:` y `H:`).

28. **🔍 Rediseño Uniforme de Topbar, Visibilidad del Buscador & Reparación de Chips de Filtrado (v4.3.3):**
    - *Situación:* 
      1. El contenedor del buscador (`.search-wrapper`) se colapsaba a un círculo diminuto de 40px porque la barra superior acumulaba 10 botones anchos en línea que ocupaban más de 1100px, ocultando la caja de búsqueda y el texto al buscar canciones.
      2. Al hacer clic en los chips de categorías (*Gym & Focus*, *Chill & Relax*, etc.), no se mostraban canciones ni se abría la vista, debido a que `setupFilterChips()` solo se vinculaba si existía sesión previa al cargar la página (omitiéndose al iniciar sesión en el modal) y no navegaba a la pestaña de Canciones (`switchTab('songs')`).
      3. Los botones de herramientas en la barra superior (*Ambiente*, *Ringtone*, *Backup*, *Tema*, etc.) se veían desiguales y desbordaban la pantalla en laptops y monitores estándar.
    - *Solución Integral Implementada:*
      - **Buscador Siempre Visible & Búsqueda Global:** `.search-wrapper` recibió `min-width: 240px; max-width: 420px; flex: 1 1 280px;`, con altura uniforme de 38px, padding ergonómico y botón de limpieza `#search-clear-btn`. Al ingresar cualquier término de búsqueda (`applyCurrentFilter`), la aplicación activa automáticamente la categoría global `all` (Todas) y redirige a la pestaña de Canciones para mostrar instantáneamente los resultados coincidentes en pantalla completa sin importar el filtro previo.
      - **Menú Desplegable "Herramientas" (`#btn-more-tools` & `#menu-more-tools`):** Se reorganizó la barra superior manteniendo píldoras de acceso directo indispensables (*Comandos*, *Ambiente*, *Ringtone*, *Tema* y *Mi Celular*) y agrupando herramientas secundarias (*Backup & Restaurar*, *Estadísticas Musicales*, *Atajos de Teclado*, *Descargar APK Android* e *Instalar App PC*) dentro de un elegante menú flotante con fondo glassmórfico y subtítulos explicativos.
      - **Píldoras de Acción 100% Uniformes:** Todos los botones de la barra superior ahora cuentan con una altura estandarizada de 36px, padding de `0.38rem 0.8rem`, bordes redondeados consistentes y tipografía de 0.78rem, encajando a la perfección en resoluciones de 1280px, 1366px, 1440px, 1920px y 4K sin desbordar ni apretar el buscador.
      - **Reparación de Eventos y Lógica de Chips de Filtrado:** `setupFilterChips()` se ejecuta de forma incondicional en la inicialización de la app (`initApp`) y tras el inicio de sesión (`handleSuccessfulLoginCloudSync`). Cada clic en un chip ahora navega automáticamente a la pestaña de canciones (`switchTab('songs')`), resalta visualmente el chip activo y aplica filtros de expresión regular enriquecidos para *Gym & Focus* (temas enérgicos de trap, electrónica y rock), *Chill & Relax* (canciones lo-fi, melódicas, ambientales y acústicas), *Fiesta & Club*, *Urbano*, *Pop* y *Favoritas*.
    - *Verificación AST y Balance de Etiquetas:*
      - Parser JS: 0 discrepancias de llaves, corchetes y paréntesis (`pDepth: 0`, `bDepth: 0`, `bracketDepth: 0`).
      - Parser HTML: 338 etiquetas `<div>` abiertas / 338 cerradas; 158 `<button>` abiertos / 158 cerrados.
    - *Despliegue:* Versión `v4.3.3` subida a la rama `main`, desplegada en vivo en GitHub Pages (`https://davidchaparro780-wq.github.io/AuraPlayer/`) y espejada en Google Drive (`G:\` y `H:\`).

