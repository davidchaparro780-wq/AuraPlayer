const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  minimize: () => ipcRenderer.send('window-minimize'),
  maximize: () => ipcRenderer.send('window-maximize'),
  close: () => ipcRenderer.send('window-close'),
  openFolderDialog: () => ipcRenderer.invoke('dialog:open-folder'),
  openFilesDialog: () => ipcRenderer.invoke('dialog:open-files'),
  scanFolder: (folderPath) => ipcRenderer.invoke('fs:scan-folder', folderPath),
  parseMetadata: (filePath) => ipcRenderer.invoke('fs:parse-metadata', filePath)
});
