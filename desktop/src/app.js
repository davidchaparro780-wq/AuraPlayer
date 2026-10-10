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
let normalizerCompressor = null;
let isNormalizerActive = localStorage.getItem('dave_normalizer') === 'true';

let convolverNode = null;
let reverbDryGain = null;
let reverbWetGain = null;
let currentReverbPreset = 'off';

let sleepTimerInterval = null;
let sleepTimerRemaining = 0;
let sleepTimerMode = null; // null, 'minutes', 'end-track'

let userPlaylists = [];
let activePlaylistId = null;

let pipDrawInterval = null;

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
// 2.5 TOAST NOTIFICATIONS SYSTEM
// ==========================================
function showToast(message, type = 'info', icon = null) {
  const container = document.getElementById('toast-container');
  if (!container) return;

  // Máximo 2 notificaciones para no saturar la vista
  while (container.children.length >= 2) {
    container.firstElementChild.remove();
  }

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;

  let defaultIcon = 'fa-circle-info';
  if (type === 'success') defaultIcon = 'fa-circle-check';
  else if (type === 'heart') defaultIcon = 'fa-heart';
  else if (type === 'warning') defaultIcon = 'fa-triangle-exclamation';

  const iconClass = icon ? (icon.startsWith('fa-') ? icon : `fa-${icon}`) : defaultIcon;
  toast.innerHTML = `<i class="fa-solid ${iconClass}"></i> <span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('removing');
    setTimeout(() => toast.remove(), 260);
  }, 2400);
}

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

  // 3. Normalizer Dynamics Compressor (Smart Auto-Volume)
  normalizerCompressor = audioCtx.createDynamicsCompressor();
  if (isNormalizerActive) {
    normalizerCompressor.threshold.value = -24;
    normalizerCompressor.knee.value = 30;
    normalizerCompressor.ratio.value = 12;
    normalizerCompressor.attack.value = 0.003;
    normalizerCompressor.release.value = 0.25;
  } else {
    normalizerCompressor.threshold.value = 0;
    normalizerCompressor.ratio.value = 1;
  }
  lastNode.connect(normalizerCompressor);
  lastNode = normalizerCompressor;

  // 4. Analyser Node for FFT Visualizer & VU meters
  analyserNode = audioCtx.createAnalyser();
  analyserNode.fftSize = 512;
  analyserNode.smoothingTimeConstant = 0.8;
  lastNode.connect(analyserNode);

  // 5. Spatial Reverb Engine (Convolver + Dry/Wet gains)
  convolverNode = audioCtx.createConvolver();
  reverbDryGain = audioCtx.createGain();
  reverbWetGain = audioCtx.createGain();
  reverbDryGain.gain.value = 1.0;
  reverbWetGain.gain.value = 0.0;

  analyserNode.connect(reverbDryGain);
  analyserNode.connect(convolverNode);
  convolverNode.connect(reverbWetGain);

  // 6. Master Gain Node (Controls speaker volume without dampening analyser)
  masterGain = audioCtx.createGain();
  masterGain.gain.value = currentVolume;
  reverbDryGain.connect(masterGain);
  reverbWetGain.connect(masterGain);
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

    // Mini wave bars in bottom player
    const miniBars = document.getElementById('mini-wave-bars');
    if (miniBars) {
      if (isPlaying) {
        miniBars.classList.add('active');
        const spans = miniBars.children;
        if (spans.length >= 4) {
          const h1 = Math.max(4, Math.min(18, (dataArray[2] / 255) * 18));
          const h2 = Math.max(4, Math.min(18, (dataArray[6] / 255) * 18));
          const h3 = Math.max(4, Math.min(18, (dataArray[14] / 255) * 18));
          const h4 = Math.max(4, Math.min(18, (dataArray[24] / 255) * 18));
          spans[0].style.height = `${h1}px`;
          spans[1].style.height = `${h2}px`;
          spans[2].style.height = `${h3}px`;
          spans[3].style.height = `${h4}px`;
        }
      } else {
        miniBars.classList.remove('active');
        const spans = miniBars.children;
        for (let i = 0; i < spans.length; i++) spans[i].style.height = '4px';
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

  // If already playing this track, toggle play/pause smoothly
  if (currentIndex === index && audio.src) {
    if (isPlaying) {
      audio.pause();
      isPlaying = false;
      updatePlayPauseUI();
      renderTrackList();
      renderPhoneTracksList();
    } else {
      audio.play().then(() => {
        isPlaying = true;
        updatePlayPauseUI();
        renderTrackList();
        renderPhoneTracksList();
      });
    }
    return;
  }

  currentIndex = index;
  const track = playlist[index];

  // Smooth Crossfade & Anti-Click: subtle volume dip before new track
  if (masterGain && audioCtx && audioCtx.state === 'running') {
    try {
      masterGain.gain.cancelScheduledValues(audioCtx.currentTime);
      masterGain.gain.setValueAtTime(masterGain.gain.value, audioCtx.currentTime);
      masterGain.gain.linearRampToValueAtTime(0.08, audioCtx.currentTime + 0.08);
    } catch (e) {}
  }

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
    showToast(`Reproduciendo: ${track.title}`, 'info', 'fa-play');

    // Ramp volume back up smoothly to currentVolume (velvety crossfade)
    if (masterGain && audioCtx) {
      try {
        masterGain.gain.cancelScheduledValues(audioCtx.currentTime);
        masterGain.gain.setValueAtTime(0.1, audioCtx.currentTime);
        masterGain.gain.linearRampToValueAtTime(currentVolume, audioCtx.currentTime + 0.22);
      } catch (e) {}
    }
  }).catch(err => {
    console.warn('Playback error / autoplay blocked:', err);
    isPlaying = false;
    updatePlayPauseUI();
  });

  playerTitle.innerText = track.title;
  playerArtist.innerText = track.artist;
  if (track.coverUrl) {
    playerArt.src = track.coverUrl;
    applyDynamicArtworkPalette(track.coverUrl);
  } else {
    playerArt.src = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><rect width="100" height="100" fill="%23181824"/><circle cx="50" cy="50" r="25" fill="%236366f1"/></svg>';
    applyDynamicArtworkPalette(null);
  }
  artGlow.style.opacity = '1';

  // System Media Controls & Queue Sync
  setupMediaSession(track);
  renderQueueDrawer();
  if (typeof updateFloatingMiniUI === 'function') updateFloatingMiniUI();

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
  renderPhoneTracksList();
}

function updatePlayPauseUI() {
  if (isPlaying) {
    playBtn.innerHTML = '<i class="fa-solid fa-pause"></i>';
    artGlow.style.opacity = '1';
  } else {
    playBtn.innerHTML = '<i class="fa-solid fa-play"></i>';
    artGlow.style.opacity = '0';
  }
  if (typeof updateFloatingMiniUI === 'function') updateFloatingMiniUI();
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

  // Sync native MediaSession position state (lock screen & Windows widget)
  if ('mediaSession' in navigator && 'setPositionState' in navigator.mediaSession) {
    try {
      navigator.mediaSession.setPositionState({
        duration: audio.duration,
        playbackRate: audio.playbackRate || 1.0,
        position: audio.currentTime
      });
    } catch (e) {}
  }
});

// Audio Ended Event
audio.addEventListener('ended', () => {
  if (sleepTimerMode === 'end-track') {
    cancelSleepTimer();
    audio.pause();
    isPlaying = false;
    updatePlayPauseUI();
    showToast('🌙 Temporizador finalizado al terminar la canción.', 'info', 'fa-moon');
    return;
  }
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

// Scrubber Seeking & Interactive Hover Tooltip
let isScrubbing = false;

progressContainer?.addEventListener('mousemove', (e) => {
  if (!audio.duration || !isFinite(audio.duration)) return;
  const rect = progressContainer.getBoundingClientRect();
  const pos = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
  const hoverTime = pos * audio.duration;
  const tooltip = document.getElementById('scrubber-tooltip');
  if (tooltip) {
    tooltip.innerText = formatTime(hoverTime);
    tooltip.style.left = `${(pos * 100).toFixed(1)}%`;
  }
  if (isScrubbing) {
    audio.currentTime = hoverTime;
    progressFill.style.width = `${pos * 100}%`;
    currentTimeEl.innerText = formatTime(hoverTime);
  }
});

progressContainer?.addEventListener('mousedown', (e) => {
  if (!audio.duration || !isFinite(audio.duration)) return;
  isScrubbing = true;
  const rect = progressContainer.getBoundingClientRect();
  const pos = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
  audio.currentTime = pos * audio.duration;
  progressFill.style.width = `${pos * 100}%`;
  currentTimeEl.innerText = formatTime(audio.currentTime);
});

window.addEventListener('mouseup', () => {
  isScrubbing = false;
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
    showToast(`Volumen: ${Math.round(currentVolume * 100)}%`, 'info', 'fa-volume-high');
  } else {
    previousVolume = currentVolume;
    currentVolume = 0;
    volumeSlider.value = 0;
    if (masterGain) masterGain.gain.value = 0;
    isMuted = true;
    showToast('Sonido silenciado', 'info', 'fa-volume-xmark');
  }
  updateVolumeIcon(currentVolume);
});

// Favorite Toggle Helper
function toggleFavorite(track) {
  if (!track) return;
  const trackId = track.title + track.artist;
  let isFav = false;

  if (favorites.has(trackId)) {
    favorites.delete(trackId);
    isFav = false;
    showToast(`Eliminada de Favoritas: ${track.title}`, 'info', 'fa-heart');
  } else {
    favorites.add(trackId);
    isFav = true;
    showToast(`Añadida a Favoritas: ${track.title}`, 'heart', 'fa-heart');
  }

  // Update badges
  if (favoritesCount) favoritesCount.innerText = favorites.size;
  const filterFavCount = document.getElementById('filter-favs-count');
  if (filterFavCount) filterFavCount.innerText = favorites.size;
  if (pstatFavs) pstatFavs.innerText = favorites.size;

  // Update bottom player like button if playing this song
  if (currentIndex !== -1 && playlist[currentIndex] && (playlist[currentIndex].title + playlist[currentIndex].artist) === trackId) {
    if (isFav) {
      btnLike?.classList.add('active');
      if (btnLike) btnLike.innerHTML = '<i class="fa-solid fa-heart"></i>';
    } else {
      btnLike?.classList.remove('active');
      if (btnLike) btnLike.innerHTML = '<i class="fa-regular fa-heart"></i>';
    }
  }

  localStorage.setItem('dave_favorites', JSON.stringify(Array.from(favorites)));
  if (currentFilter === 'favs') {
    applyCurrentFilter();
  } else {
    renderTrackList();
    renderPhoneTracksList();
  }
}

btnLike?.addEventListener('click', () => {
  if (currentIndex === -1 || playlist.length === 0) return;
  toggleFavorite(playlist[currentIndex]);
});

// Favorites Playlist Card Click
document.querySelector('.favorites-card')?.addEventListener('click', () => {
  switchTab('songs');
  const favChip = document.querySelector('.filter-chip[data-filter="favs"]');
  if (favChip) {
    favChip.click();
  }
  const favSongs = playlist.filter(t => favorites.has(t.title + t.artist));
  if (favSongs.length === 0) {
    showToast('No tienes favoritas aún. Pulsa el corazón ❤️ en cualquier canción para guardarla.', 'info', 'fa-heart');
  }
});

// ==========================================
// 9. UNIFIED TRACK ROW BUILDER & CATEGORIES
// ==========================================
function createTrackRow(track, displayIndex, isPhoneTab = false) {
  const tr = document.createElement('tr');
  const isThisPlaying = (currentIndex !== -1 && playlist[currentIndex] === track);
  tr.className = `track-row ${isThisPlaying ? 'playing' : ''}`;
  const cover = track.coverUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="38" height="38" viewBox="0 0 38 38"><rect width="38" height="38" fill="%231e202e"/><circle cx="19" cy="19" r="8" fill="%236366f1"/></svg>';
  const trackId = track.title + track.artist;
  const isFav = favorites.has(trackId);

  tr.innerHTML = `
    <td class="row-num-cell">
      ${isThisPlaying ? `
        <div class="sound-wave-bars ${isPlaying ? '' : 'paused'}">
          <span></span><span></span><span></span><span></span>
        </div>
      ` : `
        <span class="row-index-num">${displayIndex + 1}</span>
        <i class="fa-solid fa-play row-hover-play"></i>
      `}
    </td>
    <td class="track-title-cell">
      <img src="${cover}" class="track-cover-mini" style="width:44px; height:44px; min-width:44px; max-width:44px; border-radius:8px; object-fit:cover; flex-shrink:0;" alt="Cover" loading="lazy" onerror="this.src='https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=120&auto=format&fit=crop&q=80'">
      <div class="track-meta-col" style="display:flex; flex-direction:column; overflow:hidden; min-width:0;">
        <span class="track-title-text" style="font-weight:600; white-space:nowrap; text-overflow:ellipsis; overflow:hidden; font-size:0.9rem; color:#fff;">${track.title}</span>
        <span class="track-artist-sub mobile-sub-artist" style="font-size:0.75rem; color:var(--text-dim); white-space:nowrap; text-overflow:ellipsis; overflow:hidden;">${track.artist}</span>
      </div>
    </td>
    <td class="track-artist-col" style="white-space:nowrap; text-overflow:ellipsis; overflow:hidden;">${track.artist}</td>
    <td class="track-album-col" style="white-space:nowrap; text-overflow:ellipsis; overflow:hidden;">${track.album || 'Infinix HOT 40i'}</td>
    <td class="track-duration-col" style="text-align:right; font-variant-numeric:tabular-nums;">${track.duration ? formatTime(track.duration) : '--:--'}</td>
    <td>
      <div class="row-actions-cell">
        <button class="btn-row-action heart ${isFav ? 'active' : ''}" title="${isFav ? 'Quitar de Favoritas' : 'Añadir a Favoritas'}" data-action="fav">
          <i class="${isFav ? 'fa-solid' : 'fa-regular'} fa-heart"></i>
        </button>
        ${track.streamUrl ? `
          <a class="btn-row-action download" href="${track.streamUrl}" download="${track.title} - ${track.artist}.mp3" title="Descargar MP3 a tu PC" data-action="download">
            <i class="fa-solid fa-arrow-down-to-bracket"></i>
          </a>
        ` : ''}
      </div>
    </td>
  `;

  tr.addEventListener('click', (e) => {
    const actionBtn = e.target.closest('[data-action]');
    if (actionBtn) {
      const action = actionBtn.dataset.action;
      if (action === 'fav') {
        e.stopPropagation();
        toggleFavorite(track);
        return;
      } else if (action === 'download') {
        e.stopPropagation();
        showToast(`Descargando: ${track.title}`, 'success', 'fa-arrow-down-to-bracket');
        return;
      }
    }

    if (!playlist.includes(track)) {
      playlist.push(track);
    }
    const targetIdx = playlist.indexOf(track);
    if (currentIndex === targetIdx && audio.src) {
      if (isPlaying) {
        audio.pause();
        isPlaying = false;
        updatePlayPauseUI();
        renderTrackList();
        renderPhoneTracksList();
      } else {
        audio.play().then(() => {
          isPlaying = true;
          updatePlayPauseUI();
          renderTrackList();
          renderPhoneTracksList();
        });
      }
    } else {
      playTrack(targetIdx);
    }
  });

  // Right-click context menu (Aislar voz, Reproducir siguiente, Letras, etc.)
  tr.addEventListener('contextmenu', (e) => {
    openContextMenu(e, track);
  });

  return tr;
}

let currentFilter = 'all';

function normalizeStr(str) {
  return (str || '')
    .toString()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
}

function escapeHtml(text) {
  if (!text) return '';
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

const searchEmptyState = document.getElementById('search-empty-state');
const searchEmptyText = document.getElementById('search-empty-text');

function renderTrackList(filtered = null, searchQuery = '') {
  const list = filtered || playlist;
  if (songsCount) songsCount.innerText = list.length;
  const filterAllCount = document.getElementById('filter-all-count');
  if (filterAllCount) filterAllCount.innerText = playlist.length;
  const filterFavCount = document.getElementById('filter-favs-count');
  if (filterFavCount) filterFavCount.innerText = favorites.size;

  if (list.length === 0) {
    trackTable?.classList.add('hidden');
    if (searchQuery) {
      emptyState?.classList.add('hidden');
      if (searchEmptyState) {
        searchEmptyState.classList.remove('hidden');
        if (searchEmptyText) {
          searchEmptyText.innerHTML = `No se encontraron canciones que coincidan con "<strong>${escapeHtml(searchQuery)}</strong>".`;
        }
      }
    } else {
      searchEmptyState?.classList.add('hidden');
      emptyState?.classList.remove('hidden');
    }
    return;
  }

  searchEmptyState?.classList.add('hidden');
  emptyState?.classList.add('hidden');
  trackTable?.classList.remove('hidden');
  if (!trackList) return;
  trackList.innerHTML = '';

  list.forEach((track, i) => {
    const tr = createTrackRow(track, i);
    trackList.appendChild(tr);
  });
}

function applyCurrentFilter() {
  const rawQ = searchInput?.value || '';
  const q = normalizeStr(rawQ);

  if (searchClearBtn) {
    searchClearBtn.style.display = rawQ ? 'block' : 'none';
  }

  // Si el usuario escribe y está en otra pestaña que no es canciones ni celular, cambiar a canciones
  const activePane = document.querySelector('.tab-pane.active');
  const activeTabId = activePane ? activePane.id : '';
  if (q && activeTabId !== 'tab-songs' && activeTabId !== 'tab-phone') {
    switchTab('songs');
  }

  let list = playlist;

  if (currentFilter === 'urban') {
    list = playlist.filter(t => /bad bunny|blessd|cris mj|westcol|beéle|daddy yankee|urbano|trap/i.test(`${t.title} ${t.artist} ${t.album}`));
  } else if (currentFilter === 'pop') {
    list = playlist.filter(t => /lady gaga|rihanna|ace of base|bôa|pop|dance/i.test(`${t.title} ${t.artist} ${t.album}`));
  } else if (currentFilter === 'cyber') {
    list = playlist.filter(t => /cyberpunk|dawid|edgerunners|topic|phantom|ziraki|sound/i.test(`${t.title} ${t.artist} ${t.album}`));
  } else if (currentFilter === 'favs') {
    list = playlist.filter(t => favorites.has(t.title + t.artist));
  }

  if (q) {
    list = list.filter(t => {
      const matchTitle = normalizeStr(t.title).includes(q);
      const matchArtist = normalizeStr(t.artist).includes(q);
      const matchAlbum = normalizeStr(t.album).includes(q);
      return matchTitle || matchArtist || matchAlbum;
    });
  }

  renderTrackList(list, rawQ);

  // Filtrar sincronizadamente las canciones del celular en su pestaña
  let phoneList = phoneTracks;
  if (q) {
    phoneList = phoneTracks.filter(t => {
      const matchTitle = normalizeStr(t.title).includes(q);
      const matchArtist = normalizeStr(t.artist).includes(q);
      const matchAlbum = normalizeStr(t.album).includes(q);
      return matchTitle || matchArtist || matchAlbum;
    });
  }
  renderPhoneTracksList(phoneList, rawQ);
}

function setupFilterChips() {
  const container = document.getElementById('category-chips');
  if (!container) return;

  container.querySelectorAll('.filter-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      container.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      currentFilter = chip.dataset.filter;
      applyCurrentFilter();
    });
  });
}

// Search Filter & Clear Button
const searchClearBtn = document.getElementById('search-clear-btn');
searchInput?.addEventListener('input', () => {
  applyCurrentFilter();
});

function clearGlobalSearch() {
  if (searchInput) {
    searchInput.value = '';
    if (searchClearBtn) searchClearBtn.style.display = 'none';
    searchInput.focus();
    applyCurrentFilter();
  }
}

searchClearBtn?.addEventListener('click', clearGlobalSearch);
document.getElementById('btn-search-clear-action')?.addEventListener('click', clearGlobalSearch);
document.getElementById('btn-phone-search-clear-action')?.addEventListener('click', clearGlobalSearch);

searchInput?.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    clearGlobalSearch();
  }
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

// Full Window Drag & Drop Overlay Handlers
const dragOverlay = document.getElementById('drag-drop-overlay');
let dragCounter = 0;

window.addEventListener('dragenter', (e) => {
  e.preventDefault();
  dragCounter++;
  if (dragOverlay) dragOverlay.classList.add('active');
});

window.addEventListener('dragleave', (e) => {
  e.preventDefault();
  dragCounter--;
  if (dragCounter <= 0) {
    dragCounter = 0;
    if (dragOverlay) dragOverlay.classList.remove('active');
  }
});

window.addEventListener('dragover', (e) => {
  e.preventDefault();
  e.stopPropagation();
});

window.addEventListener('drop', (e) => {
  e.preventDefault();
  e.stopPropagation();
  dragCounter = 0;
  if (dragOverlay) dragOverlay.classList.remove('active');

  if (e.dataTransfer && e.dataTransfer.files.length > 0) {
    const files = Array.from(e.dataTransfer.files);
    const activeTab = document.querySelector('.tab-pane.active');
    if (activeTab && activeTab.id === 'tab-phone') {
      handlePhoneBrowserFiles(files);
      showToast(`${files.length} archivo(s) añadidos al Celular`, 'success', 'fa-mobile-screen');
    } else {
      handleBrowserFiles(files);
      showToast(`${files.length} archivo(s) añadidos a tu Biblioteca`, 'success', 'fa-music');
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
document.getElementById('btn-banner-close')?.addEventListener('click', () => {
  const banner = document.getElementById('welcome-banner');
  if (banner) {
    banner.classList.add('dismissed');
    sessionStorage.setItem('dave_banner_dismissed', 'true');
  }
});
if (sessionStorage.getItem('dave_banner_dismissed') === 'true') {
  document.getElementById('welcome-banner')?.classList.add('dismissed');
}

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
  } else if (name === 'playlists') {
    renderPlaylistsTab();
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
const authDisconnectBanner = document.getElementById('auth-disconnect-banner');
const btnPhoneDisconnect = document.getElementById('btn-phone-disconnect');
const syncActiveBox = document.getElementById('sync-active-box');
const syncConnectedIpText = document.getElementById('sync-connected-ip-text');
const btnModalDisconnectServer = document.getElementById('btn-modal-disconnect-server');

let connectedPhoneIp = null;
let phoneHeartbeatInterval = null;

function updatePhoneConnectionUI(isConnected, ip = '') {
  if (isConnected) {
    connectedPhoneIp = ip;
    btnPhoneDisconnect?.classList.remove('hidden');
    syncActiveBox?.classList.remove('hidden');
    if (syncConnectedIpText) syncConnectedIpText.innerText = ip;
    if (phoneSyncStatus) {
      phoneSyncStatus.innerHTML = `<span style="color:#10b981; font-weight:600;">● Servidor Conectado</span> en <code style="color:var(--accent-cyan); font-family:monospace;">${escapeHtml(ip)}</code> • ${phoneTracks.length} canciones sincronizadas.`;
    }
  } else {
    connectedPhoneIp = null;
    btnPhoneDisconnect?.classList.add('hidden');
    syncActiveBox?.classList.add('hidden');
    if (syncConnectedIpText) syncConnectedIpText.innerText = '';
    if (phoneSyncStatus) {
      phoneSyncStatus.innerText = currentUser 
        ? `Sincronizado con ${currentUser.email} • ${phoneTracks.length} canciones de tu nube disponibles.`
        : 'Inicia sesión con tu correo o conecta tu celular por WiFi/PIN para ver tus canciones aquí.';
    }
  }
}

function startPhoneHeartbeat(ip) {
  stopPhoneHeartbeat();
  updatePhoneConnectionUI(true, ip);
  localStorage.setItem('dave_connected_phone_ip', ip);

  let consecutiveFailures = 0;
  phoneHeartbeatInterval = setInterval(async () => {
    if (!connectedPhoneIp) return;
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 2500);
      const res = await fetch(`${connectedPhoneIp}/api/status`, { signal: controller.signal, cache: 'no-store' });
      clearTimeout(timeoutId);
      if (res.ok) {
        consecutiveFailures = 0;
      } else {
        consecutiveFailures++;
      }
    } catch (e) {
      consecutiveFailures++;
    }

    if (consecutiveFailures >= 2) {
      console.warn('Servidor del teléfono desconectado (heartbeat fallido):', connectedPhoneIp);
      disconnectPhoneServer(true);
    }
  }, 3500);
}

function stopPhoneHeartbeat() {
  if (phoneHeartbeatInterval) {
    clearInterval(phoneHeartbeatInterval);
    phoneHeartbeatInterval = null;
  }
}

function disconnectPhoneServer(isRemoteDisconnect = false) {
  try {
    stopPhoneHeartbeat();
    updatePhoneConnectionUI(false);
  } catch (e) {}
  localStorage.removeItem('dave_connected_phone_ip');
  sessionStorage.removeItem('dave_active_session');

  // 1. Detener audio inmediatamente
  try {
    if (audio) {
      audio.pause();
      audio.src = '';
    }
  } catch (e) {}
  isPlaying = false;
  currentIndex = -1;
  try {
    updatePlayPauseUI();
  } catch (e) {}

  // 2. VACIAR COMPLETAMENTE LAS LISTAS DE MÚSICA PARA QUE NO APAREZCA NADA
  playlist = [];
  phoneTracks = [];
  localStorage.removeItem('dave_phone_tracks');
  localStorage.removeItem('dave_cloud_tracks');
  try {
    renderTrackList();
    renderPhoneTracksList();
  } catch (e) {}

  if (playerTitle) playerTitle.innerText = 'Sin música';
  if (playerArtist) playerArtist.innerText = 'Inicia sesión para cargar tu música';
  if (playerArt) playerArt.src = 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&auto=format&fit=crop&q=80';

  // 3. Cerrar sesión activa
  currentUser = null;
  localStorage.removeItem('dave_user');
  try {
    updateUserUI();
  } catch (e) {}

  // 4. OCULTAR TODO EL REPRODUCTOR Y REGRESAR AL PRINCIPIO (PANTALLA DE INICIAR SESIÓN)
  const appViewport = document.querySelector('.app-viewport') || document.getElementById('app-viewport');
  if (appViewport) {
    appViewport.classList.add('hidden');
  }
  modalSync?.classList.add('hidden');
  modalAuth?.classList.add('hidden');

  const fullLogin = document.getElementById('full-login-screen');
  if (fullLogin) {
    fullLogin.classList.remove('hidden');
    const title = document.getElementById('full-login-status-title');
    const desc = document.getElementById('full-login-status-desc');
    if (title) title.innerText = isRemoteDisconnect ? 'Servidor del teléfono desconectado' : 'Servidor desconectado';
    if (desc) desc.innerText = 'Se ha cerrado la conexión con tu celular. Inicia sesión con tu cuenta para acceder a todas las canciones que tenías en la nube.';
    setTimeout(() => document.getElementById('full-login-email')?.focus(), 250);
  }

  showToast('⚠️ Servidor desconectado. Regresando al inicio de sesión...', 'warning', 'fa-power-off');
}

btnPhoneDisconnect?.addEventListener('click', () => disconnectPhoneServer(false));
btnModalDisconnectServer?.addEventListener('click', () => disconnectPhoneServer(false));

btnLoginModal?.addEventListener('click', () => {
  authDisconnectBanner?.classList.add('hidden');
  modalAuth?.classList.remove('hidden');
  updateUserUI();
});
btnCloseAuth?.addEventListener('click', () => {
  authDisconnectBanner?.classList.add('hidden');
  modalAuth?.classList.add('hidden');
});
btnCloseSync?.addEventListener('click', () => modalSync?.classList.add('hidden'));

document.getElementById('btn-open-sync-modal')?.addEventListener('click', () => modalSync?.classList.remove('hidden'));
document.getElementById('btn-empty-phone-connect')?.addEventListener('click', () => modalSync?.classList.remove('hidden'));
document.getElementById('btn-phone-sync-trigger')?.addEventListener('click', () => modalSync?.classList.remove('hidden'));

function updateUserUI() {
  const topbarLogout = document.getElementById('btn-topbar-logout');
  const sidebarAuthTitle = document.getElementById('sidebar-auth-title');
  const sidebarAuthList = document.getElementById('sidebar-auth-list');

  if (currentUser) {
    if (userDisplayName) userDisplayName.innerText = currentUser.name || currentUser.email.split('@')[0];
    if (profileName) profileName.innerText = currentUser.name || currentUser.email.split('@')[0];
    if (profileEmail) profileEmail.innerText = currentUser.email;
    if (pstatSongs) pstatSongs.innerText = phoneTracks.length;
    if (pstatFavs) pstatFavs.innerText = favorites.size;

    authFormContainer?.classList.add('hidden');
    authUserProfile?.classList.remove('hidden');
    if (phoneSyncStatus && !connectedPhoneIp) {
      phoneSyncStatus.innerText = `Sincronizado con ${currentUser.email} • ${phoneTracks.length} canciones de tu nube disponibles.`;
    }

    // Mostrar botones de cerrar sesión
    topbarLogout?.classList.remove('hidden');
    sidebarAuthTitle?.classList.remove('hidden');
    sidebarAuthList?.classList.remove('hidden');
  } else {
    if (userDisplayName) userDisplayName.innerText = 'Iniciar Sesión';
    authFormContainer?.classList.remove('hidden');
    authUserProfile?.classList.add('hidden');
    if (phoneSyncStatus && !connectedPhoneIp) {
      phoneSyncStatus.innerText = 'Inicia sesión con tu correo o conecta tu celular por WiFi/PIN para ver tus canciones aquí.';
    }

    // Ocultar botones de cerrar sesión
    topbarLogout?.classList.add('hidden');
    sidebarAuthTitle?.classList.add('hidden');
    sidebarAuthList?.classList.add('hidden');
  }
}

function handleSuccessfulLoginCloudSync(userObj = null) {
  if (userObj) {
    currentUser = userObj;
    localStorage.setItem('dave_user', JSON.stringify(currentUser));
  }
  sessionStorage.setItem('dave_active_session', 'true');

  // 1. Ocultar pantallas de login
  document.getElementById('full-login-screen')?.classList.add('hidden');
  authDisconnectBanner?.classList.add('hidden');
  modalAuth?.classList.add('hidden');

  // 2. Mostrar la interfaz completa de la app
  document.querySelector('.app-viewport')?.classList.remove('hidden');

  // 3. RESTAURAR Y CARGAR TODAS LAS CANCIONES QUE TENÍA EN LA NUBE
  phoneTracks = [...realPhoneTracks];
  playlist = [...realPhoneTracks];
  localStorage.setItem('dave_phone_tracks', JSON.stringify(phoneTracks));
  localStorage.setItem('dave_cloud_tracks', JSON.stringify(realPhoneTracks));

  // 4. Renderizar listas con toda la música
  renderTrackList();
  renderPhoneTracksList();
  updateUserUI();

  // 5. Ir a la pestaña de Canciones y precargar la primera pista
  switchTab('songs');
  if (playlist.length > 0) {
    loadTrackMeta(0);
  }

  showToast(`☁️ ¡Bienvenido, ${currentUser?.name || 'Usuario'}! Se cargaron ${realPhoneTracks.length} canciones de tu nube`, 'success', 'fa-cloud');
}

// Listeners de Formulario Modal Auth
authForm?.addEventListener('submit', (e) => {
  e.preventDefault();
  const email = document.getElementById('auth-email').value;
  const name = email.split('@')[0];
  handleSuccessfulLoginCloudSync({
    email: email,
    name: name.charAt(0).toUpperCase() + name.slice(1),
    loggedInAt: new Date().toISOString()
  });
});

document.getElementById('auth-link-demo')?.addEventListener('click', (e) => {
  e.preventDefault();
  handleSuccessfulLoginCloudSync({
    email: 'david.chaparro@daveplayer.app',
    name: 'David Chaparro',
    loggedInAt: new Date().toISOString()
  });
});

document.getElementById('btn-google-login')?.addEventListener('click', () => {
  handleSuccessfulLoginCloudSync({
    email: 'usuario.google@gmail.com',
    name: 'Usuario DaVE',
    loggedInAt: new Date().toISOString()
  });
});

// Listeners de Pantalla Completa Inicial (full-login-screen)
document.getElementById('full-login-form')?.addEventListener('submit', (e) => {
  e.preventDefault();
  const email = document.getElementById('full-login-email').value;
  const name = email.split('@')[0];
  handleSuccessfulLoginCloudSync({
    email: email,
    name: name.charAt(0).toUpperCase() + name.slice(1),
    loggedInAt: new Date().toISOString()
  });
});

document.getElementById('btn-full-login-demo')?.addEventListener('click', () => {
  handleSuccessfulLoginCloudSync({
    email: 'david.chaparro@daveplayer.app',
    name: 'David Chaparro',
    loggedInAt: new Date().toISOString()
  });
});

document.getElementById('btn-full-login-google')?.addEventListener('click', () => {
  handleSuccessfulLoginCloudSync({
    email: 'usuario.google@gmail.com',
    name: 'Usuario DaVE',
    loggedInAt: new Date().toISOString()
  });
});

function logoutUser() {
  // 1. Detener audio inmediatamente
  try {
    if (audio) {
      audio.pause();
      audio.src = '';
    }
  } catch (e) {
    console.error('Error al pausar audio:', e);
  }
  isPlaying = false;
  currentIndex = -1;
  try {
    updatePlayPauseUI();
  } catch (e) {}

  // 2. Limpiar sesión y almacenamiento local
  currentUser = null;
  sessionStorage.removeItem('dave_active_session');
  localStorage.removeItem('dave_user');
  localStorage.removeItem('dave_phone_tracks');
  localStorage.removeItem('dave_cloud_tracks');
  localStorage.removeItem('dave_connected_phone_ip');
  try {
    stopPhoneHeartbeat();
    updatePhoneConnectionUI(false);
  } catch (e) {}

  // 3. VACIAR COMPLETAMENTE LAS LISTAS DE MÚSICA PARA QUE NO APAREZCA NADA
  playlist = [];
  phoneTracks = [];
  try {
    renderTrackList();
    renderPhoneTracksList();
  } catch (e) {}

  if (playerTitle) playerTitle.innerText = 'Sin música';
  if (playerArtist) playerArtist.innerText = 'Inicia sesión para cargar tu música';
  if (playerArt) playerArt.src = 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&auto=format&fit=crop&q=80';

  // 4. Actualizar interfaz de usuario (oculta botones de logout)
  try {
    updateUserUI();
  } catch (e) {}

  // 5. Cerrar cualquier modal abierto
  modalAuth?.classList.add('hidden');
  modalSync?.classList.add('hidden');
  document.getElementById('modal-shortcuts')?.classList.add('hidden');

  // 6. OCULTAR TOTALMENTE EL REPRODUCTOR Y MOSTRAR LA PANTALLA PRINCIPAL DE INICIAR SESIÓN
  const appViewport = document.querySelector('.app-viewport') || document.getElementById('app-viewport');
  if (appViewport) {
    appViewport.classList.add('hidden');
  }
  const fullLogin = document.getElementById('full-login-screen');
  if (fullLogin) {
    fullLogin.classList.remove('hidden');
    const title = document.getElementById('full-login-status-title');
    const desc = document.getElementById('full-login-status-desc');
    if (title) title.innerText = 'Iniciar Sesión en DaVE Cloud';
    if (desc) desc.innerText = 'Has cerrado tu sesión. Inicia sesión con tu cuenta para acceder y escuchar todas las canciones que tienes guardadas en la nube.';
    
    const passInput = document.getElementById('full-login-password');
    if (passInput) passInput.value = '';

    setTimeout(() => document.getElementById('full-login-email')?.focus(), 250);
  }

  showToast('👋 Sesión cerrada correctamente', 'info', 'fa-right-from-bracket');
}

document.getElementById('btn-profile-logout')?.addEventListener('click', (e) => {
  e.preventDefault();
  logoutUser();
});

document.getElementById('btn-topbar-logout')?.addEventListener('click', (e) => {
  e.preventDefault();
  logoutUser();
});

document.getElementById('btn-sidebar-logout')?.addEventListener('click', (e) => {
  e.preventDefault();
  logoutUser();
});

document.getElementById('btn-profile-sync')?.addEventListener('click', () => {
  modalAuth?.classList.add('hidden');
  modalSync?.classList.remove('hidden');
});

// ==========================================
// REAL MOBILE TRACKS (INFINIX HOT 40i SYNC — 30 CANCIONES)
// ==========================================
const realPhoneTracks = [
  {
    title: 'Happy Nation',
    artist: 'Ace of Base',
    album: 'Happy Nation (Remastered)',
    duration: 255,
    streamUrl: 'music/Ace%20of%20Base%20-%20Happy%20Nation.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music3/v4/fb/fd/a8/fbfda872-a03c-4c01-c3f1-8e185c081f7b/cover.jpg/600x600bb.jpg'
  },
  {
    title: 'NADIE SABE',
    artist: 'Bad Bunny',
    album: 'Nadie Sabe Lo Que Va a Pasar Mañana',
    duration: 374,
    streamUrl: 'music/BAD%20BUNNY%20-%20%20NADIE%20SABE%20(Visualizer)%20_%20nadie%20sabe%20lo%20que%20va%20a%20pasar%20ma%C3%B1ana.mp3',
    coverUrl: 'https://upload.wikimedia.org/wikipedia/en/7/74/Bad_Bunny_-_Nadie_Sabe_Lo_Que_Va_a_Pasar_Ma%C3%B1ana.png'
  },
  {
    title: 'Dos Mil 16',
    artist: 'Bad Bunny',
    album: 'Un Verano Sin Ti',
    duration: 208,
    streamUrl: 'music/Bad%20Bunny%20-%20Dos%20Mil%2016%20(360%C2%B0%20Visualizer)%20_%20Un%20Verano%20Sin%20Ti.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/3e/04/eb/3e04ebf6-370f-f59d-ec84-2c2643db92f1/196626945068.jpg/600x600bb.jpg'
  },
  {
    title: 'Breakin\' Dishes',
    artist: 'Rihanna',
    album: 'Good Girl Gone Bad',
    duration: 200,
    streamUrl: 'music/Breakin_%20Dishes(M4A_128K).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/2b/c0/81/2bc081c8-25f0-ba43-d451-587a54613778/16UMGIM59202.rgb.jpg/600x600bb.jpg'
  },
  {
    title: 'QUÉ LÍO',
    artist: 'Blessd',
    album: 'CantoYo • QUÉ LÍO',
    duration: 135,
    streamUrl: 'music/Blessd%20%20-%20QU%C3%89%20L%C3%8DO%20(Lyric%20Video)%20_%20CantoYo.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/b4/8f/cc/b48fccb4-aaf0-913f-1dd6-bd4aa9c8ac2d/827568017680.jpg/600x600bb.jpg'
  },
  {
    title: 'Después De La Una',
    artist: 'Cris MJ, FloyyMenor, LOUKI',
    album: 'Después De La Una - Single',
    duration: 185,
    streamUrl: 'music/Cris%20MJ_%20FloyyMenor_%20LOUKI%20-%20Despu%C3%A9s%20De%20La%20Una%20(Vi(M4A_128K).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/47/64/f9/4764f901-1f97-26c4-61cf-fcf3dc016c09/430931.jpg/600x600bb.jpg'
  },
  {
    title: 'Guardian',
    artist: 'Curly & QORA',
    album: 'Guardian - Single',
    duration: 206,
    streamUrl: 'music/Curly%20%26%20QORA%20-%20Guardian.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music113/v4/73/5c/46/735c4661-ce10-71f4-1021-5a8efe0e9173/73589bec-0d08-4524-addf-e7d684335c1d.jpg/600x600bb.jpg'
  },
  {
    title: 'Let You Down (Ending Theme)',
    artist: 'Dawid Podsiadło',
    album: 'Cyberpunk: Edgerunners (Netflix)',
    duration: 238,
    streamUrl: 'music/Cyberpunk_%20Edgerunners%20%E2%80%94%20Ending%20Theme%20_%20Let%20You%20Down%20by%20Dawid%20Podsiad%C5%82o%20_%20Netflix.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/82/35/0e/82350ed4-f66f-600b-f572-c7507fc66a10/196589453082.jpg/600x600bb.jpg'
  },
  {
    title: 'Phantom Liberty',
    artist: 'Dawid Podsiadło, P.T. Adamczyk',
    album: 'Cyberpunk 2077: Phantom Liberty',
    duration: 279,
    streamUrl: 'music/Dawid%20Podsiad%C5%82o%2C%20P.T.%20Adamczyk%20%E2%80%94%20Phantom%20Liberty%20(Official%20Cyberpunk%202077%20Music%20Video).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/e1/6e/79/e16e7907-1a77-8e18-64df-6a5d28ecc17d/196871442299.jpg/600x600bb.jpg'
  },
  {
    title: 'Pose',
    artist: 'Daddy Yankee',
    album: 'Talento de Barrio',
    duration: 220,
    streamUrl: 'music/Daddy%20Yankee%20_%20Pose%20%5BLetra%5D(M4A_128K).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/f9/95/52/f99552d9-b212-a3a0-cea4-fe0c5dc26243/24CRGIM46809.rgb.jpg/600x600bb.jpg'
  },
  {
    title: 'L\'Amour Toujours (Tanzen Vision Rmx)',
    artist: 'Gigi D\'Agostino',
    album: 'Clásicos Electrónica',
    duration: 425,
    streamUrl: 'music/Topic%20-%20L%27Amour%20Toujours%20(Tanzen%20Vision%20Rmx).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/86/4c/46/864c4651-6277-6126-f58f-fec0ef34109f/090204669776_neu.jpg/600x600bb.jpg'
  },
  {
    title: 'Paparazzi (Dubstep Remix)',
    artist: 'Lady Gaga (Alximo / Kareto)',
    album: 'Remixes Electrónicos',
    duration: 185,
    streamUrl: 'music/Lady%20Gaga%20%3B%20Paparazzi%20Dubstep%20remix%20(Alximo)%20-%20(Sub.%20Espa%C3%B1ol).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/e0/80/34/e080341a-b442-72cd-1d0f-eb6863e8cb88/10UMGIM11335.rgb.jpg/600x600bb.jpg'
  },
  {
    title: 'Abracadabra',
    artist: 'Lady Gaga',
    album: 'Abracadabra / Disease',
    duration: 245,
    streamUrl: 'music/Lady%20Gaga%20-%20Abracadabra%20(Official%20Music%20Video).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/08/12/80/08128053-d7df-489d-bfde-be6f45f075be/26UMGIM57129.rgb.jpg/600x600bb.jpg'
  },
  {
    title: 'Bad Romance',
    artist: 'Lady Gaga',
    album: 'The Fame Monster',
    duration: 295,
    streamUrl: 'music/Lady%20Gaga%20-%20Bad%20Romance%20(Official%20Music%20Video).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/1f/25/c4/1f25c4bf-7f7a-ff26-8769-20ab6052dadf/09UMGIM40719.rgb.jpg/600x600bb.jpg'
  },
  {
    title: 'Bloody Mary',
    artist: 'Lady Gaga',
    album: 'Born This Way',
    duration: 244,
    streamUrl: 'music/Lady%20Gaga%20-%20Bloody%20Mary%20(Official%20Audio).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/42/9f/0f/429f0fd2-30bd-b64e-27fc-76d8fbbd0988/11UMGIM12476.rgb.jpg/600x600bb.jpg'
  },
  {
    title: 'Just Dance',
    artist: 'Lady Gaga ft. Colby O\'Donis',
    album: 'The Fame',
    duration: 241,
    streamUrl: 'music/Lady%20Gaga%20-%20Just%20Dance%20(Official%20Music%20Video)%20ft.%20Colby%20O%27Donis.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/a6/68/28/a66828c0-3fe3-5419-374d-ad98739f3166/08UMGIM13954.rgb.jpg/600x600bb.jpg'
  },
  {
    title: 'Paparazzi (Original)',
    artist: 'Lady Gaga',
    album: 'The Fame',
    duration: 238,
    streamUrl: 'music/Lady%20Gaga%20-%20Paparazzi%20(Official%20Music%20Video).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/1b/98/88/1b9888da-6a1f-bff0-ec03-518f445019f6/19UMGIM73435.rgb.jpg/600x600bb.jpg'
  },
  {
    title: 'Color Your Night',
    artist: 'Lotus Juice',
    album: 'Persona 3 Reload OST',
    duration: 228,
    streamUrl: 'music/Topic%20-%20Color%20Your%20Night.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/d7/0e/72/d70e724e-d8c0-8043-516a-ba47299b1554/PA00136839_1_185077_jacket.jpg/600x600bb.jpg'
  },
  {
    title: 'A Phantom Pain',
    artist: 'Ludvig Forssell',
    album: 'Metal Gear Solid V OST',
    duration: 239,
    streamUrl: 'music/Topic%20-%20A%20Phantom%20Pain.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music49/v4/c2/87/60/c2876016-b688-18cc-7649-c44049450c79/007725_4988602168907.jpg/600x600bb.jpg'
  },
  {
    title: 'Somos de Calle',
    artist: 'Daddy Yankee',
    album: 'Talento de Barrio',
    duration: 214,
    streamUrl: 'music/Somos%20de%20Calle.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/f9/95/52/f99552d9-b212-a3a0-cea4-fe0c5dc26243/24CRGIM46809.rgb.jpg/600x600bb.jpg'
  },
  {
    title: 'Duvet (Serial Experiments Lain)',
    artist: 'Bôa (sweetblue.)',
    album: 'Anime Classics',
    duration: 203,
    streamUrl: 'music/B%C3%B4a%20-%20Duvet%20(Sub.%20Espa%C3%B1ol%20%2B%20Lyrics).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/45/44/15/45441528-0288-eedc-f6fc-93137b8cfe96/067003248969.png/600x600bb.jpg'
  },
  {
    title: 'LA PLENA (W Sound 05)',
    artist: 'Beéle, Westcol, Ovy On The Drums',
    album: 'W Sound Series',
    duration: 151,
    streamUrl: 'music/W%20Sound%2005%20_LA%20PLENA_%20-%20Be%C3%A9le%2C%20Westcol%2C%20Ovy%20On%20The%20Drums.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/76/c1/83/76c18371-1a13-b500-12a5-da71f33a8d25/0.jpg/600x600bb.jpg'
  },
  {
    title: 'original sound (el_bonitillo_rb)',
    artist: 'Wuancho_dr430',
    album: 'TikTok Trending',
    duration: 13,
    streamUrl: 'music/original%20sound%20-%20el_bonitillo_rb.mp3',
    coverUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80'
  },
  {
    title: 'original sound (papo_yt1)',
    artist: 'Audios Más Virales',
    album: 'TikTok Trending',
    duration: 14,
    streamUrl: 'music/original%20sound%20-%20papo_yt1.mp3',
    coverUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80'
  },
  {
    title: 'SSRHD (Remix)',
    artist: 'Ziraki',
    album: 'Suno AI • WhatsApp Audio',
    duration: 242,
    streamUrl: 'music/Ziraki%20-%20SSRHD%20(Remix).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/25/b9/d0/25b9d0a5-323a-fd54-bce4-6c768c8b00e4/8721056924288.png/600x600bb.jpg'
  },
  {
    title: 'Verdades (TikTok)',
    artist: 'Westcol',
    album: 'Westcol Audios Virales',
    duration: 78,
    streamUrl: 'music/Westcol%20-%20Verdades%20(TikTok).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/76/c1/83/76c18371-1a13-b500-12a5-da71f33a8d25/0.jpg/600x600bb.jpg'
  },
  {
    title: 'HJ-S',
    artist: 'Ziraki',
    album: 'Suno AI Studio',
    duration: 151,
    streamUrl: 'music/Ziraki%20-%20HJ-S.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/25/b9/d0/25b9d0a5-323a-fd54-bce4-6c768c8b00e4/8721056924288.png/600x600bb.jpg'
  },
  {
    title: 'Dame Fuerza (Intro Doña Bárbara)',
    artist: 'Marta Sánchez',
    album: 'Doña Bárbara Soundtrack',
    duration: 188,
    streamUrl: 'music/Martha%20Sanchez%20-%20Dame%20Fuerza%20(Intro%20Dona%20Barbara).mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music/v4/7d/53/7d/7d537dfa-2cb0-b08e-5b12-58133ce745c4/00602517871379.rgb.jpg/600x600bb.jpg'
  },
  {
    title: 'Gritona (Efecto de Sonido)',
    artist: 'Jorge Murguía Quiroz',
    album: 'Efectos & Vines',
    duration: 6,
    streamUrl: 'music/Jorge%20Murguia%20Quiroz%20-%20Gritona%20(Efecto).mp3',
    coverUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80'
  },
  {
    title: 'Let You Down (Extended)',
    artist: 'Dawid Podsiadło',
    album: 'Cyberpunk 2077 OST',
    duration: 280,
    streamUrl: 'music/Let%20You%20Down.mp3',
    coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/82/35/0e/82350ed4-f66f-600b-f572-c7507fc66a10/196589453082.jpg/600x600bb.jpg'
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
    showToast('No se encontraron archivos de audio compatibles (MP3, WAV, FLAC, M4A)', 'warning', 'fa-triangle-exclamation');
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
    showToast('Introduce la IP que muestra tu celular (ej. 192.168.0.161:8080)', 'warning', 'fa-triangle-exclamation');
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
    showSyncFeedback('Por favor introduce la IP que muestra tu celular (ej. 192.168.0.161:8080)', 'error');
    return;
  }
  if (!ip.startsWith('http://') && !ip.startsWith('https://')) {
    ip = `http://${ip}`;
  }
  ip = ip.replace(/\/+$/, '');

  // If on HTTPS (GitHub Pages), the browser prevents background fetch to private LAN IPs.
  // Seamlessly open the phone console in a new tab so the user sees all their songs immediately!
  if (window.location.protocol === 'https:') {
    window.open(ip, '_blank');
    startPhoneHeartbeat(ip);
    showSyncFeedback(`¡Abriendo tu celular (${ip}) en una nueva pestaña! En esa pestaña tienes todas tus canciones listas.`, 'success');
    setTimeout(() => modalSync?.classList.add('hidden'), 2000);
    return;
  }

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
        startPhoneHeartbeat(ip);
        renderPhoneTracksList();
        updateUserUI();
        showSyncFeedback(`¡Conectado! Se cargaron ${phoneTracks.length} canciones de tu celular.`, 'success');
        setTimeout(() => modalSync?.classList.add('hidden'), 1200);
        return;
      }
    }
  } catch (err) {
    console.log('Direct WiFi error:', err);
  }

  syncCloudPhoneLibrary();
  showSyncFeedback(`¡Sincronización Cloud completada! Se vincularon las canciones de tu cuenta.`, 'success');
  setTimeout(() => modalSync?.classList.add('hidden'), 1200);
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

const phoneSearchEmptyState = document.getElementById('phone-search-empty-state');
const phoneSearchEmptyText = document.getElementById('phone-search-empty-text');

function renderPhoneTracksList(filtered = null, searchQuery = '') {
  const list = filtered !== null ? filtered : phoneTracks;
  if (phoneSongsCount) phoneSongsCount.innerText = list.length;
  if (pstatSongs) pstatSongs.innerText = phoneTracks.length;

  if (list.length === 0) {
    phoneTrackTable?.classList.add('hidden');
    if (searchQuery) {
      phoneEmptyState?.classList.add('hidden');
      if (phoneSearchEmptyState) {
        phoneSearchEmptyState.classList.remove('hidden');
        if (phoneSearchEmptyText) {
          phoneSearchEmptyText.innerHTML = `No se encontraron canciones en tu celular para "<strong>${escapeHtml(searchQuery)}</strong>".`;
        }
      }
    } else {
      phoneSearchEmptyState?.classList.add('hidden');
      phoneEmptyState?.classList.remove('hidden');
    }
    return;
  }

  phoneSearchEmptyState?.classList.add('hidden');
  phoneEmptyState?.classList.add('hidden');
  phoneTrackTable?.classList.remove('hidden');
  if (!phoneTrackList) return;
  phoneTrackList.innerHTML = '';

  list.forEach((track, i) => {
    const tr = createTrackRow(track, i, true);
    phoneTrackList.appendChild(tr);
  });
}

// ==========================================
// 14. KEYBOARD SHORTCUTS & HELP MODAL
// ==========================================
const modalShortcuts = document.getElementById('modal-shortcuts');
const btnShortcutsModal = document.getElementById('btn-shortcuts-modal');
const btnCloseShortcuts = document.getElementById('btn-close-shortcuts');

btnShortcutsModal?.addEventListener('click', () => {
  modalShortcuts?.classList.remove('hidden');
});
btnCloseShortcuts?.addEventListener('click', () => {
  modalShortcuts?.classList.add('hidden');
});
modalShortcuts?.addEventListener('click', (e) => {
  if (e.target === modalShortcuts) modalShortcuts.classList.add('hidden');
});

window.addEventListener('keydown', (e) => {
  if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

  if (e.key === '?' || (e.shiftKey && e.code === 'Slash')) {
    e.preventDefault();
    modalShortcuts?.classList.toggle('hidden');
  } else if (e.code === 'Space') {
    e.preventDefault();
    playBtn.click();
  } else if (e.code === 'ArrowRight') {
    if (e.shiftKey) nextBtn.click();
    else if (audio.duration) {
      audio.currentTime = Math.min(audio.duration, audio.currentTime + 5);
      showToast(`+5s (${formatTime(audio.currentTime)})`, 'info', 'fa-forward');
    }
  } else if (e.code === 'ArrowLeft') {
    if (e.shiftKey) prevBtn.click();
    else {
      audio.currentTime = Math.max(0, audio.currentTime - 5);
      showToast(`-5s (${formatTime(audio.currentTime)})`, 'info', 'fa-backward');
    }
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
  } else if (e.code === 'KeyL') {
    switchTab('lyrics');
    showToast('Modo Karaoke & Letras', 'info', 'fa-microphone-lines');
  } else if (e.code === 'KeyS') {
    shuffleBtn?.click();
    showToast(isShuffle ? 'Modo Aleatorio activado' : 'Modo Aleatorio desactivado', 'info', 'fa-shuffle');
  } else if (e.code === 'KeyR') {
    repeatBtn?.click();
    const modes = ['Repetir desactivado', 'Repetir lista completa', 'Repetir canción actual'];
    showToast(modes[repeatMode] || 'Repetir', 'info', 'fa-repeat');
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
    playerArt.onerror = () => {
      playerArt.src = 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&auto=format&fit=crop&q=80';
    };
    applyDynamicArtworkPalette(track.coverUrl);
  } else {
    applyDynamicArtworkPalette(null);
  }
  setupMediaSession(track);
  const lyrTitle = document.getElementById('lyrics-song-title');
  const lyrArtist = document.getElementById('lyrics-song-artist');
  if (lyrTitle) lyrTitle.innerText = track.title;
  if (lyrArtist) lyrArtist.innerText = track.artist;
}

// ==========================================
// 15. DYNAMIC ARTWORK PALETTE & AMBIENT GLOW
// ==========================================
function extractCoverPalette(imgSrc, callback) {
  if (!imgSrc || imgSrc.startsWith('data:image/svg')) {
    return callback(['rgba(99, 102, 241, 0.35)', 'rgba(6, 182, 212, 0.22)']);
  }
  const img = new Image();
  img.crossOrigin = 'Anonymous';
  img.onload = () => {
    try {
      const c = document.createElement('canvas');
      const ctx = c.getContext('2d');
      c.width = 16;
      c.height = 16;
      ctx.drawImage(img, 0, 0, 16, 16);
      const data = ctx.getImageData(0, 0, 16, 16).data;
      let r = 0, g = 0, b = 0, count = 0;
      for (let i = 0; i < data.length; i += 4) {
        const pr = data[i], pg = data[i + 1], pb = data[i + 2];
        const brightness = (pr * 299 + pg * 587 + pb * 114) / 1000;
        if (brightness > 35 && brightness < 225) {
          r += pr; g += pg; b += pb;
          count++;
        }
      }
      if (count > 0) {
        r = Math.round(r / count);
        g = Math.round(g / count);
        b = Math.round(b / count);
        const col1 = `rgba(${r}, ${g}, ${b}, 0.38)`;
        const col2 = `rgba(${Math.min(255, Math.round(r * 0.6 + 40))}, ${Math.min(255, Math.round(g * 0.8 + 30))}, ${Math.min(255, Math.round(b * 1.2 + 50))}, 0.24)`;
        callback([col1, col2]);
      } else {
        callback(['rgba(99, 102, 241, 0.35)', 'rgba(6, 182, 212, 0.22)']);
      }
    } catch (e) {
      callback(['rgba(99, 102, 241, 0.35)', 'rgba(6, 182, 212, 0.22)']);
    }
  };
  img.onerror = () => callback(['rgba(99, 102, 241, 0.35)', 'rgba(6, 182, 212, 0.22)']);
  img.src = imgSrc;
}

function applyDynamicArtworkPalette(coverUrl) {
  extractCoverPalette(coverUrl, ([col1, col2]) => {
    if (ambientGlow) {
      ambientGlow.style.background = `radial-gradient(circle at 50% 30%, ${col1}, ${col2}, transparent 72%)`;
    }
    if (artGlow) {
      artGlow.style.boxShadow = `0 0 35px ${col1}`;
    }
    const bottomPlayer = document.querySelector('.bottom-player');
    if (bottomPlayer) {
      bottomPlayer.style.borderTopColor = col1;
    }
  });
}

// ==========================================
// 16. NATIVE MEDIASESSION API (WINDOWS & MOBILE)
// ==========================================
function setupMediaSession(track) {
  if (!('mediaSession' in navigator) || !track) return;
  try {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: track.title,
      artist: track.artist,
      album: track.album || 'DaVE Player',
      artwork: track.coverUrl ? [
        { src: track.coverUrl, sizes: '96x96', type: 'image/jpeg' },
        { src: track.coverUrl, sizes: '192x192', type: 'image/jpeg' },
        { src: track.coverUrl, sizes: '512x512', type: 'image/jpeg' }
      ] : []
    });

    navigator.mediaSession.setActionHandler('play', () => {
      audio.play().then(() => {
        isPlaying = true;
        updatePlayPauseUI();
      });
    });
    navigator.mediaSession.setActionHandler('pause', () => {
      audio.pause();
      isPlaying = false;
      updatePlayPauseUI();
    });
    navigator.mediaSession.setActionHandler('previoustrack', () => {
      prevBtn?.click();
    });
    navigator.mediaSession.setActionHandler('nexttrack', () => {
      nextBtn?.click();
    });
    try {
      navigator.mediaSession.setActionHandler('seekto', (details) => {
        if (details.seekTime !== undefined && audio.duration) {
          audio.currentTime = details.seekTime;
          const ratio = audio.currentTime / audio.duration;
          progressFill.style.width = `${ratio * 100}%`;
          currentTimeEl.innerText = formatTime(audio.currentTime);
        }
      });
      navigator.mediaSession.setActionHandler('seekforward', (details) => {
        audio.currentTime = Math.min(audio.duration || 0, audio.currentTime + (details.seekOffset || 10));
      });
      navigator.mediaSession.setActionHandler('seekbackward', (details) => {
        audio.currentTime = Math.max(0, audio.currentTime - (details.seekOffset || 10));
      });
    } catch (e) {}
  } catch (err) {
    console.log('MediaSession error:', err);
  }
}

// ==========================================
// 17. SMART CONTEXT MENU (CLIC DERECHO)
// ==========================================
let ctxSelectedTrack = null;
const ctxMenu = document.getElementById('custom-context-menu');
const ctxCover = document.getElementById('ctx-cover');
const ctxTitle = document.getElementById('ctx-title');
const ctxArtist = document.getElementById('ctx-artist');
const ctxFavLabel = document.getElementById('ctx-fav-label');
const ctxDownloadBtn = document.getElementById('ctx-download-btn');

function openContextMenu(e, track) {
  e.preventDefault();
  ctxSelectedTrack = track;
  if (!ctxMenu) return;

  if (ctxCover) ctxCover.src = track.coverUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 36 36"><rect width="36" height="36" fill="%23191c28"/></svg>';
  if (ctxTitle) ctxTitle.innerText = track.title;
  if (ctxArtist) ctxArtist.innerText = track.artist;

  const isFav = favorites.has(track.title + track.artist);
  if (ctxFavLabel) ctxFavLabel.innerText = isFav ? 'Quitar de Favoritas' : 'Añadir a Favoritas';

  if (ctxDownloadBtn) {
    if (track.streamUrl) {
      ctxDownloadBtn.style.display = 'flex';
      ctxDownloadBtn.href = track.streamUrl;
      ctxDownloadBtn.setAttribute('download', `${track.title} - ${track.artist}.mp3`);
    } else {
      ctxDownloadBtn.style.display = 'none';
    }
  }

  ctxMenu.classList.remove('hidden');
  const menuWidth = 250;
  const menuHeight = 280;
  let posX = e.clientX;
  let posY = e.clientY;

  if (posX + menuWidth > window.innerWidth) posX = window.innerWidth - menuWidth - 12;
  if (posY + menuHeight > window.innerHeight) posY = window.innerHeight - menuHeight - 12;

  ctxMenu.style.left = `${Math.max(10, posX)}px`;
  ctxMenu.style.top = `${Math.max(10, posY)}px`;
}

function closeContextMenu() {
  ctxMenu?.classList.add('hidden');
  ctxSelectedTrack = null;
}

window.addEventListener('click', (e) => {
  if (!e.target.closest('#custom-context-menu')) closeContextMenu();
});
window.addEventListener('scroll', () => closeContextMenu(), true);
window.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') closeContextMenu();
});

document.getElementById('ctx-btn-play')?.addEventListener('click', () => {
  if (ctxSelectedTrack) {
    if (!playlist.includes(ctxSelectedTrack)) playlist.push(ctxSelectedTrack);
    playTrack(playlist.indexOf(ctxSelectedTrack));
  }
  closeContextMenu();
});

document.getElementById('ctx-btn-next')?.addEventListener('click', () => {
  if (ctxSelectedTrack) {
    const existingIdx = playlist.indexOf(ctxSelectedTrack);
    if (existingIdx !== -1) playlist.splice(existingIdx, 1);
    const insertIdx = (currentIndex >= 0 && currentIndex < playlist.length) ? currentIndex + 1 : playlist.length;
    playlist.splice(insertIdx, 0, ctxSelectedTrack);
    renderTrackList();
    renderQueueDrawer();
    showToast(`"${ctxSelectedTrack.title}" sonará a continuación`, 'success', 'fa-forward-step');
  }
  closeContextMenu();
});

document.getElementById('ctx-btn-lyrics')?.addEventListener('click', () => {
  if (ctxSelectedTrack) {
    if (!playlist.includes(ctxSelectedTrack)) playlist.push(ctxSelectedTrack);
    playTrack(playlist.indexOf(ctxSelectedTrack));
  }
  switchTab('lyrics');
  closeContextMenu();
});

document.getElementById('ctx-btn-stems')?.addEventListener('click', () => {
  if (ctxSelectedTrack) {
    if (!playlist.includes(ctxSelectedTrack)) playlist.push(ctxSelectedTrack);
    playTrack(playlist.indexOf(ctxSelectedTrack));
  }
  switchTab('stem-mixer');
  closeContextMenu();
});

document.getElementById('ctx-btn-fav')?.addEventListener('click', () => {
  if (ctxSelectedTrack) toggleFavorite(ctxSelectedTrack);
  closeContextMenu();
});

// ==========================================
// 18. QUEUE DRAWER ("A CONTINUACIÓN")
// ==========================================
const queueDrawer = document.getElementById('queue-drawer');
const btnToggleQueue = document.getElementById('btn-toggle-queue');
const btnCloseQueue = document.getElementById('btn-close-queue');
const queueUpcomingList = document.getElementById('queue-upcoming-list');
const queueCountEl = document.getElementById('queue-count');
const queueNowTitle = document.getElementById('queue-now-title');
const queueNowArtist = document.getElementById('queue-now-artist');
const queueNowArt = document.getElementById('queue-now-art');

function toggleQueueDrawer() {
  if (!queueDrawer) return;
  const isOpen = queueDrawer.classList.toggle('open');
  if (btnToggleQueue) btnToggleQueue.classList.toggle('active', isOpen);
  if (isOpen) renderQueueDrawer();
}

function renderQueueDrawer() {
  if (!queueDrawer) return;
  const currentTrack = (currentIndex >= 0 && currentIndex < playlist.length) ? playlist[currentIndex] : null;

  if (currentTrack) {
    if (queueNowTitle) queueNowTitle.innerText = currentTrack.title;
    if (queueNowArtist) queueNowArtist.innerText = currentTrack.artist;
    if (queueNowArt) queueNowArt.src = currentTrack.coverUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="46" height="46" viewBox="0 0 46 46"><rect width="46" height="46" fill="%23191c28"/></svg>';
  } else {
    if (queueNowTitle) queueNowTitle.innerText = 'Sin música';
    if (queueNowArtist) queueNowArtist.innerText = 'Inicia sesión para reproducir';
    if (queueNowArt) queueNowArt.src = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="46" height="46" viewBox="0 0 46 46"><rect width="46" height="46" fill="%23191c28"/></svg>';
  }

  if (!queueUpcomingList) return;
  queueUpcomingList.innerHTML = '';

  const upcoming = [];
  for (let i = currentIndex + 1; i < playlist.length; i++) {
    upcoming.push({ track: playlist[i], index: i });
  }

  if (queueCountEl) queueCountEl.innerText = upcoming.length;

  if (upcoming.length === 0) {
    queueUpcomingList.innerHTML = `
      <div style="text-align:center; padding: 2rem 1rem; color: var(--text-dim); font-size: 0.85rem;">
        <i class="fa-solid fa-compact-disc" style="font-size: 1.8rem; margin-bottom: 0.5rem; opacity: 0.4; display:block;"></i>
        Fin de la lista de reproducción
      </div>
    `;
    return;
  }

  upcoming.forEach(({ track, index }) => {
    const item = document.createElement('div');
    item.className = 'queue-item';
    const cover = track.coverUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 36 36"><rect width="36" height="36" fill="%23191c28"/></svg>';
    item.innerHTML = `
      <img src="${cover}" alt="Art" loading="lazy">
      <div class="queue-item-meta">
        <strong>${track.title}</strong>
        <span>${track.artist}</span>
      </div>
      <i class="fa-solid fa-play" style="font-size: 0.75rem; color: var(--accent-cyan); opacity: 0.7;"></i>
    `;
    item.addEventListener('click', () => {
      playTrack(index);
      renderQueueDrawer();
    });
    queueUpcomingList.appendChild(item);
  });
}

btnToggleQueue?.addEventListener('click', toggleQueueDrawer);
btnCloseQueue?.addEventListener('click', () => {
  queueDrawer?.classList.remove('open');
  btnToggleQueue?.classList.remove('active');
});

// ==========================================
// 19. PWA INSTALLATION & SERVICE WORKER
// ==========================================
let deferredInstallPrompt = null;
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredInstallPrompt = e;
  const btnInstall = document.getElementById('btn-install-pwa');
  if (btnInstall) btnInstall.classList.remove('hidden');
});

document.getElementById('btn-install-pwa')?.addEventListener('click', async () => {
  if (deferredInstallPrompt) {
    deferredInstallPrompt.prompt();
    const { outcome } = await deferredInstallPrompt.userChoice;
    if (outcome === 'accepted') {
      document.getElementById('btn-install-pwa')?.classList.add('hidden');
      showToast('¡DaVE Player instalado con éxito!', 'success', 'fa-circle-check');
    }
    deferredInstallPrompt = null;
  }
});

if ('serviceWorker' in navigator && window.location.protocol.startsWith('http')) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js').catch(err => console.log('SW reg error:', err));
  });
}

// ==========================================
// 21. SMART VOLUME NORMALIZER (COMPRESSOR DSP)
// ==========================================
function toggleVolumeNormalizer() {
  initAudioEngine();
  isNormalizerActive = !isNormalizerActive;
  localStorage.setItem('dave_normalizer', isNormalizerActive.toString());

  const btnNormalizer = document.getElementById('btn-toggle-normalizer');
  if (btnNormalizer) btnNormalizer.classList.toggle('active', isNormalizerActive);

  if (normalizerCompressor && audioCtx) {
    try {
      if (isNormalizerActive) {
        normalizerCompressor.threshold.setValueAtTime(-24, audioCtx.currentTime);
        normalizerCompressor.knee.setValueAtTime(30, audioCtx.currentTime);
        normalizerCompressor.ratio.setValueAtTime(12, audioCtx.currentTime);
        normalizerCompressor.attack.setValueAtTime(0.003, audioCtx.currentTime);
        normalizerCompressor.release.setValueAtTime(0.25, audioCtx.currentTime);
        showToast('Normalizador activo: Volumen nivelado automáticamente', 'success', 'fa-compress');
      } else {
        normalizerCompressor.threshold.setValueAtTime(0, audioCtx.currentTime);
        normalizerCompressor.ratio.setValueAtTime(1, audioCtx.currentTime);
        showToast('Normalizador desactivado: Audio dinámico directo', 'info', 'fa-compress');
      }
    } catch (e) {}
  } else {
    showToast(isNormalizerActive ? 'Normalizador activado' : 'Normalizador desactivado', 'info', 'fa-compress');
  }
}
document.getElementById('btn-toggle-normalizer')?.addEventListener('click', toggleVolumeNormalizer);

// ==========================================
// 22. SPATIAL REVERB DSP (ACÚSTICA ESPACIAL)
// ==========================================
const reverbPresets = {
  off: { dry: 1.0, wet: 0.0, label: 'Plano / Seco' },
  studio: { dry: 0.95, wet: 0.22, duration: 0.8, decay: 3.0, label: 'Estudio Íntimo' },
  club: { dry: 0.85, wet: 0.38, duration: 1.8, decay: 2.2, label: 'Club Nocturno' },
  stadium: { dry: 0.72, wet: 0.52, duration: 3.2, decay: 1.6, label: 'Estadio en Vivo' },
  cathedral: { dry: 0.62, wet: 0.68, duration: 4.5, decay: 1.1, label: 'Catedral' }
};

function createReverbBuffer(ctx, duration = 2.0, decay = 2.0, reverse = false) {
  const sampleRate = ctx.sampleRate;
  const length = Math.floor(sampleRate * duration);
  const impulse = ctx.createBuffer(2, length, sampleRate);
  const left = impulse.getChannelData(0);
  const right = impulse.getChannelData(1);
  for (let i = 0; i < length; i++) {
    const n = reverse ? length - i : i;
    left[i] = (Math.random() * 2 - 1) * Math.pow(1 - n / length, decay);
    right[i] = (Math.random() * 2 - 1) * Math.pow(1 - n / length, decay);
  }
  return impulse;
}

function setSpatialReverb(presetKey) {
  initAudioEngine();
  const preset = reverbPresets[presetKey] || reverbPresets.off;
  currentReverbPreset = presetKey;

  document.querySelectorAll('.spatial-card').forEach(card => {
    card.classList.toggle('active', card.dataset.reverb === presetKey);
  });

  if (reverbDryGain && reverbWetGain && convolverNode && audioCtx) {
    try {
      reverbDryGain.gain.setValueAtTime(preset.dry, audioCtx.currentTime);
      reverbWetGain.gain.setValueAtTime(preset.wet, audioCtx.currentTime);
      if (preset.wet > 0 && preset.duration) {
        convolverNode.buffer = createReverbBuffer(audioCtx, preset.duration, preset.decay);
      }
    } catch (e) {}
  }
  showToast(`Acústica espacial: ${preset.label}`, 'info', 'fa-earth-americas');
}

document.querySelectorAll('.spatial-card').forEach(card => {
  card.addEventListener('click', () => {
    setSpatialReverb(card.dataset.reverb);
  });
});

// ==========================================
// 23. SLEEP TIMER (TEMPORIZADOR DE APAGADO)
// ==========================================
const modalSleepTimer = document.getElementById('modal-sleep-timer');
const btnSleepTimer = document.getElementById('btn-sleep-timer');
const btnCloseSleepTimer = document.getElementById('btn-close-sleep-timer');
const sleepTimerBadge = document.getElementById('sleep-timer-badge');
const sleepStatusText = document.getElementById('sleep-status-text');
const sleepCustomSlider = document.getElementById('sleep-custom-slider');
const sleepCustomLabel = document.getElementById('sleep-custom-label');

function openSleepTimerModal() {
  if (modalSleepTimer) modalSleepTimer.classList.remove('hidden');
}

function closeSleepTimerModal() {
  if (modalSleepTimer) modalSleepTimer.classList.add('hidden');
}

btnSleepTimer?.addEventListener('click', openSleepTimerModal);
btnCloseSleepTimer?.addEventListener('click', closeSleepTimerModal);

function cancelSleepTimer() {
  if (sleepTimerInterval) {
    clearInterval(sleepTimerInterval);
    sleepTimerInterval = null;
  }
  sleepTimerRemaining = 0;
  sleepTimerMode = null;
  if (sleepTimerBadge) {
    sleepTimerBadge.classList.add('hidden');
    sleepTimerBadge.innerText = '0m';
  }
  btnSleepTimer?.classList.remove('active');
  if (sleepStatusText) sleepStatusText.innerText = 'Temporizador inactivo';
  document.querySelectorAll('.timer-preset-btn').forEach(b => b.classList.remove('active'));
}

function startSleepTimerMinutes(minutes) {
  cancelSleepTimer();
  sleepTimerMode = 'minutes';
  sleepTimerRemaining = minutes * 60;

  btnSleepTimer?.classList.add('active');
  if (sleepTimerBadge) {
    sleepTimerBadge.classList.remove('hidden');
    sleepTimerBadge.innerText = `${minutes}m`;
  }
  updateSleepStatusDisplay();

  sleepTimerInterval = setInterval(() => {
    sleepTimerRemaining--;
    updateSleepStatusDisplay();

    // Gentle fade out in final 20 seconds
    if (sleepTimerRemaining <= 20 && sleepTimerRemaining > 0) {
      if (masterGain && audioCtx) {
        try {
          const ratio = Math.max(0, sleepTimerRemaining / 20);
          masterGain.gain.setValueAtTime(currentVolume * ratio, audioCtx.currentTime);
        } catch (e) {}
      }
    }

    if (sleepTimerRemaining <= 0) {
      clearInterval(sleepTimerInterval);
      sleepTimerInterval = null;
      audio.pause();
      isPlaying = false;
      updatePlayPauseUI();
      if (masterGain) masterGain.gain.value = currentVolume;
      cancelSleepTimer();
      showToast('🌙 Temporizador finalizado. ¡Que descanses!', 'success', 'fa-moon');
    }
  }, 1000);

  closeSleepTimerModal();
  showToast(`Temporizador activo: Apagado en ${minutes} min`, 'info', 'fa-moon');
}

function updateSleepStatusDisplay() {
  if (sleepTimerRemaining <= 0) return;
  const m = Math.floor(sleepTimerRemaining / 60);
  const s = sleepTimerRemaining % 60;
  const str = `${m}:${s < 10 ? '0' : ''}${s}`;
  if (sleepStatusText) sleepStatusText.innerText = `Apagando en ${str}`;
  if (sleepTimerBadge) sleepTimerBadge.innerText = m >= 1 ? `${m}m` : `${s}s`;
}

document.querySelectorAll('.timer-preset-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    const mins = btn.dataset.minutes;
    if (mins === 'end-track') {
      cancelSleepTimer();
      sleepTimerMode = 'end-track';
      btnSleepTimer?.classList.add('active');
      if (sleepTimerBadge) {
        sleepTimerBadge.classList.remove('hidden');
        sleepTimerBadge.innerText = 'Fin';
      }
      if (sleepStatusText) sleepStatusText.innerText = 'Apagará al terminar la canción actual';
      btn.classList.add('active');
      closeSleepTimerModal();
      showToast('La música se detendrá al terminar esta canción', 'info', 'fa-moon');
    } else if (mins) {
      startSleepTimerMinutes(parseInt(mins, 10));
    }
  });
});

document.getElementById('btn-cancel-sleep-timer')?.addEventListener('click', () => {
  cancelSleepTimer();
  showToast('Temporizador desactivado', 'info', 'fa-circle-xmark');
});

sleepCustomSlider?.addEventListener('input', (e) => {
  if (sleepCustomLabel) sleepCustomLabel.innerText = `${e.target.value} min`;
});

document.getElementById('btn-apply-custom-sleep')?.addEventListener('click', () => {
  const val = parseInt(sleepCustomSlider?.value || '20', 10);
  startSleepTimerMinutes(val);
});

// ==========================================
// 24. CUSTOM PLAYLISTS & FAVORITES MANAGEMENT
// ==========================================
const modalAddToPlaylist = document.getElementById('modal-add-to-playlist');
const modalCreatePlaylist = document.getElementById('modal-create-playlist');
const plPickerList = document.getElementById('pl-picker-list');
const btnCloseAddPl = document.getElementById('btn-close-add-pl');
const btnCloseCreatePl = document.getElementById('btn-close-create-pl');
const btnCreatePlTrigger = document.getElementById('btn-create-playlist-trigger');
const btnNewPlFromPicker = document.getElementById('btn-new-pl-from-picker');
const formCreatePlaylist = document.getElementById('form-create-playlist');

function loadUserPlaylists() {
  const saved = localStorage.getItem('dave_user_playlists');
  if (saved) {
    try {
      userPlaylists = JSON.parse(saved);
    } catch (e) {
      userPlaylists = [];
    }
  } else {
    userPlaylists = [
      { id: 'pl_training', name: '🔥 Para Entrenar', description: 'Máxima energía para darlo todo', trackKeys: [] },
      { id: 'pl_chill', name: '🌙 Chill & Relax', description: 'Sonidos envolventes y relajantes', trackKeys: [] }
    ];
    saveUserPlaylists();
  }
  updatePlaylistsBadge();
}

function saveUserPlaylists() {
  localStorage.setItem('dave_user_playlists', JSON.stringify(userPlaylists));
  updatePlaylistsBadge();
}

function updatePlaylistsBadge() {
  const plCountEl = document.getElementById('playlists-count');
  if (plCountEl) plCountEl.innerText = (userPlaylists.length + 1);
}

function renderPlaylistsTab() {
  const overview = document.getElementById('playlists-overview-view');
  const detail = document.getElementById('playlist-detail-view');
  if (!overview) return;

  overview.classList.remove('hidden');
  if (detail) detail.classList.add('hidden');

  const grid = document.getElementById('playlist-cards-grid');
  if (!grid) return;
  grid.innerHTML = '';

  // 1. Favorites Card (Always first)
  const favCard = document.createElement('div');
  favCard.className = 'playlist-card';
  favCard.innerHTML = `
    <div class="playlist-art-wrap" style="background: linear-gradient(135deg, #ec4899, #f43f5e);">
      <i class="fa-solid fa-heart" style="color: #fff;"></i>
    </div>
    <div class="playlist-card-title">❤️ Favoritas</div>
    <div class="playlist-card-meta">${favorites.size} canciones</div>
  `;
  favCard.addEventListener('click', () => openPlaylistDetail('favorites'));
  grid.appendChild(favCard);

  // 2. User Playlists Cards
  userPlaylists.forEach(pl => {
    const card = document.createElement('div');
    card.className = 'playlist-card';
    card.innerHTML = `
      <div class="playlist-art-wrap">
        <i class="fa-solid fa-compact-disc"></i>
      </div>
      <div class="playlist-card-title">${pl.name}</div>
      <div class="playlist-card-meta">${pl.trackKeys ? pl.trackKeys.length : 0} canciones</div>
    `;
    card.addEventListener('click', () => openPlaylistDetail(pl.id));
    grid.appendChild(card);
  });
}

function openPlaylistDetail(playlistId) {
  activePlaylistId = playlistId;
  const overview = document.getElementById('playlists-overview-view');
  const detail = document.getElementById('playlist-detail-view');
  const titleEl = document.getElementById('pl-detail-title');
  const descEl = document.getElementById('pl-detail-desc');
  const countEl = document.getElementById('pl-detail-count');
  const artEl = document.getElementById('pl-detail-art');
  const tracksBody = document.getElementById('pl-tracks-body');
  const btnDelete = document.getElementById('btn-delete-current-playlist');

  if (!overview || !detail) return;
  overview.classList.add('hidden');
  detail.classList.remove('hidden');

  let tracksToDisplay = [];

  if (playlistId === 'favorites') {
    if (titleEl) titleEl.innerText = '❤️ Canciones Favoritas';
    if (descEl) descEl.innerText = 'Todas las canciones a las que les diste Me Gusta.';
    if (artEl) {
      artEl.style.background = 'linear-gradient(135deg, #ec4899, #f43f5e)';
      artEl.innerHTML = '<i class="fa-solid fa-heart"></i>';
    }
    if (btnDelete) btnDelete.style.display = 'none';
    tracksToDisplay = realPhoneTracks.filter(t => favorites.has(t.title + t.artist));
  } else {
    const pl = userPlaylists.find(p => p.id === playlistId);
    if (!pl) return;
    if (titleEl) titleEl.innerText = pl.name;
    if (descEl) descEl.innerText = pl.description || 'Playlist personalizada de DaVE Player';
    if (artEl) {
      artEl.style.background = 'linear-gradient(135deg, #4f46e5, #06b6d4)';
      artEl.innerHTML = '<i class="fa-solid fa-compact-disc"></i>';
    }
    if (btnDelete) btnDelete.style.display = 'inline-flex';
    const keys = new Set(pl.trackKeys || []);
    tracksToDisplay = realPhoneTracks.filter(t => keys.has(t.title + t.artist));
  }

  if (countEl) countEl.innerText = tracksToDisplay.length;

  if (!tracksBody) return;
  tracksBody.innerHTML = '';

  if (tracksToDisplay.length === 0) {
    tracksBody.innerHTML = `
      <tr>
        <td colspan="6" style="text-align:center; padding: 2.5rem; color: var(--text-muted);">
          <i class="fa-solid fa-music" style="font-size:2rem; opacity:0.3; display:block; margin-bottom:0.6rem;"></i>
          Esta lista aún no tiene canciones. Haz clic derecho en cualquier canción y selecciona "Añadir a Playlist".
        </td>
      </tr>
    `;
    return;
  }

  tracksToDisplay.forEach((track, i) => {
    const tr = document.createElement('tr');
    tr.className = 'track-row';
    const cover = track.coverUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="44" height="44" viewBox="0 0 44 44"><rect width="44" height="44" fill="%23191c28"/></svg>';
    tr.innerHTML = `
      <td style="text-align:center; color: var(--text-dim);">${i + 1}</td>
      <td>
        <div style="display:flex; align-items:center; gap:0.75rem;">
          <img src="${cover}" class="track-cover-mini" alt="Art" loading="lazy">
          <strong style="color:#fff; font-size:0.86rem;">${track.title}</strong>
        </div>
      </td>
      <td style="color:var(--text-muted); font-size:0.82rem;">${track.artist}</td>
      <td style="color:var(--text-dim); font-size:0.8rem;">${track.album || 'DaVE Cloud'}</td>
      <td style="text-align:right; color:var(--text-dim); font-size:0.8rem;">${formatTime(track.duration || 180)}</td>
      <td style="text-align:right;">
        <button class="btn-icon-round remove-pl-track" title="Quitar de esta lista" style="width:30px; height:30px; font-size:0.75rem; color:#ef4444;"><i class="fa-solid fa-trash-can"></i></button>
      </td>
    `;

    tr.addEventListener('click', (e) => {
      if (e.target.closest('.remove-pl-track')) return;
      if (!playlist.includes(track)) playlist.push(track);
      playTrack(playlist.indexOf(track));
    });

    tr.querySelector('.remove-pl-track')?.addEventListener('click', (e) => {
      e.stopPropagation();
      if (playlistId === 'favorites') {
        toggleFavorite(track);
        openPlaylistDetail('favorites');
      } else {
        const pl = userPlaylists.find(p => p.id === playlistId);
        if (pl && pl.trackKeys) {
          pl.trackKeys = pl.trackKeys.filter(k => k !== (track.title + track.artist));
          saveUserPlaylists();
          openPlaylistDetail(playlistId);
          showToast(`Quitada de ${pl.name}`, 'info', 'fa-trash-can');
        }
      }
    });

    tracksBody.appendChild(tr);
  });
}

document.getElementById('btn-back-to-playlists')?.addEventListener('click', renderPlaylistsTab);

document.getElementById('btn-play-all-playlist')?.addEventListener('click', () => {
  if (!activePlaylistId) return;
  let tracks = [];
  if (activePlaylistId === 'favorites') {
    tracks = realPhoneTracks.filter(t => favorites.has(t.title + t.artist));
  } else {
    const pl = userPlaylists.find(p => p.id === activePlaylistId);
    if (pl) {
      const keys = new Set(pl.trackKeys || []);
      tracks = realPhoneTracks.filter(t => keys.has(t.title + t.artist));
    }
  }
  if (tracks.length > 0) {
    playlist = [...tracks];
    renderTrackList();
    renderQueueDrawer();
    playTrack(0);
    showToast(`Reproduciendo playlist (${tracks.length} canciones)`, 'success', 'fa-play');
  } else {
    showToast('La playlist está vacía', 'warning', 'fa-circle-exclamation');
  }
});

document.getElementById('btn-delete-current-playlist')?.addEventListener('click', () => {
  if (!activePlaylistId || activePlaylistId === 'favorites') return;
  const pl = userPlaylists.find(p => p.id === activePlaylistId);
  if (!pl) return;
  if (confirm(`¿Eliminar la playlist "${pl.name}"?`)) {
    userPlaylists = userPlaylists.filter(p => p.id !== activePlaylistId);
    saveUserPlaylists();
    renderPlaylistsTab();
    showToast(`Playlist "${pl.name}" eliminada`, 'info', 'fa-trash-can');
  }
});

function openAddToPlaylistModal(track) {
  if (!track || !modalAddToPlaylist) return;
  ctxSelectedTrack = track;
  const nameEl = document.getElementById('add-pl-track-name');
  if (nameEl) nameEl.innerText = `${track.title} — ${track.artist}`;

  if (plPickerList) {
    plPickerList.innerHTML = '';
    if (userPlaylists.length === 0) {
      plPickerList.innerHTML = '<div style="color:var(--text-muted); font-size:0.82rem; padding:0.5rem;">No tienes listas creadas aún.</div>';
    } else {
      userPlaylists.forEach(pl => {
        const item = document.createElement('button');
        item.className = 'btn-outline-secondary';
        item.style.cssText = 'text-align:left; justify-content:space-between; display:flex; padding:0.6rem 0.85rem; font-size:0.84rem; width:100%;';
        const isAlready = (pl.trackKeys || []).includes(track.title + track.artist);
        item.innerHTML = `
          <span><i class="fa-solid fa-list" style="color:var(--accent-cyan); margin-right:0.4rem;"></i> ${pl.name}</span>
          <span style="font-size:0.75rem; color:${isAlready ? 'var(--accent-green)' : 'var(--text-dim)'};">${isAlready ? '✓ Añadida' : '+ Añadir'}</span>
        `;
        item.addEventListener('click', () => {
          if (!pl.trackKeys) pl.trackKeys = [];
          const key = track.title + track.artist;
          if (!pl.trackKeys.includes(key)) {
            pl.trackKeys.push(key);
            saveUserPlaylists();
            showToast(`Añadida a "${pl.name}"`, 'success', 'fa-circle-check');
          } else {
            showToast(`Ya está en "${pl.name}"`, 'info', 'fa-info');
          }
          modalAddToPlaylist.classList.add('hidden');
        });
        plPickerList.appendChild(item);
      });
    }
  }

  modalAddToPlaylist.classList.remove('hidden');
}

btnCloseAddPl?.addEventListener('click', () => modalAddToPlaylist?.classList.add('hidden'));
btnCloseCreatePl?.addEventListener('click', () => modalCreatePlaylist?.classList.add('hidden'));

btnCreatePlTrigger?.addEventListener('click', () => modalCreatePlaylist?.classList.remove('hidden'));
btnNewPlFromPicker?.addEventListener('click', () => {
  modalAddToPlaylist?.classList.add('hidden');
  modalCreatePlaylist?.classList.remove('hidden');
});

formCreatePlaylist?.addEventListener('submit', (e) => {
  e.preventDefault();
  const nameInput = document.getElementById('new-pl-name-input');
  const descInput = document.getElementById('new-pl-desc-input');
  const name = nameInput?.value?.trim();
  const desc = descInput?.value?.trim() || '';
  if (!name) return;

  const newPl = {
    id: 'pl_' + Date.now(),
    name: name,
    description: desc,
    trackKeys: ctxSelectedTrack ? [ctxSelectedTrack.title + ctxSelectedTrack.artist] : []
  };

  userPlaylists.push(newPl);
  saveUserPlaylists();
  renderPlaylistsTab();

  if (nameInput) nameInput.value = '';
  if (descInput) descInput.value = '';
  modalCreatePlaylist?.classList.add('hidden');
  showToast(`Playlist "${name}" creada`, 'success', 'fa-circle-check');
});

// Context menu playlist trigger
document.getElementById('ctx-btn-playlist')?.addEventListener('click', () => {
  if (ctxSelectedTrack) openAddToPlaylistModal(ctxSelectedTrack);
  closeContextMenu();
});

// ==========================================
// 25. FLOATING MINI-PLAYER & PICTURE-IN-PICTURE
// ==========================================
const floatingMiniPlayer = document.getElementById('floating-mini-player');
const fminiArt = document.getElementById('fmini-art');
const fminiTitle = document.getElementById('fmini-title');
const fminiArtist = document.getElementById('fmini-artist');
const fminiBtnPlay = document.getElementById('fmini-btn-play');
const fminiBtnPrev = document.getElementById('fmini-btn-prev');
const fminiBtnNext = document.getElementById('fmini-btn-next');
const fminiBtnClose = document.getElementById('fmini-btn-close');
const btnPipPlayer = document.getElementById('btn-pip-player');

function updateFloatingMiniUI() {
  if (!floatingMiniPlayer) return;
  const currentTrack = (currentIndex >= 0 && currentIndex < playlist.length) ? playlist[currentIndex] : null;
  if (currentTrack) {
    if (fminiTitle) fminiTitle.innerText = currentTrack.title;
    if (fminiArtist) fminiArtist.innerText = currentTrack.artist;
    if (fminiArt) fminiArt.src = currentTrack.coverUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="46" height="46"><rect width="46" height="46" fill="%23191c28"/></svg>';
  }
  if (fminiBtnPlay) {
    fminiBtnPlay.innerHTML = isPlaying ? '<i class="fa-solid fa-pause"></i>' : '<i class="fa-solid fa-play"></i>';
  }
}

function toggleFloatingMiniPlayer() {
  if (!floatingMiniPlayer) return;
  const isHidden = floatingMiniPlayer.classList.toggle('hidden');
  btnPipPlayer?.classList.toggle('active', !isHidden);
  if (!isHidden) {
    updateFloatingMiniUI();
  }
}

fminiBtnPlay?.addEventListener('click', () => playBtn?.click());
fminiBtnPrev?.addEventListener('click', () => prevBtn?.click());
fminiBtnNext?.addEventListener('click', () => nextBtn?.click());
fminiBtnClose?.addEventListener('click', () => {
  floatingMiniPlayer?.classList.add('hidden');
  btnPipPlayer?.classList.remove('active');
});

async function togglePictureInPicture() {
  const video = document.getElementById('pip-video');
  const canvas = document.getElementById('pip-canvas');

  if (document.pictureInPictureElement) {
    await document.exitPictureInPicture();
    btnPipPlayer?.classList.remove('active');
    return;
  }

  if (video && canvas && 'requestPictureInPicture' in video) {
    try {
      const ctx = canvas.getContext('2d');
      const drawPiP = () => {
        ctx.fillStyle = '#0f172a';
        ctx.fillRect(0, 0, 480, 270);

        // Gradient glow
        const grad = ctx.createLinearGradient(0, 0, 480, 270);
        grad.addColorStop(0, 'rgba(99, 102, 241, 0.4)');
        grad.addColorStop(1, 'rgba(6, 182, 212, 0.3)');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, 480, 270);

        // Text metadata
        const currentTrack = (currentIndex >= 0 && currentIndex < playlist.length) ? playlist[currentIndex] : null;
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 20px "Plus Jakarta Sans", sans-serif';
        ctx.fillText(currentTrack ? currentTrack.title : 'DaVE Player', 30, 90);

        ctx.fillStyle = '#94a3b8';
        ctx.font = '15px "Plus Jakarta Sans", sans-serif';
        ctx.fillText(currentTrack ? currentTrack.artist : 'Sin reproducción', 30, 125);

        // Animated neon bars
        ctx.fillStyle = '#06b6d4';
        const barsCount = 20;
        for (let b = 0; b < barsCount; b++) {
          const barHeight = isPlaying ? Math.random() * 65 + 10 : 8;
          ctx.fillRect(30 + b * 20, 230 - barHeight, 14, barHeight);
        }
      };

      drawPiP();
      const stream = canvas.captureStream(20);
      video.srcObject = stream;
      await video.play();
      await video.requestPictureInPicture();
      btnPipPlayer?.classList.add('active');

      if (pipDrawInterval) clearInterval(pipDrawInterval);
      pipDrawInterval = setInterval(drawPiP, 80);

      video.addEventListener('leavepictureinpicture', () => {
        clearInterval(pipDrawInterval);
        btnPipPlayer?.classList.remove('active');
      }, { once: true });
      return;
    } catch (err) {
      console.log('Native PiP not available, falling back to floating widget:', err);
    }
  }

  // Fallback: in-page floating widget
  toggleFloatingMiniPlayer();
}

btnPipPlayer?.addEventListener('click', togglePictureInPicture);

// ==========================================
// 26. STARTUP INITIALIZATION
// ==========================================
function initApp() {
  // 1. Render EQ Sliders immediately
  renderEqualizerUI();

  // 2. Load stored favorites & user playlists
  const savedFavs = localStorage.getItem('dave_favorites');
  if (savedFavs) {
    try {
      favorites = new Set(JSON.parse(savedFavs));
    } catch (e) {}
  }
  if (favoritesCount) favoritesCount.innerText = favorites.size;
  const filterFavCount = document.getElementById('filter-favs-count');
  if (filterFavCount) filterFavCount.innerText = favorites.size;
  if (pstatFavs) pstatFavs.innerText = favorites.size;

  loadUserPlaylists();

  // Normalizer button state
  const btnNormalizer = document.getElementById('btn-toggle-normalizer');
  if (btnNormalizer) btnNormalizer.classList.toggle('active', isNormalizerActive);

  // 3. Versioning y catálogo
  const CATALOG_VERSION = '3.8.0';
  localStorage.setItem('dave_catalog_ver', CATALOG_VERSION);

  // 4. Session check: ¿Existe sesión activa en esta sesión de navegación?
  const hasActiveSession = sessionStorage.getItem('dave_active_session') === 'true';
  const savedUser = localStorage.getItem('dave_user');
  let rememberedUser = null;
  if (savedUser) {
    try {
      rememberedUser = JSON.parse(savedUser);
    } catch (e) {}
  }

  // Pre-rellenar correo en el formulario inicial si existía un usuario previo
  if (rememberedUser && rememberedUser.email) {
    const fullLoginEmail = document.getElementById('full-login-email');
    if (fullLoginEmail && !fullLoginEmail.value) {
      fullLoginEmail.value = rememberedUser.email;
    }
  }

  if (!hasActiveSession) {
    // ESTRICTO: NO MOSTRAR NINGUNA MÚSICA Y MOSTRAR ÚNICAMENTE LA PANTALLA INICIAL DE INICIAR SESIÓN
    currentUser = null;
    playlist = [];
    phoneTracks = [];
    renderTrackList();
    renderPhoneTracksList();

    document.querySelector('.app-viewport')?.classList.add('hidden');
    const fullLogin = document.getElementById('full-login-screen');
    if (fullLogin) {
      fullLogin.classList.remove('hidden');
      const title = document.getElementById('full-login-status-title');
      const desc = document.getElementById('full-login-status-desc');
      if (title) title.innerText = 'Iniciar Sesión en DaVE Cloud';
      if (desc) desc.innerText = 'Inicia sesión con tu cuenta para acceder y escuchar todas las canciones que tienes guardadas en la nube.';
    }
  } else {
    // Sesión activa confirmada: cargar canciones de la nube y mostrar app
    currentUser = rememberedUser || { email: 'david.chaparro@daveplayer.app', name: 'David Chaparro' };
    phoneTracks = [...realPhoneTracks];
    playlist = [...realPhoneTracks];
    localStorage.setItem('dave_phone_tracks', JSON.stringify(phoneTracks));

    document.querySelector('.app-viewport')?.classList.remove('hidden');
    document.getElementById('full-login-screen')?.classList.add('hidden');
    setupFilterChips();
    renderTrackList();
    renderPhoneTracksList();
    if (playlist.length > 0) {
      loadTrackMeta(0);
    }
  }
  updateUserUI();

  // 5. Verificar si había un servidor de teléfono previamente conectado
  const savedConnectedIp = localStorage.getItem('dave_connected_phone_ip');
  if (savedConnectedIp && hasActiveSession) {
    startPhoneHeartbeat(savedConnectedIp);
  }
}

initApp();
