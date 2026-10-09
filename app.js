// DaVE Player Desktop — Core Engine & UI Logic

// State
let playlist = [];
let currentIndex = -1;
let isPlaying = false;
let isMuted = false;
let previousVolume = 0.8;
let isShuffle = false;
let repeatMode = 0; // 0: off, 1: all, 2: one
let favorites = new Set();
let scannedFolders = new Set();

// Web Audio API Context & Nodes
let audioCtx = null;
let sourceNode = null;
let analyserNode = null;
let eqFilters = [];
let stemNodes = {
  vocals: { filter: null, gain: null },
  drums: { filter: null, gain: null },
  bass: { filter: null, gain: null },
  melodies: { filter: null, gain: null }
};
let masterGain = null;

// DOM Elements
const audio = document.getElementById('audio-player');
const playBtn = document.getElementById('btn-play-pause');
const prevBtn = document.getElementById('btn-prev');
const nextBtn = document.getElementById('btn-next');
const shuffleBtn = document.getElementById('btn-shuffle');
const repeatBtn = document.getElementById('btn-repeat');
const progressContainer = document.getElementById('progress-container');
const progressFill = document.getElementById('progress-fill');
const currentTimeEl = document.getElementById('current-time');
const totalTimeEl = document.getElementById('total-time');
const volumeSlider = document.getElementById('volume-slider');
const muteBtn = document.getElementById('btn-mute');
const playerTitle = document.getElementById('player-title');
const playerArtist = document.getElementById('player-artist');
const playerArt = document.getElementById('player-art');
const artGlow = document.getElementById('art-glow');
const searchInput = document.getElementById('search-input');
const trackTable = document.getElementById('track-table');
const trackList = document.getElementById('track-list');
const emptyState = document.getElementById('empty-state');
const songsCount = document.getElementById('songs-count');
const favoritesCount = document.getElementById('favorites-count');
const btnLike = document.getElementById('btn-like');
const foldersList = document.getElementById('folders-list');

// Window Controls
document.getElementById('btn-minimize')?.addEventListener('click', () => window.electronAPI?.minimize());
document.getElementById('btn-maximize')?.addEventListener('click', () => window.electronAPI?.maximize());
document.getElementById('btn-close')?.addEventListener('click', () => window.electronAPI?.close());

// Tab Switching
document.querySelectorAll('.nav-item').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    const tabId = `tab-${btn.dataset.tab}`;
    const target = document.getElementById(tabId);
    if (target) target.classList.add('active');
  });
});

// Quick bottom buttons
document.getElementById('btn-quick-stem')?.addEventListener('click', () => switchTab('stem-mixer'));
document.getElementById('btn-quick-lyrics')?.addEventListener('click', () => switchTab('lyrics'));
document.getElementById('btn-quick-eq')?.addEventListener('click', () => switchTab('equalizer'));

function switchTab(name) {
  const btn = document.querySelector(`.nav-item[data-tab="${name}"]`);
  if (btn) btn.click();
}

// Initialize Web Audio Engine
function initAudioEngine() {
  if (audioCtx) return;
  const AudioContext = window.AudioContext || window.webkitAudioContext;
  audioCtx = new AudioContext();

  sourceNode = audioCtx.createMediaElementSource(audio);
  analyserNode = audioCtx.createAnalyser();
  analyserNode.fftSize = 512;
  analyserNode.smoothingTimeConstant = 0.8;

  // 10-Band Equalizer Setup
  const eqFrequencies = [31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000];
  eqFilters = eqFrequencies.map((freq, i) => {
    const filter = audioCtx.createBiquadFilter();
    if (i === 0) {
      filter.type = 'lowshelf';
    } else if (i === eqFrequencies.length - 1) {
      filter.type = 'highshelf';
    } else {
      filter.type = 'peaking';
      filter.Q.value = 1.4;
    }
    filter.frequency.value = freq;
    filter.gain.value = 0;
    return filter;
  });

  // Chain EQ filters in series
  let lastNode = sourceNode;
  eqFilters.forEach(f => {
    lastNode.connect(f);
    lastNode = f;
  });

  // Master Gain & Analyser
  masterGain = audioCtx.createGain();
  masterGain.gain.value = volumeSlider.value / 100;

  lastNode.connect(analyserNode);
  analyserNode.connect(masterGain);
  masterGain.connect(audioCtx.destination);

  // Init EQ Sliders UI
  renderEqualizerUI(eqFrequencies);
  // Start Visualizer Loop
  startVisualizerLoop();
}

// Render Equalizer UI
function renderEqualizerUI(freqs) {
  const container = document.getElementById('eq-bands');
  if (!container) return;
  container.innerHTML = '';
  freqs.forEach((freq, idx) => {
    const col = document.createElement('div');
    col.className = 'eq-slider-col';
    const label = freq >= 1000 ? `${freq / 1000}k` : `${freq}`;
    col.innerHTML = `
      <div class="slider-wrapper" style="height: 160px;">
        <input type="range" min="-12" max="12" value="0" step="0.5" class="vertical-slider eq-slider" data-index="${idx}">
      </div>
      <span>${label}Hz</span>
      <span class="eq-val" id="eq-val-${idx}">0dB</span>
    `;
    container.appendChild(col);
  });

  container.querySelectorAll('.eq-slider').forEach(slider => {
    slider.addEventListener('input', (e) => {
      const idx = parseInt(e.target.dataset.index);
      const val = parseFloat(e.target.value);
      if (eqFilters[idx]) eqFilters[idx].gain.value = val;
      const text = document.getElementById(`eq-val-${idx}`);
      if (text) text.innerText = `${val > 0 ? '+' : ''}${val}dB`;
    });
  });
}

// EQ Presets
const eqPresets = {
  flat: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  bass: [6, 5, 4, 2, 0, 0, 0, 1, 2, 2],
  rock: [4, 3, 2, 0, -1, 1, 2, 3, 4, 4],
  pop: [-1, 1, 2, 3, 4, 3, 1, 0, 1, 2],
  vocal: [-2, -1, 0, 2, 4, 5, 3, 1, 0, -1],
  electronic: [5, 4, 3, 0, -1, 2, 1, 3, 4, 5],
  acoustic: [3, 2, 1, 1, 2, 2, 3, 3, 2, 1]
};

document.getElementById('eq-preset-dropdown')?.addEventListener('change', (e) => {
  const preset = eqPresets[e.target.value] || eqPresets.flat;
  const sliders = document.querySelectorAll('.eq-slider');
  preset.forEach((val, i) => {
    if (sliders[i]) {
      sliders[i].value = val;
      if (eqFilters[i]) eqFilters[i].gain.value = val;
      const text = document.getElementById(`eq-val-${i}`);
      if (text) text.innerText = `${val > 0 ? '+' : ''}${val}dB`;
    }
  });
});

// AI Stem Mixer Presets
const stemSliders = {
  vocals: document.getElementById('stem-vocals'),
  drums: document.getElementById('stem-drums'),
  bass: document.getElementById('stem-bass'),
  melodies: document.getElementById('stem-melodies')
};

function setStemPreset(vocals, drums, bass, melodies) {
  if (stemSliders.vocals) { stemSliders.vocals.value = vocals; updateStemValue('vocals', vocals); }
  if (stemSliders.drums) { stemSliders.drums.value = drums; updateStemValue('drums', drums); }
  if (stemSliders.bass) { stemSliders.bass.value = bass; updateStemValue('bass', bass); }
  if (stemSliders.melodies) { stemSliders.melodies.value = melodies; updateStemValue('melodies', melodies); }
}

function updateStemValue(name, val) {
  const text = document.getElementById(`val-${name}`);
  if (text) text.innerText = `${val}%`;
  // Psychoacoustic frequency adjustments based on stem levels
  if (eqFilters.length === 10) {
    if (name === 'vocals') {
      const g = ((val - 100) / 100) * 12;
      eqFilters[5].gain.value = g; // 1kHz
      eqFilters[6].gain.value = g; // 2kHz
    } else if (name === 'bass') {
      const g = ((val - 100) / 100) * 14;
      eqFilters[0].gain.value = g; // 31Hz
      eqFilters[1].gain.value = g; // 62Hz
      eqFilters[2].gain.value = g * 0.7; // 125Hz
    } else if (name === 'drums') {
      const g = ((val - 100) / 100) * 10;
      eqFilters[3].gain.value = g; // 250Hz
      eqFilters[8].gain.value = g; // 8kHz
      eqFilters[9].gain.value = g; // 16kHz
    } else if (name === 'melodies') {
      const g = ((val - 100) / 100) * 8;
      eqFilters[4].gain.value = g; // 500Hz
      eqFilters[7].gain.value = g; // 4kHz
    }
  }
}

Object.keys(stemSliders).forEach(key => {
  stemSliders[key]?.addEventListener('input', (e) => {
    updateStemValue(key, parseFloat(e.target.value));
  });
});

document.querySelectorAll('.preset-chips .chip').forEach(chip => {
  chip.addEventListener('click', () => {
    document.querySelectorAll('.preset-chips .chip').forEach(c => c.classList.remove('active'));
    chip.classList.add('active');
    const type = chip.dataset.preset;
    if (type === 'karaoke') setStemPreset(0, 110, 100, 100);
    else if (type === 'acapella') setStemPreset(140, 0, 0, 10);
    else if (type === 'bass') setStemPreset(100, 120, 150, 90);
    else if (type === 'instrumental') setStemPreset(0, 100, 100, 100);
    else if (type === 'reset') setStemPreset(100, 100, 100, 100);
  });
});

// Real-time Canvas FFT Visualizer
const canvas = document.getElementById('party-canvas');
const ctx = canvas ? canvas.getContext('2d') : null;

function resizeCanvas() {
  if (!canvas) return;
  canvas.width = canvas.parentElement.clientWidth;
  canvas.height = canvas.parentElement.clientHeight;
}
window.addEventListener('resize', resizeCanvas);
setTimeout(resizeCanvas, 200);

function startVisualizerLoop() {
  if (!analyserNode || !ctx) return;
  const bufferLength = analyserNode.frequencyBinCount;
  const dataArray = new Uint8Array(bufferLength);

  function draw() {
    requestAnimationFrame(draw);
    analyserNode.getByteFrequencyData(dataArray);

    ctx.fillStyle = 'rgba(5, 6, 12, 0.3)';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Calculate Bass Energy for Strobe and VU Meters
    let bassSum = 0;
    for (let i = 0; i < 8; i++) bassSum += dataArray[i];
    const bassAvg = bassSum / 8; // 0 to 255
    const bassRatio = bassAvg / 255;

    // VU meter updates
    const vuVocals = document.getElementById('vu-vocals');
    const vuDrums = document.getElementById('vu-drums');
    const vuBass = document.getElementById('vu-bass');
    const vuMelodies = document.getElementById('vu-melodies');

    if (vuBass) vuBass.style.width = `${Math.min(100, bassRatio * 130)}%`;
    if (vuDrums) vuDrums.style.width = `${Math.min(100, (dataArray[20] / 255) * 120)}%`;
    if (vuVocals) vuVocals.style.width = `${Math.min(100, (dataArray[50] / 255) * 120)}%`;
    if (vuMelodies) vuMelodies.style.width = `${Math.min(100, (dataArray[35] / 255) * 120)}%`;

    // Strobe neon border effect
    const strobeBorder = document.querySelector('.canvas-visualizer-wrapper');
    const chkPulse = document.getElementById('chk-strobe-pulse');
    if (strobeBorder && chkPulse?.checked && isPlaying) {
      if (bassRatio > 0.6) {
        strobeBorder.style.borderColor = `hsl(${Date.now() % 360}, 100%, 60%)`;
        strobeBorder.style.boxShadow = `0 0 ${bassRatio * 40}px hsl(${Date.now() % 360}, 100%, 60%)`;
      } else {
        strobeBorder.style.borderColor = 'transparent';
        strobeBorder.style.boxShadow = 'none';
      }
    }

    // Draw Spectrum Bars
    const barCount = 64;
    const barWidth = (canvas.width / barCount) - 2;
    for (let i = 0; i < barCount; i++) {
      const val = dataArray[i * 2];
      const barHeight = (val / 255) * (canvas.height * 0.8);
      const x = i * (barWidth + 2);
      const y = canvas.height - barHeight;

      const hue = (i / barCount) * 280 + 160;
      ctx.fillStyle = `hsl(${hue}, 100%, 60%)`;
      ctx.shadowBlur = 10;
      ctx.shadowColor = `hsl(${hue}, 100%, 60%)`;
      ctx.fillRect(x, y, barWidth, barHeight);
    }
  }
  draw();
}

// File & Folder Scanning (Electron + Browser/PWA Fallback)
const fileInputBrowser = document.getElementById('file-input-browser');
const folderInputBrowser = document.getElementById('folder-input-browser');

async function addFolder() {
  if (window.electronAPI) {
    const folder = await window.electronAPI.openFolderDialog();
    if (!folder) return;

    scannedFolders.add(folder);
    renderFoldersList();
    const files = await window.electronAPI.scanFolder(folder);
    for (const filePath of files) {
      const meta = await window.electronAPI.parseMetadata(filePath);
      playlist.push(meta);
    }
    renderTrackList();
  } else {
    folderInputBrowser?.click();
  }
}

async function importFiles() {
  if (window.electronAPI) {
    const files = await window.electronAPI.openFilesDialog();
    if (!files || files.length === 0) return;

    for (const filePath of files) {
      const meta = await window.electronAPI.parseMetadata(filePath);
      playlist.push(meta);
    }
    renderTrackList();
  } else {
    fileInputBrowser?.click();
  }
}

// Browser File Pickers event listeners
fileInputBrowser?.addEventListener('change', (e) => {
  handleBrowserFiles(Array.from(e.target.files));
});

folderInputBrowser?.addEventListener('change', (e) => {
  const files = Array.from(e.target.files);
  if (files.length > 0) {
    const folderName = files[0].webkitRelativePath ? files[0].webkitRelativePath.split('/')[0] : 'Carpeta Local';
    scannedFolders.add(folderName);
    renderFoldersList();
  }
  handleBrowserFiles(files);
});

function handleBrowserFiles(files) {
  const audioFiles = files.filter(f => f.type.startsWith('audio/') || /\.(mp3|wav|flac|m4a|aac|ogg|opus)$/i.test(f.name));
  audioFiles.forEach(file => {
    const nameWithoutExt = file.name.replace(/\.[^/.]+$/, "");
    playlist.push({
      title: nameWithoutExt,
      artist: 'Biblioteca Local',
      album: 'PC Audio',
      duration: 0,
      fileObj: file,
      coverUrl: null
    });
  });
  renderTrackList();
}

// Drag and Drop support
window.addEventListener('dragover', (e) => {
  e.preventDefault();
  e.stopPropagation();
});

window.addEventListener('drop', (e) => {
  e.preventDefault();
  e.stopPropagation();
  if (e.dataTransfer && e.dataTransfer.files.length > 0) {
    handleBrowserFiles(Array.from(e.dataTransfer.files));
  }
});

document.getElementById('btn-add-folder')?.addEventListener('click', addFolder);
document.getElementById('btn-add-folder-2')?.addEventListener('click', addFolder);
document.getElementById('btn-empty-scan')?.addEventListener('click', addFolder);
document.getElementById('btn-import-files')?.addEventListener('click', importFiles);

function renderFoldersList() {
  if (!foldersList) return;
  foldersList.innerHTML = '';
  scannedFolders.forEach(f => {
    const item = document.createElement('div');
    item.className = 'folder-item';
    item.style.padding = '0.8rem';
    item.style.background = 'rgba(255, 255, 255, 0.05)';
    item.style.borderRadius = '8px';
    item.style.marginBottom = '0.5rem';
    item.innerHTML = `<i class="fa-solid fa-folder" style="color: var(--accent-cyan); margin-right: 0.5rem;"></i> ${f}`;
    foldersList.appendChild(item);
  });
}

function formatTime(secs) {
  if (isNaN(secs) || secs < 0) return '0:00';
  const m = Math.floor(secs / 60);
  const s = Math.floor(secs % 60);
  return `${m}:${s < 10 ? '0' : ''}${s}`;
}

function renderTrackList(filtered = null) {
  const list = filtered || playlist;
  if (songsCount) songsCount.innerText = list.length;

  if (list.length === 0) {
    emptyState.classList.remove('hidden');
    trackTable.classList.add('hidden');
    return;
  }

  emptyState.classList.add('hidden');
  trackTable.classList.remove('hidden');
  trackList.innerHTML = '';

  list.forEach((track, idx) => {
    const tr = document.createElement('tr');
    tr.className = `track-row ${currentIndex === idx ? 'playing' : ''}`;
    const cover = track.coverUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="38" height="38" viewBox="0 0 38 38"><rect width="38" height="38" fill="%231e202e"/><circle cx="19" cy="19" r="8" fill="%233a3d52"/></svg>';
    tr.innerHTML = `
      <td>${currentIndex === idx && isPlaying ? '<i class="fa-solid fa-volume-high"></i>' : idx + 1}</td>
      <td class="track-title-cell">
        <img src="${cover}" class="track-cover-mini" alt="Cover">
        <span>${track.title}</span>
      </td>
      <td>${track.artist}</td>
      <td>${track.album}</td>
      <td>${track.duration ? formatTime(track.duration) : '--:--'}</td>
    `;
    tr.addEventListener('click', () => playTrack(idx));
    trackList.appendChild(tr);
  });
}

function playTrack(index) {
  if (index < 0 || index >= playlist.length) return;
  initAudioEngine();
  if (audioCtx && audioCtx.state === 'suspended') audioCtx.resume();

  currentIndex = index;
  const track = playlist[index];

  if (track.fileObj) {
    audio.src = URL.createObjectURL(track.fileObj);
  } else if (track.path) {
    audio.src = `file://${track.path}`;
  }

  audio.play().then(() => {
    isPlaying = true;
    updatePlayPauseUI();
  }).catch(err => console.error('Play error:', err));

  playerTitle.innerText = track.title;
  playerArtist.innerText = track.artist;
  if (track.coverUrl) {
    playerArt.src = track.coverUrl;
    document.getElementById('ambient-glow').style.background = `radial-gradient(circle at 50% 30%, rgba(99, 102, 241, 0.35), rgba(6, 182, 212, 0.25), transparent 70%)`;
  }
  artGlow.style.opacity = '1';

  // Update Lyrics header
  const lyrTitle = document.getElementById('lyrics-song-title');
  const lyrArtist = document.getElementById('lyrics-song-artist');
  if (lyrTitle) lyrTitle.innerText = track.title;
  if (lyrArtist) lyrArtist.innerText = track.artist;

  renderTrackList();
}

function updatePlayPauseUI() {
  if (isPlaying) {
    playBtn.innerHTML = '<i class="fa-solid fa-pause"></i>';
    artGlow.style.opacity = '1';
  } else {
    playBtn.innerHTML = '<i class="fa-solid fa-play"></i>';
    artGlow.style.opacity = '0';
  }
}

playBtn.addEventListener('click', () => {
  if (playlist.length === 0) return;
  if (currentIndex === -1) {
    playTrack(0);
    return;
  }
  initAudioEngine();
  if (audio.paused) {
    audio.play();
    isPlaying = true;
  } else {
    audio.pause();
    isPlaying = false;
  }
  updatePlayPauseUI();
  renderTrackList();
});

prevBtn.addEventListener('click', () => {
  if (currentIndex > 0) playTrack(currentIndex - 1);
  else playTrack(playlist.length - 1);
});

nextBtn.addEventListener('click', () => {
  if (isShuffle) {
    const rnd = Math.floor(Math.random() * playlist.length);
    playTrack(rnd);
  } else if (currentIndex < playlist.length - 1) {
    playTrack(currentIndex + 1);
  } else {
    playTrack(0);
  }
});

audio.addEventListener('timeupdate', () => {
  if (!audio.duration) return;
  const ratio = audio.currentTime / audio.duration;
  progressFill.style.width = `${ratio * 100}%`;
  currentTimeEl.innerText = formatTime(audio.currentTime);
  totalTimeEl.innerText = formatTime(audio.duration);
});

audio.addEventListener('ended', () => {
  if (repeatMode === 2) {
    playTrack(currentIndex);
  } else {
    nextBtn.click();
  }
});

progressContainer.addEventListener('click', (e) => {
  if (!audio.duration) return;
  const rect = progressContainer.getBoundingClientRect();
  const clickX = e.clientX - rect.left;
  const ratio = clickX / rect.width;
  audio.currentTime = ratio * audio.duration;
});

volumeSlider.addEventListener('input', (e) => {
  const val = e.target.value / 100;
  audio.volume = val;
  if (masterGain) masterGain.gain.value = val;
  updateVolumeIcon(val);
});

function updateVolumeIcon(val) {
  if (val === 0) muteBtn.innerHTML = '<i class="fa-solid fa-volume-xmark"></i>';
  else if (val < 0.5) muteBtn.innerHTML = '<i class="fa-solid fa-volume-low"></i>';
  else muteBtn.innerHTML = '<i class="fa-solid fa-volume-high"></i>';
}

muteBtn.addEventListener('click', () => {
  if (isMuted) {
    audio.volume = previousVolume;
    volumeSlider.value = previousVolume * 100;
    isMuted = false;
  } else {
    previousVolume = audio.volume;
    audio.volume = 0;
    volumeSlider.value = 0;
    isMuted = true;
  }
  updateVolumeIcon(audio.volume);
});

// Search filter
searchInput.addEventListener('input', (e) => {
  const q = e.target.value.toLowerCase();
  const filtered = playlist.filter(t => 
    t.title.toLowerCase().includes(q) ||
    t.artist.toLowerCase().includes(q) ||
    t.album.toLowerCase().includes(q)
  );
  renderTrackList(filtered);
});

// Keyboard Shortcuts
window.addEventListener('keydown', (e) => {
  if (e.target.tagName === 'INPUT') return;
  if (e.code === 'Space') {
    e.preventDefault();
    playBtn.click();
  } else if (e.code === 'ArrowRight') {
    if (e.shiftKey) nextBtn.click();
    else audio.currentTime += 5;
  } else if (e.code === 'ArrowLeft') {
    if (e.shiftKey) prevBtn.click();
    else audio.currentTime -= 5;
  } else if (e.code === 'ArrowUp') {
    volumeSlider.value = Math.min(100, parseInt(volumeSlider.value) + 5);
    volumeSlider.dispatchEvent(new Event('input'));
  } else if (e.code === 'ArrowDown') {
    volumeSlider.value = Math.max(0, parseInt(volumeSlider.value) - 5);
    volumeSlider.dispatchEvent(new Event('input'));
  } else if (e.code === 'KeyM') {
    muteBtn.click();
  } else if (e.code === 'KeyF') {
    document.getElementById('btn-party-fullscreen')?.click();
  }
});
