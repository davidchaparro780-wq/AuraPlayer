# Walkthrough — DaVE v1.8.7: Nuevo Ícono Gótico, Bóveda Segura con Clave de Restauración y Motor TikTok Ultra Rápido

## 🚀 Resumen de Cambios en la Versión 1.8.7

---

### 1. 🎨 Nuevo Ícono Oficial de la App (Letra Gótica 'D')
- **Generación en todas las densidades**:
  - `mipmap-mdpi`: 48x48 px
  - `mipmap-hdpi`: 72x72 px
  - `mipmap-xhdpi`: 96x96 px
  - `mipmap-xxhdpi`: 144x144 px
  - `mipmap-xxxhdpi`: 192x192 px
  - `drawable/ic_dave_foreground.png`: 432x432 px
- **Fondo adaptable**: Ajustado a `#FFFFFF` puro para coincidir con la imagen original.
- **Zona Segura (Safe-Zone)**: Centrado y escalado al 82% para asegurar que los lanzadores con máscaras circulares o cuadradas no recorten los ornamentos ni la flecha del diseño.

---

### 2. 🔒 Bóveda Privada: Eliminación de Galería y Restauración con Clave
- **Desocultar solo con Contraseña**:
  - Ahora al presionar **"🔓 Desocultar (Restaurar a Galería)"**, la app abre un diálogo de seguridad solicitando el **PIN de 4 dígitos**.
  - Solo si la clave coincide con la registrada, el archivo se transfiere de vuelta a la galería pública (`Movies/` o `Pictures/`).
- **Eliminación Total de la Galería Principal**:
  - Se crearon archivos `.nomedia` en `.secure_vault`, `.secure_vault/videos` y `.secure_vault/photos`.
  - El archivo original se borra físicamente y se elimina su registro en `MediaStore`.
  - Se ejecuta un refresco forzado con `MediaScannerConnection` para que la galería del teléfono actualice su caché y el archivo desaparezca de inmediato de Google Photos / Galería.
- **Permisos de Archivos (Android 11+)**:
  - Se incluyó banner y acceso directo para conceder el permiso de "Administrar todos los archivos" si el sistema lo requiere.
- **Navegación Volver Corregida**:
  - Se implementó `BackHandler { onBack() }` en `VaultScreen` para que el gesto o botón de volver en el celular regrese limpiamente a la app sin cerrarla.

---

### 3. ⚡ Motor de Búsqueda y TikTok Ultra Rápido
- **Cero Esperas en Tendencias TikTok**: Catálogo de los temas más virales de TikTok (FloyyMenor, Feid, Karol G, Trueno, Bad Bunny, Cris Mj, Sabrina Carpenter, etc.) con carátulas oficiales en alta resolución (500x500 px) que cargan al instante (0 ms).
- **Consultas Concurrentes**: Deezer, TikTok y Jamendo corren en hilos paralelos (`async`) con un tiempo límite de 3.5 segundos, eliminando el congelamiento del spinner.
- **Audios Completos**: Extracción de enlaces de TikTok con su duración real sin cortes artificiales.
- **Lupa Clicable**: El ícono de búsqueda ahora es un botón táctil y el botón (X) permite limpiar la búsqueda y regresar a tendencias en milisegundos.

---

### 📦 Disponibilidad
- **Google Drive**: `G:\Mi unidad\DaVE-v1.8.7.apk`
