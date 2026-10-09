const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const mm = require('music-metadata');

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    minWidth: 850,
    minHeight: 580,
    frame: false,
    titleBarStyle: 'hidden',
    backgroundColor: '#0a0b10',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: false // allow local audio file protocol
    },
    icon: path.join(__dirname, 'assets', 'icon.png')
  });

  mainWindow.loadFile(path.join(__dirname, 'src', 'index.html'));
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', function () {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', function () {
  if (process.platform !== 'darwin') app.quit();
});

// Window controls
ipcMain.on('window-minimize', () => mainWindow?.minimize());
ipcMain.on('window-maximize', () => {
  if (mainWindow?.isMaximized()) {
    mainWindow.unmaximize();
  } else {
    mainWindow?.maximize();
  }
});
ipcMain.on('window-close', () => mainWindow?.close());

// Open folder dialog
ipcMain.handle('dialog:open-folder', async () => {
  const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
    properties: ['openDirectory']
  });
  if (canceled || filePaths.length === 0) return null;
  return filePaths[0];
});

// Open files dialog
ipcMain.handle('dialog:open-files', async () => {
  const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
    properties: ['openFile', 'multiSelections'],
    filters: [
      { name: 'Audio Files', extensions: ['mp3', 'flac', 'wav', 'm4a', 'aac', 'ogg', 'opus'] }
    ]
  });
  if (canceled || filePaths.length === 0) return [];
  return filePaths;
});

// Scan folder for audio files
const SUPPORTED_EXT = ['.mp3', '.flac', '.wav', '.m4a', '.aac', '.ogg', '.opus'];

async function scanDirectory(dirPath) {
  let results = [];
  try {
    const list = await fs.promises.readdir(dirPath, { withFileTypes: true });
    for (const item of list) {
      const fullPath = path.join(dirPath, item.name);
      if (item.isDirectory()) {
        const sub = await scanDirectory(fullPath);
        results = results.concat(sub);
      } else if (item.isFile()) {
        const ext = path.extname(item.name).toLowerCase();
        if (SUPPORTED_EXT.includes(ext)) {
          results.push(fullPath);
        }
      }
    }
  } catch (err) {
    console.error('Error scanning folder:', err);
  }
  return results;
}

ipcMain.handle('fs:scan-folder', async (event, folderPath) => {
  const files = await scanDirectory(folderPath);
  return files;
});

// Extract metadata from file
ipcMain.handle('fs:parse-metadata', async (event, filePath) => {
  try {
    const metadata = await mm.parseFile(filePath, { skipCovers: false });
    let coverUrl = null;
    if (metadata.common.picture && metadata.common.picture.length > 0) {
      const pic = metadata.common.picture[0];
      const base64 = Buffer.from(pic.data).toString('base64');
      coverUrl = `data:${pic.format};base64,${base64}`;
    }
    return {
      title: metadata.common.title || path.basename(filePath, path.extname(filePath)),
      artist: metadata.common.artist || 'Artista Desconocido',
      album: metadata.common.album || 'Álbum Desconocido',
      duration: metadata.format.duration || 0,
      genre: metadata.common.genre ? metadata.common.genre.join(', ') : '',
      year: metadata.common.year || '',
      coverUrl: coverUrl,
      path: filePath
    };
  } catch (err) {
    return {
      title: path.basename(filePath, path.extname(filePath)),
      artist: 'Artista Desconocido',
      album: 'Álbum Desconocido',
      duration: 0,
      coverUrl: null,
      path: filePath
    };
  }
});
