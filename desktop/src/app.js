// DaVE Player — Unified Core Audio Engine & UI Logic (Web & Desktop)

// ==========================================
// 1. GLOBAL STATE
// ==========================================
let playlist = [];
let currentIndex = -1;
let isPlaying = false;
let isMuted = false;
let currentVolume = 0.8;
let previousVolume = 0.8;
let isShuffle = false;
let repeatMode = 0; // 0: off, 1: repeat-all, 2: repeat-one
let favorites = new Set();
let scannedFolders = new Set();
let phoneTracks = [];
let currentUser = null;

// ==========================================
// 2. WEB AUDIO API NODES & ROUTING
// ==========================================
let audioCtx = null;
let sourceNode = null;
let analyserNode = null;
let masterGain = null;
let eqFilters = [];
const eqFrequencies = [31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000];

// Dedicated Stem Filter Bank (Separated from EQ to prevent mutual overwrite)
let stemFilters = {
  vocals: null,
  drums: null,
  bass: null,
  melodies: null
};

// DOM References
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
const ambientGlow = document.getElementById('ambient-glow');

// Modals & Auth DOM
const modalAuth = document.getElementById('modal-auth');
const modalSync = document.getElementById('modal-sync');
const btnLoginModal = document.getElementById('btn-login-modal');
const btnCloseAuth = document.getElementById('btn-close-auth');
const btnCloseSync = document.getElementById('btn-close-sync');
const authForm = document.getElementById('auth-form');
const authFormContainer = document.getElementById('auth-form-container');
const authUserProfile = document.getElementById('auth-user-profile');
const userDisplayName = document.getElementById('user-display-name');
const profileName = document.getElementById('profile-name');
const profileEmail = document.getElementById('profile-email');
const pstatSongs = document.getElementById('pstat-songs');
const pstatFavs = document.getElementById('pstat-favs');
const phoneSongsCount = document.getElementById('phone-songs-count');
const phoneSyncStatus = document.getElementById('phone-sync-status');
const phoneTrackTable = document.getElementById('phone-track-table');
const phoneTrackList = document.getElementById('phone-track-list');
const phoneEmptyState = document.getElementById('phone-empty-state');
const syncFeedback = document.getElementById('sync-feedback');

// Canvas
const canvas = document.getElementById('party-canvas');
const ctx = canvas ? canvas.getContext('2d') : null;

// ==========================================
// 3. AUDIO ENGINE INITIALIZATION
// ==========================================
function initAudioEngine() {
  if (audioCtx) {
    if (audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
    return;
  }

  const AudioContext = window.AudioContext || window.webkitAudioContext;
  audioCtx = new AudioContext();

  try {
    sourceNode = audioCtx.createMediaElementSource(audio);
  } catch (err) {
    console.warn('MediaElementSource already created or error:', err);
    return;
  }

  // 1. Build 10-Band EQ filters in series
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

  // Chain EQ in series
  let lastNode = sourceNode;
  eqFilters.forEach(f => {
    lastNode.connect(f);
    lastNode = f;
  });

  // 2. Build Dedicated Stem Filters
  stemFilters.bass = audioCtx.createBiquadFilter();
  stemFilters.bass.type = 'lowshelf';
  stemFilters.bass.frequency.value = 120;
  stemFilters.bass.gain.value = 0;

  stemFilters.drums = audioCtx.createBiquadFilter();
  stemFilters.drums.type = 'peaking';
  stemFilters.drums.frequency.value = 4000;
  stemFilters.drums.Q.value = 1.0;
  stemFilters.drums.gain.value = 0;

  stemFilters.melodies = audioCtx.createBiquadFilter();
  stemFilters.melodies.type = 'peaking';
  stemFilters.melodies.frequency.value = 800;
  stemFilters.melodies.Q.value = 1.0;
  stemFilters.melodies.gain.value = 0;

  stemFilters.vocals = audioCtx.createBiquadFilter();
  stemFilters.vocals.type = 'peaking';
  stemFilters.vocals.frequency.value = 2200;
  stemFilters.vocals.Q.value = 1.2;
  stemFilters.vocals.gain.value = 0;

  lastNode.connect(stemFilters.bass);
  stemFilters.bass.connect(stemFilters.drums);
  stemFilters.drums.connect(stemFilters.melodies);
  stemFilters.melodies.connect(stemFilters.vocals);
  lastNode = stemFilters.vocals;

  // 3. Analyser Node for FFT Visualizer & VU meters
  analyserNode = audioCtx.createAnalyser();
  analyserNode.fftSize = 512;
  analyserNode.smoothingTimeConstant = 0.8;
  lastNode.connect(analyserNode);

  // 4. Master Gain Node (Controls speaker volume without dampening analyser)
  masterGain = audioCtx.createGain();
  masterGain.gain.value = currentVolume;
  analyserNode.connect(masterGain);
  masterGain.connect(audioCtx.destination);

  // Audio element itself stays at 1.0 so analyser always receives full audio signal
  audio.volume = 1.0;

  startVisualizerLoop();
}

// ==========================================
// 4. EQUALIZER UI & PRESETS
// ==========================================
function renderEqualizerUI() {
  const container = document.getElementById('eq-bands');
  if (!container) return;
  container.innerHTML = '';

  eqFrequencies.forEach((freq, idx) => {
    const col = document.createElement('div');
    col.className = 'eq-slider-col';
    const label = freq >= 1000 ? `${freq / 1000}k` : `${freq}`;
    col.innerHTML = `
      <div class="slider-wrapper" style="height: 160px;">
        <input type="range" min="-12" max="12" value="0" step="0.5" class="vertical-slider eq-slider" data-index="${idx}" id="eq-slider-${idx}">
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
      if (eqFilters[idx]) {
        eqFilters[idx].gain.value = val;
      }
      const text = document.getElementById(`eq-val-${idx}`);
      if (text) text.innerText = `${val > 0 ? '+' : ''}${val}dB`;
    });
  });
}

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
  preset.forEach((val, i) => {
    const slider = document.getElementById(`eq-slider-${i}`);
    if (slider) slider.value = val;
    if (eqFilters[i]) eqFilters[i].gain.value = val;
    const text = document.getElementById(`eq-val-${i}`);
    if (text) text.innerText = `${val > 0 ? '+' : ''}${val}dB`;
  });
});

// ==========================================
// 5. LIVE AI STEM MIXER (INDEPENDENT BANK)
// ==========================================
const stemSliders = {
  vocals: document.getElementById('stem-vocals'),
  drums: document.getElementById('stem-drums'),
  bass: document.getElementById('stem-bass'),
  melodies: document.getElementById('stem-melodies')
};

function updateStemValue(name, val) {
  const text = document.getElementById(`val-${name}`);
  if (text) text.innerText = `${val}%`;

  if (!stemFilters[name]) return;

  // Scale: 100% = 0dB. 0% = -24dB cut. 150% = +6dB boost
  const gainDb = val <= 100 ? ((val - 100) / 100) * 24 : ((val - 100) / 50) * 6;
  stemFilters[name].gain.value = gainDb;
}

function setStemPreset(vocals, drums, bass, melodies) {
  if (stemSliders.vocals) { stemSliders.vocals.value = vocals; updateStemValue('vocals', vocals); }
  if (stemSliders.drums) { stemSliders.drums.value = drums; updateStemValue('drums', drums); }
  if (stemSliders.bass) { stemSliders.bass.value = bass; updateStemValue('bass', bass); }
  if (stemSliders.melodies) { stemSliders.melodies.value = melodies; updateStemValue('melodies', melodies); }
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

// ==========================================
// 6. VISUALIZER & PARTY FFT LOOP
// ==========================================
function resizeCanvas() {
  if (!canvas || !canvas.parentElement) return;
  const w = canvas.parentElement.clientWidth;
  const h = canvas.parentElement.clientHeight;
  if (w > 0 && h > 0) {
    canvas.width = w;
    canvas.height = h;
  }
}
window.addEventListener('resize', resizeCanvas);

function startVisualizerLoop() {
  if (!analyserNode || !ctx) return;
  const bufferLength = analyserNode.frequencyBinCount;
  const dataArray = new Uint8Array(bufferLength);

  // Cached VU meters
  const vuVocals = document.getElementById('vu-vocals');
  const vuDrums = document.getElementById('vu-drums');
  const vuBass = document.getElementById('vu-bass');
  const vuMelodies = document.getElementById('vu-melodies');
  const strobeBorder = document.querySelector('.canvas-visualizer-wrapper');
  const chkPulse = document.getElementById('chk-strobe-pulse');

  function draw() {
    requestAnimationFrame(draw);
    analyserNode.getByteFrequencyData(dataArray);

    if (canvas.width > 0 && canvas.height > 0) {
      ctx.fillStyle = 'rgba(5, 6, 12, 0.35)';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Spectrum Bars
      const barCount = 48;
      const barWidth = Math.max(2, (canvas.width / barCount) - 3);
      for (let i = 0; i < barCount; i++) {
        const val = dataArray[i * 3];
        const barHeight = (val / 255) * (canvas.height * 0.85);
        const x = i * (barWidth + 3);
        const y = canvas.height - barHeight;

        const hue = (i / barCount) * 260 + 170;
        ctx.fillStyle = `hsl(${hue}, 100%, 60%)`;
        ctx.shadowBlur = 8;
        ctx.shadowColor = `hsl(${hue}, 100%, 60%)`;
        ctx.fillRect(x, y, barWidth, barHeight);
      }
    }

    // VU Meters & Strobe Pulse
    let bassSum = 0;
    for (let i = 0; i < 6; i++) bassSum += dataArray[i];
    const bassRatio = (bassSum / 6) / 255;

    if (vuBass) vuBass.style.width = `${Math.min(100, bassRatio * 135)}%`;
    if (vuDrums) vuDrums.style.width = `${Math.min(100, (dataArray[16] / 255) * 125)}%`;
    if (vuVocals) vuVocals.style.width = `${Math.min(100, (dataArray[45] / 255) * 125)}%`;
    if (vuMelodies) vuMelodies.style.width = `${Math.min(100, (dataArray[28] / 255) * 125)}%`;

    if (strobeBorder && chkPulse?.checked && isPlaying) {
      if (bassRatio > 0.58) {
        strobeBorder.style.borderColor = `hsl(${Date.now() % 360}, 100%, 65%)`;
        strobeBorder.style.boxShadow = `0 0 ${bassRatio * 35}px hsl(${Date.now() % 360}, 100%, 65%)`;
      } else {
        strobeBorder.style.borderColor = 'transparent';
        strobeBorder.style.boxShadow = 'none';
      }
    }
  }
  draw();
}

// Fullscreen Party Mode
document.getElementById('btn-party-fullscreen')?.addEventListener('click', () => {
  const container = document.querySelector('.canvas-visualizer-wrapper');
  if (!container) return;
  if (!document.fullscreenElement) {
    container.requestFullscreen?.().then(() => resizeCanvas()).catch(e => console.log(e));
  } else {
    document.exitFullscreen?.().then(() => resizeCanvas()).catch(e => console.log(e));
  }
});
document.addEventListener('fullscreenchange', () => setTimeout(resizeCanvas, 100));

// ==========================================
// 7. TIME & SYNCED KARAOKE LYRICS
// ==========================================
function formatTime(secs) {
  if (!isFinite(secs) || isNaN(secs) || secs < 0) return '0:00';
  const m = Math.floor(secs / 60);
  const s = Math.floor(secs % 60);
  return `${m}:${s < 10 ? '0' : ''}${s}`;
}

const defaultLyrics = [
  { time: 0, text: "✨ DaVE Player — Audio HD Master Pro" },
  { time: 4, text: "🎶 Sintetizadores en estéreo y bajos 808" },
  { time: 8, text: "🎛️ AI Stem Mixer aislando frecuencias en vivo" },
  { time: 12, text: "🔥 Siente el ritmo con el ecualizador de 10 bandas" },
  { time: 16, text: "🎤 Modo Karaoke activo: canta tu tema favorito" },
  { time: 20, text: "⚡ Disfruta la mejor música sin anuncios y sin cortes" }
];

const songLyricsMap = {
  'Happy Nation': [
    { time: 0, text: "✨ Happy Nation — Ace of Base (Infinix Audio HD)" },
    { time: 8, text: "Laudate omnes gentes laudate" },
    { time: 16, text: "Magnificat in secula" },
    { time: 24, text: "Et anima mea laudate" },
    { time: 32, text: "Magnificat in secula" },
    { time: 43, text: "Happy Nation, living in a happy nation" },
    { time: 52, text: "Where the people understand" },
    { time: 56, text: "And face the thing we're fighting for" },
    { time: 61, text: "Happy Nation, living in a happy nation" },
    { time: 70, text: "Where the people understand" },
    { time: 74, text: "And face the thing we're fighting for" },
    { time: 80, text: "Ideas aiming for the better" },
    { time: 85, text: "Even though they tell us it's too late" }
  ],
  'SSRHD (Remix)': [
    { time: 0, text: "✨ Ziraki — SSRHD (Remix)" },
    { time: 8, text: "Suno AI Producción Exclusiva" },
    { time: 18, text: "Bajos 808 y armonía electrónica" },
    { time: 30, text: "Sintetizadores al máximo volumen" }
  ]
};

function getCurrentLyrics() {
  if (currentIndex >= 0 && currentIndex < playlist.length) {
    const title = playlist[currentIndex].title;
    if (songLyricsMap[title]) return songLyricsMap[title];
  }
  return defaultLyrics;
}

function renderLyrics(currentTime) {
  const box = document.getElementById('lyrics-box');
  if (!box) return;

  const currentLyrics = getCurrentLyrics();

  // Render on track start or when track changes
  if (box.dataset.trackIndex !== String(currentIndex) || box.children.length !== currentLyrics.length) {
    box.innerHTML = '';
    box.dataset.trackIndex = String(currentIndex);
    currentLyrics.forEach((line) => {
      const div = document.createElement('div');
      div.className = 'lyric-line';
      div.dataset.time = line.time;
      div.innerText = line.text;
      div.addEventListener('click', () => {
        audio.currentTime = line.time;
      });
      box.appendChild(div);
    });
  }

  // Find active line
  let activeIdx = 0;
  for (let i = 0; i < currentLyrics.length; i++) {
    if (currentTime >= currentLyrics[i].time) {
      activeIdx = i;
    }
  }

  const lines = box.querySelectorAll('.lyric-line');
  lines.forEach((l, idx) => {
    if (idx === activeIdx) {
      l.classList.add('active');
      l.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } else {
      l.classList.remove('active');
    }
  });
}

// ==========================================
// 8. CORE PLAYBACK ENGINE
// ==========================================
function playTrack(index) {
  if (index < 0 || index >= playlist.length) return;
  initAudioEngine();
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }

  currentIndex = index;
  const track = playlist[index];

  // Set audio source
  if (track.demoUrl) {
    audio.src = track.demoUrl;
  } else if (track.streamUrl) {
    audio.src = track.streamUrl;
  } else if (track.fileObj) {
    audio.src = URL.createObjectURL(track.fileObj);
  } else if (track.path) {
    audio.src = `file://${track.path}`;
  }

  audio.play().then(() => {
    isPlaying = true;
    updatePlayPauseUI();
  }).catch(err => {
    console.warn('Playback error / autoplay blocked:', err);
    isPlaying = false;
    updatePlayPauseUI();
  });

  playerTitle.innerText = track.title;
  playerArtist.innerText = track.artist;
  if (track.coverUrl) {
    playerArt.src = track.coverUrl;
    ambientGlow.style.background = `radial-gradient(circle at 50% 30%, rgba(99, 102, 241, 0.35), rgba(6, 182, 212, 0.25), transparent 70%)`;
  } else {
    playerArt.src = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><rect width="100" height="100" fill="%23181824"/><circle cx="50" cy="50" r="25" fill="%236366f1"/></svg>';
  }
  artGlow.style.opacity = '1';

  // Update Lyrics header
  const lyrTitle = document.getElementById('lyrics-song-title');
  const lyrArtist = document.getElementById('lyrics-song-artist');
  if (lyrTitle) lyrTitle.innerText = track.title;
  if (lyrArtist) lyrArtist.innerText = track.artist;

  // Update Like button state
  const trackId = track.title + track.artist;
  if (favorites.has(trackId)) {
    btnLike?.classList.add('active');
    btnLike.innerHTML = '<i class="fa-solid fa-heart"></i>';
  } else {
    btnLike?.classList.remove('active');
    btnLike.innerHTML = '<i class="fa-regular fa-heart"></i>';
  }

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

// Play / Pause Toggle
playBtn.addEventListener('click', () => {
  if (playlist.length === 0) return;
  if (currentIndex === -1) {
    playTrack(0);
    return;
  }
  initAudioEngine();
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }

  if (audio.paused) {
    audio.play().then(() => {
      isPlaying = true;
      updatePlayPauseUI();
    }).catch(e => console.log(e));
  } else {
    audio.pause();
    isPlaying = false;
    updatePlayPauseUI();
  }
  renderTrackList();
});

// Previous Track
prevBtn.addEventListener('click', () => {
  if (playlist.length === 0) return;
  if (currentIndex > 0) playTrack(currentIndex - 1);
  else playTrack(playlist.length - 1);
});

// Next Track
nextBtn.addEventListener('click', () => {
  if (playlist.length === 0) return;
  if (isShuffle) {
    const rnd = Math.floor(Math.random() * playlist.length);
    playTrack(rnd);
  } else if (currentIndex < playlist.length - 1) {
    playTrack(currentIndex + 1);
  } else {
    playTrack(0);
  }
});

// Shuffle Button Toggle
shuffleBtn?.addEventListener('click', () => {
  isShuffle = !isShuffle;
  shuffleBtn.classList.toggle('active', isShuffle);
});

// Repeat Button Toggle (Off -> Repeat All -> Repeat One -> Off)
repeatBtn?.addEventListener('click', () => {
  repeatMode = (repeatMode + 1) % 3;
  if (repeatMode === 0) {
    repeatBtn.classList.remove('active');
    repeatBtn.innerHTML = '<i class="fa-solid fa-repeat"></i>';
    repeatBtn.title = 'Repetir: Desactivado';
  } else if (repeatMode === 1) {
    repeatBtn.classList.add('active');
    repeatBtn.innerHTML = '<i class="fa-solid fa-repeat"></i>';
    repeatBtn.title = 'Repetir: Toda la lista';
  } else {
    repeatBtn.classList.add('active');
    repeatBtn.innerHTML = '<i class="fa-solid fa-repeat"></i><span style="font-size:0.6rem; vertical-align:super;">1</span>';
    repeatBtn.title = 'Repetir: Esta canción';
  }
});

// Audio Time Updates
audio.addEventListener('timeupdate', () => {
  if (!audio.duration || !isFinite(audio.duration)) return;
  const ratio = audio.currentTime / audio.duration;
  progressFill.style.width = `${ratio * 100}%`;
  currentTimeEl.innerText = formatTime(audio.currentTime);
  totalTimeEl.innerText = formatTime(audio.duration);
  renderLyrics(audio.currentTime);
});

// Audio Ended Event
audio.addEventListener('ended', () => {
  if (repeatMode === 2) {
    playTrack(currentIndex);
  } else if (repeatMode === 1 || isShuffle) {
    nextBtn.click();
  } else {
    // Repeat off: advance if not at the end, else stop
    if (currentIndex < playlist.length - 1) {
      playTrack(currentIndex + 1);
    } else {
      isPlaying = false;
      updatePlayPauseUI();
    }
  }
});

// Scrubber Seeking
progressContainer.addEventListener('click', (e) => {
  if (!audio.duration || !isFinite(audio.duration)) return;
  const rect = progressContainer.getBoundingClientRect();
  const clickX = e.clientX - rect.left;
  const ratio = Math.max(0, Math.min(1, clickX / rect.width));
  audio.currentTime = ratio * audio.duration;
});

// Volume Slider
volumeSlider.addEventListener('input', (e) => {
  const val = e.target.value / 100;
  currentVolume = val;
  if (masterGain) masterGain.gain.value = val;
  isMuted = (val === 0);
  updateVolumeIcon(val);
});

function updateVolumeIcon(val) {
  if (val === 0) muteBtn.innerHTML = '<i class="fa-solid fa-volume-xmark"></i>';
  else if (val < 0.5) muteBtn.innerHTML = '<i class="fa-solid fa-volume-low"></i>';
  else muteBtn.innerHTML = '<i class="fa-solid fa-volume-high"></i>';
}

muteBtn.addEventListener('click', () => {
  if (isMuted) {
    currentVolume = previousVolume || 0.8;
    volumeSlider.value = currentVolume * 100;
    if (masterGain) masterGain.gain.value = currentVolume;
    isMuted = false;
  } else {
    previousVolume = currentVolume;
    currentVolume = 0;
    volumeSlider.value = 0;
    if (masterGain) masterGain.gain.value = 0;
    isMuted = true;
  }
  updateVolumeIcon(currentVolume);
});

// Favorite Toggle
btnLike?.addEventListener('click', () => {
  if (currentIndex === -1 || playlist.length === 0) return;
  const currentTrack = playlist[currentIndex];
  const trackId = currentTrack.title + currentTrack.artist;

  if (favorites.has(trackId)) {
    favorites.delete(trackId);
    btnLike.classList.remove('active');
    btnLike.innerHTML = '<i class="fa-regular fa-heart"></i>';
  } else {
    favorites.add(trackId);
    btnLike.classList.add('active');
    btnLike.innerHTML = '<i class="fa-solid fa-heart"></i>';
  }

  if (favoritesCount) favoritesCount.innerText = `${favorites.size} canciones`;
  if (pstatFavs) pstatFavs.innerText = favorites.size;
  localStorage.setItem('dave_favorites', JSON.stringify(Array.from(favorites)));
});

// Favorites Playlist Card Click
document.querySelector('.favorites-card')?.addEventListener('click', () => {
  switchTab('songs');
  const favSongs = playlist.filter(t => favorites.has(t.title + t.artist));
  if (favSongs.length > 0) {
    renderTrackList(favSongs);
  } else {
    alert('Aún no has marcado canciones como favoritas. Pulsa el corazón en el reproductor para agregarlas.');
  }
});

// ==========================================
// 9. TRACKLIST & SEARCH (BUG FIXED: INDEXOF)
// ==========================================
function renderTrackList(filtered = null) {
  const list = filtered || playlist;
  if (songsCount) songsCount.innerText = list.length;

  if (list.length === 0) {
    emptyState?.classList.remove('hidden');
    trackTable?.classList.add('hidden');
    return;
  }

  emptyState?.classList.add('hidden');
  trackTable?.classList.remove('hidden');
  if (!trackList) return;
  trackList.innerHTML = '';

  list.forEach((track) => {
    const tr = document.createElement('tr');
    const isThisPlaying = (currentIndex !== -1 && playlist[currentIndex] === track);
    tr.className = `track-row ${isThisPlaying ? 'playing' : ''}`;
    const cover = track.coverUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="38" height="38" viewBox="0 0 38 38"><rect width="38" height="38" fill="%231e202e"/><circle cx="19" cy="19" r="8" fill="%236366f1"/></svg>';
    const displayIndex = playlist.indexOf(track) + 1;

    tr.innerHTML = `
      <td>${isThisPlaying && isPlaying ? '<i class="fa-solid fa-volume-high"></i>' : displayIndex}</td>
      <td class="track-title-cell">
        <img src="${cover}" class="track-cover-mini" alt="Cover">
        <span>${track.title}</span>
      </td>
      <td>${track.artist}</td>
      <td>${track.album}</td>
      <td>${track.duration ? formatTime(track.duration) : '--:--'}</td>
    `;
    // FIX: Click plays the exact index in playlist, not filtered index!
    tr.addEventListener('click', () => playTrack(playlist.indexOf(track)));
    trackList.appendChild(tr);
  });
}

// Search Filter
searchInput?.addEventListener('input', (e) => {
  const q = e.target.value.toLowerCase().trim();
  if (!q) {
    renderTrackList();
    return;
  }
  const filtered = playlist.filter(t => 
    t.title.toLowerCase().includes(q) ||
    t.artist.toLowerCase().includes(q) ||
    t.album.toLowerCase().includes(q)
  );
  renderTrackList(filtered);
});

// ==========================================
// 10. FILE SCANNING & DRAG & DROP
// ==========================================
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

fileInputBrowser?.addEventListener('change', (e) => {
  handleBrowserFiles(Array.from(e.target.files));
});

folderInputBrowser?.addEventListener('change', (e) => {
  const files = Array.from(e.target.files);
  if (files.length > 0) {
    const folderName = files[0].webkitRelativePath ? files[0].webkitRelativePath.split('/')[0] : 'Carpeta PC';
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

// Drag & Drop
window.addEventListener('dragover', (e) => {
  e.preventDefault();
  e.stopPropagation();
});
window.addEventListener('drop', (e) => {
  e.preventDefault();
  e.stopPropagation();
  if (e.dataTransfer && e.dataTransfer.files.length > 0) {
    const files = Array.from(e.dataTransfer.files);
    const activeTab = document.querySelector('.tab-pane.active');
    if (activeTab && activeTab.id === 'tab-phone') {
      handlePhoneBrowserFiles(files);
    } else {
      handleBrowserFiles(files);
    }
  }
});

document.getElementById('btn-add-folder')?.addEventListener('click', addFolder);
document.getElementById('btn-empty-scan')?.addEventListener('click', importFiles);
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

// ==========================================
// 11. SYNTHWAVE DEMO TRACK GENERATOR
// ==========================================
function generateSynthwaveWav() {
  const sampleRate = 44100;
  const bpm = 120;
  const seconds = 24;
  const totalSamples = sampleRate * seconds;
  const buffer = new Float32Array(totalSamples);
  const beatSec = 60 / bpm;
  const sixteenth = beatSec / 4;

  const chords = [
    [130.81, 164.81, 196.00], // C
    [110.00, 130.81, 164.81], // Am
    [146.83, 174.61, 220.00], // Dm
    [98.00, 123.47, 146.83]   // G
  ];

  for (let i = 0; i < totalSamples; i++) {
    const t = i / sampleRate;
    const bar = Math.floor(t / (beatSec * 4));
    const chordIdx = bar % chords.length;
    const currentChord = chords[chordIdx];
    const beatInBar = (t % (beatSec * 4)) / beatSec;

    let sample = 0;

    // Kick
    const beatPos = (t % beatSec);
    if (beatPos < 0.2) {
      const kickFreq = 150 * Math.exp(-beatPos * 30);
      sample += Math.sin(2 * Math.PI * kickFreq * beatPos) * Math.exp(-beatPos * 15) * 0.7;
    }

    // Snare
    if ((beatInBar >= 1 && beatInBar < 1.3) || (beatInBar >= 3 && beatInBar < 3.3)) {
      const snarePos = beatInBar % 2;
      sample += (Math.random() * 2 - 1) * Math.exp(-snarePos * 12) * 0.35;
    }

    // Hi-hats
    const hihatPos = (t % sixteenth);
    if (hihatPos < 0.05) {
      sample += (Math.random() * 2 - 1) * Math.exp(-hihatPos * 60) * 0.15;
    }

    // 808 Bass
    const bassNote = currentChord[0] / 2;
    const bassPulse = (t % (sixteenth * 2));
    const bassEnv = Math.exp(-bassPulse * 8);
    sample += (Math.sin(2 * Math.PI * bassNote * t) + 0.3 * Math.sin(4 * Math.PI * bassNote * t)) * bassEnv * 0.45;

    // Melodies
    let pad = 0;
    currentChord.forEach((f) => {
      pad += Math.sin(2 * Math.PI * f * t) + 0.25 * Math.sin(2 * Math.PI * f * 2 * t);
    });
    sample += pad * 0.12;

    // Vocals / Lead Synth
    const arpFreq = currentChord[Math.floor((t / sixteenth) % currentChord.length)] * 2;
    const arpEnv = Math.exp(-(t % sixteenth) * 6);
    sample += Math.sin(2 * Math.PI * arpFreq * t) * arpEnv * 0.18;

    buffer[i] = Math.max(-1, Math.min(1, sample));
  }

  return URL.createObjectURL(encodeWav(buffer, sampleRate));
}

function encodeWav(samples, sampleRate) {
  const numChannels = 1;
  const bytesPerSample = 2;
  const blockAlign = numChannels * bytesPerSample;
  const byteRate = sampleRate * blockAlign;
  const dataSize = samples.length * bytesPerSample;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);

  function writeString(view, offset, string) {
    for (let i = 0; i < string.length; i++) view.setUint8(offset + i, string.charCodeAt(i));
  }

  writeString(view, 0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeString(view, 8, 'WAVE');
  writeString(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, numChannels, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, byteRate, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, 16, true);
  writeString(view, 36, 'data');
  view.setUint32(40, dataSize, true);

  let offset = 44;
  for (let i = 0; i < samples.length; i++, offset += 2) {
    const s = Math.max(-1, Math.min(1, samples[i]));
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
  }

  return new Blob([view], { type: 'audio/wav' });
}

function loadDemoTrack() {
  const demoUrl = generateSynthwaveWav();
  const demoTrack = {
    title: 'DaVE Neon Nights (Synthwave Studio Demo)',
    artist: 'DaVE Audio Engine',
    album: 'Master Pro Suite 2026',
    duration: 24,
    path: null,
    fileObj: null,
    demoUrl: demoUrl,
    coverUrl: 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><defs><linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="%236366f1"/><stop offset="100%" stop-color="%23ec4899"/></linearGradient></defs><rect width="100" height="100" fill="url(%23g)"/><circle cx="50" cy="50" r="28" fill="%230f111a"/><polygon points="44,38 64,50 44,62" fill="%2338bdf8"/></svg>'
  };

  playlist = [demoTrack, ...playlist];
  renderTrackList();
  playTrack(0);
}

document.getElementById('btn-demo-track')?.addEventListener('click', loadDemoTrack);
document.getElementById('btn-empty-demo')?.addEventListener('click', loadDemoTrack);

// ==========================================
// 12. TAB SWITCHING & NAVBAR LINKS
// ==========================================
function switchTab(name) {
  document.querySelectorAll('.nav-item, .nav-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.tab-pane, .tab-content').forEach(t => t.classList.remove('active'));
  
  const navBtn = document.querySelector(`.nav-item[data-tab="${name}"], .nav-btn[data-tab="${name}"]`);
  if (navBtn) navBtn.classList.add('active');

  const tabContent = document.getElementById(`tab-${name}`);
  if (tabContent) tabContent.classList.add('active');

  if (name === 'club-party') {
    setTimeout(resizeCanvas, 50);
  }
}

document.querySelectorAll('.nav-item, .nav-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    if (btn.dataset.tab) {
      switchTab(btn.dataset.tab);
    }
  });
});

document.getElementById('btn-quick-stem')?.addEventListener('click', () => switchTab('stem-mixer'));
document.getElementById('btn-quick-lyrics')?.addEventListener('click', () => switchTab('lyrics'));
document.getElementById('btn-quick-eq')?.addEventListener('click', () => switchTab('equalizer'));

// Navbar anchors
document.querySelector('a[href="#stem-mixer-info"]')?.addEventListener('click', (e) => {
  e.preventDefault();
  document.getElementById('player-section')?.scrollIntoView({ behavior: 'smooth' });
  switchTab('stem-mixer');
});

// ==========================================
// 13. USER AUTH & PHONE SYNC
// ==========================================
btnLoginModal?.addEventListener('click', () => {
  modalAuth?.classList.remove('hidden');
  updateUserUI();
});
btnCloseAuth?.addEventListener('click', () => modalAuth?.classList.add('hidden'));
btnCloseSync?.addEventListener('click', () => modalSync?.classList.add('hidden'));

document.getElementById('btn-open-sync-modal')?.addEventListener('click', () => modalSync?.classList.remove('hidden'));
document.getElementById('btn-empty-phone-connect')?.addEventListener('click', () => modalSync?.classList.remove('hidden'));
document.getElementById('btn-phone-sync-trigger')?.addEventListener('click', () => modalSync?.classList.remove('hidden'));

function updateUserUI() {
  if (currentUser) {
    if (userDisplayName) userDisplayName.innerText = currentUser.name || currentUser.email.split('@')[0];
    if (profileName) profileName.innerText = currentUser.name || currentUser.email.split('@')[0];
    if (profileEmail) profileEmail.innerText = currentUser.email;
    if (pstatSongs) pstatSongs.innerText = phoneTracks.length;
    if (pstatFavs) pstatFavs.innerText = favorites.size;

    authFormContainer?.classList.add('hidden');
    authUserProfile?.classList.remove('hidden');
    if (phoneSyncStatus) phoneSyncStatus.innerText = `Sincronizado con ${currentUser.email} • ${phoneTracks.length} canciones del celular disponibles.`;
  } else {
    if (userDisplayName) userDisplayName.innerText = 'Iniciar Sesión';
    authFormContainer?.classList.remove('hidden');
    authUserProfile?.classList.add('hidden');
    if (phoneSyncStatus) phoneSyncStatus.innerText = 'Inicia sesión con tu correo o conecta tu celular por WiFi/PIN para ver tus canciones aquí.';
  }
}

authForm?.addEventListener('submit', (e) => {
  e.preventDefault();
  const email = document.getElementById('auth-email').value;
  const name = email.split('@')[0];
  currentUser = {
    email: email,
    name: name.charAt(0).toUpperCase() + name.slice(1),
    loggedInAt: new Date().toISOString()
  };
  localStorage.setItem('dave_user', JSON.stringify(currentUser));
  updateUserUI();
  if (phoneTracks.length === 0) syncCloudPhoneLibrary();
  setTimeout(() => modalAuth?.classList.add('hidden'), 500);
});

document.getElementById('auth-link-demo')?.addEventListener('click', (e) => {
  e.preventDefault();
  currentUser = {
    email: 'david.chaparro@daveplayer.app',
    name: 'David Chaparro',
    loggedInAt: new Date().toISOString()
  };
  localStorage.setItem('dave_user', JSON.stringify(currentUser));
  updateUserUI();
  syncCloudPhoneLibrary();
  setTimeout(() => modalAuth?.classList.add('hidden'), 500);
});

document.getElementById('btn-google-login')?.addEventListener('click', () => {
  currentUser = {
    email: 'usuario.google@gmail.com',
    name: 'Usuario DaVE',
    loggedInAt: new Date().toISOString()
  };
  localStorage.setItem('dave_user', JSON.stringify(currentUser));
  updateUserUI();
  syncCloudPhoneLibrary();
  setTimeout(() => modalAuth?.classList.add('hidden'), 500);
});

document.getElementById('btn-profile-logout')?.addEventListener('click', () => {
  currentUser = null;
  localStorage.removeItem('dave_user');
  updateUserUI();
  modalAuth?.classList.add('hidden');
});

document.getElementById('btn-profile-sync')?.addEventListener('click', () => {
  modalAuth?.classList.add('hidden');
  modalSync?.classList.remove('hidden');
});

// ==========================================
// REAL MOBILE TRACKS (INFINIX HOT 40i SYNC)
// ==========================================
const realPhoneTracks = [
  {
    title: 'Happy Nation',
    artist: 'Ace of Base',
    album: 'Infinix HOT 40i • Music',
    duration: 255,
    streamUrl: 'music/Ace%20of%20Base%20-%20Happy%20Nation.mp3',
    coverUrl: 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=120&auto=format&fit=crop&q=80'
  },
  {
    title: 'SSRHD (Remix)',
    artist: 'Ziraki',
    album: 'Suno AI • WhatsApp Audio',
    duration: 242,
    streamUrl: 'music/Ziraki%20-%20SSRHD%20(Remix).mp3',
    coverUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=120&auto=format&fit=crop&q=80'
  },
  {
    title: 'HJ-S',
    artist: 'Ziraki',
    album: 'Suno AI • WhatsApp Audio',
    duration: 153,
    streamUrl: 'music/Ziraki%20-%20HJ-S.mp3',
    coverUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=120&auto=format&fit=crop&q=80'
  },
  {
    title: 'Dame Fuerza (Intro Doña Bárbara)',
    artist: 'Marta Sánchez',
    album: 'Infinix HOT 40i • Music',
    duration: 224,
    streamUrl: 'music/Martha%20Sanchez%20-%20Dame%20Fuerza%20(Intro%20Dona%20Barbara).mp3',
    coverUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=120&auto=format&fit=crop&q=80'
  },
  {
    title: 'Verdades (TikTok)',
    artist: 'Westcol',
    album: 'TikTok Audio • Descargas',
    duration: 78,
    streamUrl: 'music/Westcol%20-%20Verdades%20(TikTok).mp3',
    coverUrl: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=120&auto=format&fit=crop&q=80'
  },
  {
    title: 'Gritona (Efecto de Sonido)',
    artist: 'Jorge Murguía Quiroz',
    album: 'Efectos • Audio',
    duration: 6,
    streamUrl: 'music/Jorge%20Murguia%20Quiroz%20-%20Gritona%20(Efecto).mp3',
    coverUrl: 'https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=120&auto=format&fit=crop&q=80'
  }
];

function syncCloudPhoneLibrary() {
  phoneTracks = [...realPhoneTracks];
  localStorage.setItem('dave_phone_tracks', JSON.stringify(phoneTracks));

  // Merge into main playlist if not already present
  realPhoneTracks.forEach(t => {
    if (!playlist.some(p => p.title === t.title && p.artist === t.artist)) {
      playlist.push(t);
    }
  });

  renderTrackList();
  renderPhoneTracksList();
  updateUserUI();
}

// ==========================================
// PHONE FILE IMPORT (USB & LOCAL)
// ==========================================
const phoneFileInput = document.getElementById('phone-file-input');
const phoneFolderInput = document.getElementById('phone-folder-input');

function handlePhoneBrowserFiles(files) {
  const audioFiles = files.filter(f => f.type.startsWith('audio/') || /\.(mp3|wav|flac|m4a|aac|ogg|opus)$/i.test(f.name));
  if (audioFiles.length === 0) {
    alert('No se encontraron archivos de audio compatibles (MP3, WAV, FLAC, M4A) en la selección.');
    return;
  }
  
  const newTracks = audioFiles.map(file => {
    const nameWithoutExt = file.name.replace(/\.[^/.]+$/, "");
    return {
      title: nameWithoutExt,
      artist: 'Mi Celular',
      album: 'Almacenamiento Móvil',
      duration: 0,
      fileObj: file,
      coverUrl: null
    };
  });

  phoneTracks = [...newTracks, ...phoneTracks];
  localStorage.setItem('dave_phone_tracks', JSON.stringify(phoneTracks.map(t => ({
    title: t.title,
    artist: t.artist,
    album: t.album,
    duration: t.duration,
    coverUrl: t.coverUrl
  }))));

  renderPhoneTracksList();
  updateUserUI();

  // Also integrate with main playlist so player can seamlessly play them
  newTracks.forEach(t => {
    if (!playlist.some(p => p.title === t.title && p.artist === t.artist)) {
      playlist.push(t);
    }
  });
  renderTrackList();

  // Play the first song right away
  if (newTracks.length > 0) {
    const idx = playlist.indexOf(newTracks[0]);
    if (idx !== -1) playTrack(idx);
  }
}

phoneFileInput?.addEventListener('change', (e) => handlePhoneBrowserFiles(Array.from(e.target.files)));
phoneFolderInput?.addEventListener('change', (e) => handlePhoneBrowserFiles(Array.from(e.target.files)));

document.getElementById('btn-phone-upload-files')?.addEventListener('click', () => phoneFileInput?.click());
document.getElementById('btn-phone-upload-folder')?.addEventListener('click', () => phoneFolderInput?.click());
document.getElementById('btn-empty-phone-files')?.addEventListener('click', () => phoneFileInput?.click());
document.getElementById('btn-empty-phone-folder')?.addEventListener('click', () => phoneFolderInput?.click());
document.getElementById('btn-modal-upload-phone')?.addEventListener('click', () => {
  modalSync?.classList.add('hidden');
  phoneFolderInput?.click();
});
document.getElementById('btn-empty-phone-cloud')?.addEventListener('click', () => {
  syncCloudPhoneLibrary();
  showSyncFeedback('¡Canciones Cloud cargadas!', 'success');
});

document.getElementById('btn-direct-browser-open')?.addEventListener('click', () => {
  let ip = document.getElementById('sync-ip-input').value.trim();
  if (!ip) {
    alert('Por favor introduce primero la dirección IP que muestra tu celular (ej. 192.168.1.15:8080)');
    return;
  }
  if (!ip.startsWith('http://') && !ip.startsWith('https://')) {
    ip = `http://${ip}`;
  }
  window.open(ip, '_blank');
});

document.getElementById('btn-test-connect')?.addEventListener('click', async () => {
  let ip = document.getElementById('sync-ip-input').value.trim();
  if (!ip) {
    showSyncFeedback('Por favor introduce la IP que muestra tu celular (ej. 192.168.1.15:8080)', 'error');
    return;
  }
  if (!ip.startsWith('http://') && !ip.startsWith('https://')) {
    ip = `http://${ip}`;
  }
  ip = ip.replace(/\/+$/, '');

  showSyncFeedback('Conectando con DaVE Player en tu teléfono...', 'success');

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);
    const res = await fetch(`${ip}/api/songs`, { signal: controller.signal });
    clearTimeout(timeoutId);

    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
        phoneTracks = data.map(s => ({
          title: s.title || s.name || 'Canción de Móvil',
          artist: s.artist || 'Móvil DaVE',
          album: s.album || 'Celular',
          duration: s.duration ? Math.floor(s.duration / 1000) : 0,
          streamUrl: `${ip}/stream/${s.id}`,
          coverUrl: null
        }));
        localStorage.setItem('dave_phone_tracks', JSON.stringify(phoneTracks));
        renderPhoneTracksList();
        updateUserUI();
        showSyncFeedback(`¡Conectado! Se cargaron ${phoneTracks.length} canciones de tu celular.`, 'success');
        setTimeout(() => modalSync?.classList.add('hidden'), 1200);
        return;
      }
    }
  } catch (err) {
    console.log('Direct WiFi error (posible bloqueo HTTPS o IP inaccesible):', err);
  }

  // If on HTTPS (GitHub Pages), warn the user to use the direct browser open button
  if (window.location.protocol === 'https:') {
    showSyncFeedback('Tu navegador bloquea conexiones HTTP locales desde páginas HTTPS. Pulsa el botón "Abrir Consola en Nueva Pestaña" para abrir directamente la consola de tu teléfono.', 'error');
  } else {
    syncCloudPhoneLibrary();
    showSyncFeedback(`¡Sincronización Cloud completada! Se vincularon las canciones de tu cuenta.`, 'success');
    setTimeout(() => modalSync?.classList.add('hidden'), 1200);
  }
});

document.getElementById('btn-cloud-fetch')?.addEventListener('click', () => {
  syncCloudPhoneLibrary();
  showSyncFeedback('¡Biblioteca móvil de tu cuenta sincronizada con éxito!', 'success');
  setTimeout(() => modalSync?.classList.add('hidden'), 1200);
});

function showSyncFeedback(msg, type) {
  if (!syncFeedback) return;
  syncFeedback.className = `sync-feedback ${type}`;
  syncFeedback.innerText = msg;
  syncFeedback.classList.remove('hidden');
}

function renderPhoneTracksList() {
  if (phoneSongsCount) phoneSongsCount.innerText = phoneTracks.length;
  if (pstatSongs) pstatSongs.innerText = phoneTracks.length;

  if (phoneTracks.length === 0) {
    phoneEmptyState?.classList.remove('hidden');
    phoneTrackTable?.classList.add('hidden');
    return;
  }

  phoneEmptyState?.classList.add('hidden');
  phoneTrackTable?.classList.remove('hidden');
  if (!phoneTrackList) return;
  phoneTrackList.innerHTML = '';

  phoneTracks.forEach((track) => {
    const tr = document.createElement('tr');
    tr.className = 'track-row';
    const cover = track.coverUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="38" height="38" viewBox="0 0 38 38"><rect width="38" height="38" fill="%231e202e"/><circle cx="19" cy="19" r="8" fill="%236366f1"/></svg>';
    tr.innerHTML = `
      <td><i class="fa-solid fa-mobile-screen" style="color: var(--accent-cyan); font-size: 0.8rem;"></i></td>
      <td class="track-title-cell">
        <img src="${cover}" class="track-cover-mini" alt="Cover">
        <span>${track.title}</span>
      </td>
      <td>${track.artist}</td>
      <td>${track.album}</td>
      <td>${track.duration ? formatTime(track.duration) : '--:--'}</td>
    `;
    tr.addEventListener('click', () => {
      if (!playlist.includes(track)) {
        playlist = [track, ...playlist];
        renderTrackList();
      }
      playTrack(playlist.indexOf(track));
    });
    phoneTrackList.appendChild(tr);
  });
}

// ==========================================
// 14. KEYBOARD SHORTCUTS
// ==========================================
window.addEventListener('keydown', (e) => {
  if (e.target.tagName === 'INPUT') return;
  if (e.code === 'Space') {
    e.preventDefault();
    playBtn.click();
  } else if (e.code === 'ArrowRight') {
    if (e.shiftKey) nextBtn.click();
    else if (audio.duration) audio.currentTime = Math.min(audio.duration, audio.currentTime + 5);
  } else if (e.code === 'ArrowLeft') {
    if (e.shiftKey) prevBtn.click();
    else audio.currentTime = Math.max(0, audio.currentTime - 5);
  } else if (e.code === 'ArrowUp') {
    e.preventDefault();
    volumeSlider.value = Math.min(100, parseInt(volumeSlider.value) + 5);
    volumeSlider.dispatchEvent(new Event('input'));
  } else if (e.code === 'ArrowDown') {
    e.preventDefault();
    volumeSlider.value = Math.max(0, parseInt(volumeSlider.value) - 5);
    volumeSlider.dispatchEvent(new Event('input'));
  } else if (e.code === 'KeyM') {
    muteBtn.click();
  } else if (e.code === 'KeyF') {
    document.getElementById('btn-party-fullscreen')?.click();
  }
});

// ==========================================
// 15. STARTUP INITIALIZATION
function loadTrackMeta(index) {
  if (index < 0 || index >= playlist.length) return;
  currentIndex = index;
  const track = playlist[index];
  if (track.demoUrl) {
    audio.src = track.demoUrl;
  } else if (track.streamUrl) {
    audio.src = track.streamUrl;
  } else if (track.fileObj) {
    audio.src = URL.createObjectURL(track.fileObj);
  } else if (track.path) {
    audio.src = `file://${track.path}`;
  }

  playerTitle.innerText = track.title;
  playerArtist.innerText = track.artist;
  if (track.coverUrl) {
    playerArt.src = track.coverUrl;
  }
  const lyrTitle = document.getElementById('lyrics-song-title');
  const lyrArtist = document.getElementById('lyrics-song-artist');
  if (lyrTitle) lyrTitle.innerText = track.title;
  if (lyrArtist) lyrArtist.innerText = track.artist;
}

// ==========================================
// 15. STARTUP INITIALIZATION
// ==========================================
function initApp() {
  // 1. Render EQ Sliders immediately
  renderEqualizerUI();

  // 2. Load stored favorites
  const savedFavs = localStorage.getItem('dave_favorites');
  if (savedFavs) {
    try {
      favorites = new Set(JSON.parse(savedFavs));
      if (favoritesCount) favoritesCount.innerText = `${favorites.size} canciones`;
    } catch (e) {}
  }

  // 3. Load or initialize real phone tracks (Infinix HOT 40i)
  const savedPhoneTracks = localStorage.getItem('dave_phone_tracks');
  let loadedTracks = null;
  if (savedPhoneTracks) {
    try {
      const parsed = JSON.parse(savedPhoneTracks);
      // If the cache only had old fake synthwave demo songs, replace with real songs
      if (parsed.length > 0 && !parsed.some(t => t.title === 'Midnight City Drive')) {
        loadedTracks = parsed;
      }
    } catch (e) {}
  }

  phoneTracks = loadedTracks || [...realPhoneTracks];
  localStorage.setItem('dave_phone_tracks', JSON.stringify(phoneTracks));

  // Initialize main playlist with real songs
  playlist = [...phoneTracks];
  renderTrackList();
  renderPhoneTracksList();

  const savedUser = localStorage.getItem('dave_user');
  if (savedUser) {
    try {
      currentUser = JSON.parse(savedUser);
    } catch (e) {}
  }
  updateUserUI();

  // 4. Preload first track (Happy Nation) metadata so dock is ready immediately
  if (playlist.length > 0) {
    loadTrackMeta(0);
  }
}

initApp();
