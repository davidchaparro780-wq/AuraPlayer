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

let stereoPanner = null;
let is8DActive = false;
let animFrame8D = null;
let orbitAngle = 0;

// Tube Warmth Saturation & Bass Exciter State
let tubeSaturationNode = null;
let bassExciterFilter = null;
let currentTubeMode = 'off';
let activeRingtoneAudio = null;

// Stereo Widener DSP State (Haas 3D Soundstage)
let stereoWidenerMode = 'normal'; // 'normal', 'wide', 'superwide'
let widenerInput = null;
let widenerSplitter = null;
let widenerMerger = null;
let widenerDirectL = null;
let widenerDirectR = null;
let widenerCrossDelayL = null;
let widenerCrossDelayR = null;
let widenerCrossGainL = null;
let widenerCrossGainR = null;
let widenerOutput = null;

// Pitch Shifter / Karaoke Transpose State
let currentPitchSemitones = 0;

let isSlowedReverb = false;

let totalSecondsListened = parseInt(localStorage.getItem('dave_total_seconds') || '0', 10);
let trackPlayCounts = JSON.parse(localStorage.getItem('dave_track_plays') || '{}');
let currentTheme = localStorage.getItem('dave_theme') || 'cyberpunk';

let sleepTimerInterval = null;
let sleepTimerRemaining = 0;
let sleepTimerMode = null; // null, 'minutes', 'end-track'

let userPlaylists = [];
let activePlaylistId = null;

let pipDrawInterval = null;

// Audio Recorder State
let mediaRecorder = null;
let recordedChunks = [];
let isRecording = false;
let recordTimer = null;
let recordSeconds = 0;
let audioRecorderDestination = null;

// Radio 24/7 State
let activeRadioStation = null;

// DJ Automix State
let isDJAutomix = localStorage.getItem('dave_dj_automix') === 'true';
let isDJTransitioning = false;

// 3D Visualizer State
let viz3dMode = 'tunnel';
let animFrame3D = null;

// A-B Looper State
let abLoopA = null;
let abLoopB = null;
let isABLooping = false;

// Speed Multi-Gear State
let currentPlaybackSpeed = 1.0;

// Ambience Soundscapes State
let ambienceNodes = [];
let ambienceGainNode = null;
let activeAmbienceSound = null;
let ambienceVolume = 0.35;

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

  // 6. Stereo Panner Node for 8D Orbital Audio
  if (audioCtx.createStereoPanner) {
    stereoPanner = audioCtx.createStereoPanner();
    stereoPanner.pan.value = 0;
  }

  // 6b. Vintage Tube Warmth & Bass Exciter
  tubeSaturationNode = audioCtx.createWaveShaper();
  tubeSaturationNode.curve = null;
  tubeSaturationNode.oversample = '4x';

  bassExciterFilter = audioCtx.createBiquadFilter();
  bassExciterFilter.type = 'peaking';
  bassExciterFilter.frequency.value = 55;
  bassExciterFilter.Q.value = 2.0;
  bassExciterFilter.gain.value = 0;

  // 7. Master Gain Node (Controls speaker volume without dampening analyser)
  masterGain = audioCtx.createGain();
  masterGain.gain.value = currentVolume;

  if (stereoPanner) {
    reverbDryGain.connect(stereoPanner);
    reverbWetGain.connect(stereoPanner);
    stereoPanner.connect(tubeSaturationNode);
  } else {
    reverbDryGain.connect(tubeSaturationNode);
    reverbWetGain.connect(tubeSaturationNode);
  }

  // 6c. 3D Stereo Widener (Haas Effect Soundstage Expander)
  widenerInput = audioCtx.createGain();
  widenerSplitter = audioCtx.createChannelSplitter(2);
  widenerMerger = audioCtx.createChannelMerger(2);
  widenerDirectL = audioCtx.createGain();
  widenerDirectR = audioCtx.createGain();
  widenerCrossDelayL = audioCtx.createDelay(0.05);
  widenerCrossDelayR = audioCtx.createDelay(0.05);
  widenerCrossGainL = audioCtx.createGain();
  widenerCrossGainR = audioCtx.createGain();
  widenerOutput = audioCtx.createGain();

  widenerInput.connect(widenerSplitter);
  // Direct paths
  widenerSplitter.connect(widenerDirectL, 0);
  widenerDirectL.connect(widenerMerger, 0, 0);
  widenerSplitter.connect(widenerDirectR, 1);
  widenerDirectR.connect(widenerMerger, 0, 1);
  // Cross widening paths
  widenerSplitter.connect(widenerCrossDelayL, 0);
  widenerCrossDelayL.connect(widenerCrossGainL);
  widenerCrossGainL.connect(widenerMerger, 0, 1);
  widenerSplitter.connect(widenerCrossDelayR, 1);
  widenerCrossDelayR.connect(widenerCrossGainR);
  widenerCrossGainR.connect(widenerMerger, 0, 0);

  widenerMerger.connect(widenerOutput);
  widenerCrossGainL.gain.value = 0;
  widenerCrossGainR.gain.value = 0;

  tubeSaturationNode.connect(bassExciterFilter);
  bassExciterFilter.connect(widenerInput);
  widenerOutput.connect(masterGain);

  masterGain.connect(audioCtx.destination);

  // 8. Studio Audio Recording Destination (Captures master output with all active effects)
  if (!audioRecorderDestination && audioCtx.createMediaStreamDestination) {
    audioRecorderDestination = audioCtx.createMediaStreamDestination();
    masterGain.connect(audioRecorderDestination);
  }

  // Audio element itself stays at 1.0 so analyser always receives full audio signal
  audio.volume = 1.0;

  // Restore active user sound enhancements if previously selected
  if (currentTubeMode && currentTubeMode !== 'off') {
    setTubeWarmth(currentTubeMode, true);
  }
  if (stereoWidenerMode && stereoWidenerMode !== 'normal') {
    setStereoWidener(stereoWidenerMode, true);
  }

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
  const vuBarL = document.getElementById('vu-bar-l');
  const vuBarR = document.getElementById('vu-bar-r');
  const vuPeakL = document.getElementById('vu-peak-l');
  const vuPeakR = document.getElementById('vu-peak-r');

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

    // Studio Stereo VU Meters (Peak dB & True Level)
    if (vuBarL && vuBarR) {
      if (isPlaying) {
        let sumL = 0;
        let sumR = 0;
        const half = Math.min(64, Math.floor(bufferLength / 2));
        for (let i = 0; i < half; i++) sumL += dataArray[i];
        for (let i = half; i < half * 2; i++) sumR += dataArray[i];
        const avgL = (sumL / half) / 255;
        const avgR = (sumR / half) / 255;
        const pctL = Math.min(100, Math.round(avgL * 135));
        const pctR = Math.min(100, Math.round(avgR * 135));
        vuBarL.style.width = `${pctL}%`;
        vuBarR.style.width = `${pctR}%`;

        const dbL = avgL > 0.02 ? (20 * Math.log10(avgL)).toFixed(1) : '-inf';
        const dbR = avgR > 0.02 ? (20 * Math.log10(avgR)).toFixed(1) : '-inf';
        if (vuPeakL) vuPeakL.innerText = `${dbL} dB`;
        if (vuPeakR) vuPeakR.innerText = `${dbR} dB`;
      } else {
        vuBarL.style.width = '0%';
        vuBarR.style.width = '0%';
        if (vuPeakL) vuPeakL.innerText = '-inf dB';
        if (vuPeakR) vuPeakR.innerText = '-inf dB';
      }
    }

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

  // Only scroll into view and toggle class if activeIdx has changed
  if (box.dataset.activeIdx !== String(activeIdx)) {
    box.dataset.activeIdx = String(activeIdx);
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

  // Sync Apple Music Fullscreen Lyrics if overlay is visible
  const fsBox = document.getElementById('fs-lyrics-container');
  const fsOverlay = document.getElementById('overlay-fullscreen-lyrics');
  if (fsBox && fsOverlay && !fsOverlay.classList.contains('hidden')) {
    if (fsBox.dataset.trackIndex !== String(currentIndex) || fsBox.children.length !== currentLyrics.length) {
      fsBox.innerHTML = '';
      fsBox.dataset.trackIndex = String(currentIndex);
      fsBox.dataset.activeIdx = '-1';
      currentLyrics.forEach((line) => {
        const div = document.createElement('div');
        div.className = 'fs-lyric-line';
        div.dataset.time = line.time;
        div.innerText = line.text;
        div.addEventListener('click', () => {
          audio.currentTime = line.time;
        });
        fsBox.appendChild(div);
      });
    }

    if (fsBox.dataset.activeIdx !== String(activeIdx)) {
      fsBox.dataset.activeIdx = String(activeIdx);
      const fsLines = fsBox.querySelectorAll('.fs-lyric-line');
      fsLines.forEach((l, idx) => {
        if (idx === activeIdx) {
          l.classList.add('active');
          l.scrollIntoView({ behavior: 'smooth', block: 'center' });
        } else {
          l.classList.remove('active');
        }
      });
    }
  }
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
  isDJTransitioning = false;

  if (activeRadioStation) {
    activeRadioStation = null;
    document.querySelectorAll('.radio-station-card').forEach(c => c.classList.remove('active-station'));
  }

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

    applyPlaybackRateAndPitch();

    // Track playback statistics
    const trackKey = `${track.title} — ${track.artist}`;
    trackPlayCounts[trackKey] = (trackPlayCounts[trackKey] || 0) + 1;
    localStorage.setItem('dave_track_plays', JSON.stringify(trackPlayCounts));

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

  // Update Fullscreen lyrics & Ringtone modal headers
  const fsTitle = document.getElementById('fs-lyrics-title');
  const fsArtist = document.getElementById('fs-lyrics-artist');
  const fsArt = document.getElementById('fs-lyrics-art');
  const fsBg = document.getElementById('fs-lyrics-bg');
  if (fsTitle) fsTitle.innerText = track.title;
  if (fsArtist) fsArtist.innerText = track.artist;
  if (fsArt) fsArt.src = track.coverUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="54" height="54"><rect width="54" height="54" fill="%23191c28"/></svg>';
  if (fsBg && track.coverUrl) fsBg.style.backgroundImage = `url("${track.coverUrl}")`;

  const rtTitle = document.getElementById('rt-song-title');
  const rtArtist = document.getElementById('rt-song-artist');
  if (rtTitle) rtTitle.innerText = track.title;
  if (rtArtist) rtArtist.innerText = track.artist;

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
  if (activeRadioStation) {
    currentTimeEl.innerText = '🔴 EN VIVO';
    totalTimeEl.innerText = 'RADIO';
    progressFill.style.width = '100%';
    return;
  }

  if (!audio.duration || !isFinite(audio.duration)) return;

  // A-B Looper Jump
  if (isABLooping && abLoopA !== null && abLoopB !== null && audio.currentTime >= abLoopB) {
    audio.currentTime = abLoopA;
    return;
  }

  const ratio = audio.currentTime / audio.duration;
  progressFill.style.width = `${ratio * 100}%`;
  currentTimeEl.innerText = formatTime(audio.currentTime);
  totalTimeEl.innerText = formatTime(audio.duration);
  renderLyrics(audio.currentTime);

  // DaVE DJ Automix Crossfade
  if (isDJAutomix && !activeRadioStation && playlist.length > 1 && audio.duration > 20) {
    const remaining = audio.duration - audio.currentTime;
    if (remaining <= 5.5 && !isDJTransitioning) {
      triggerDJCrossfade();
    }
  }

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

  // Track listening time statistics
  if (isPlaying) {
    totalSecondsListened += 0.25;
    if (Math.floor(totalSecondsListened) % 15 === 0) {
      localStorage.setItem('dave_total_seconds', Math.floor(totalSecondsListened).toString());
    }
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
function highlightMatch(text, query) {
  if (!text) return '';
  const str = String(text);
  if (!query || !query.trim()) return escapeHtml(str);
  const cleanQ = query.trim();
  const escapedQ = cleanQ.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const regex = new RegExp(`(${escapedQ})`, 'gi');
  return escapeHtml(str).replace(regex, '<mark class="search-highlight">$1</mark>');
}

function createTrackRow(track, displayIndex, isPhoneTab = false, searchQuery = '') {
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
        <span class="track-title-text" style="font-weight:600; white-space:nowrap; text-overflow:ellipsis; overflow:hidden; font-size:0.9rem; color:#fff;">${highlightMatch(track.title, searchQuery)}</span>
        <span class="track-artist-sub mobile-sub-artist" style="font-size:0.75rem; color:var(--text-dim); white-space:nowrap; text-overflow:ellipsis; overflow:hidden;">${highlightMatch(track.artist, searchQuery)}</span>
      </div>
    </td>
    <td class="track-artist-col" style="white-space:nowrap; text-overflow:ellipsis; overflow:hidden;">${highlightMatch(track.artist, searchQuery)}</td>
    <td class="track-album-col" style="white-space:nowrap; text-overflow:ellipsis; overflow:hidden;">${highlightMatch(track.album || 'Infinix HOT 40i', searchQuery)}</td>
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
const categoryEmptyState = document.getElementById('category-empty-state');
const categoryEmptyDesc = document.getElementById('category-empty-desc');
const btnCategoryAllAction = document.getElementById('btn-category-all-action');

if (btnCategoryAllAction) {
  btnCategoryAllAction.addEventListener('click', () => {
    const allChip = document.querySelector('.filter-chip[data-filter="all"]');
    if (allChip) allChip.click();
  });
}

function renderTrackList(filtered = null, searchQuery = '') {
  const list = filtered !== null ? filtered : playlist;
  if (songsCount) songsCount.innerText = list.length;
  const filterAllCount = document.getElementById('filter-all-count');
  if (filterAllCount) filterAllCount.innerText = playlist.length;
  const filterFavCount = document.getElementById('filter-favs-count');
  if (filterFavCount) filterFavCount.innerText = favorites.size;

  if (list.length === 0) {
    trackTable?.classList.add('hidden');
    if (searchQuery) {
      emptyState?.classList.add('hidden');
      categoryEmptyState?.classList.add('hidden');
      if (searchEmptyState) {
        searchEmptyState.classList.remove('hidden');
        if (searchEmptyText) {
          searchEmptyText.innerHTML = `No se encontraron canciones que coincidan con "<strong>${escapeHtml(searchQuery)}</strong>".`;
        }
      }
    } else if (currentFilter !== 'all' && playlist.length > 0) {
      // Filtrado por categoría específica o favoritos vacíos: mostrar aviso amigable sin romper la vista
      searchEmptyState?.classList.add('hidden');
      emptyState?.classList.add('hidden');
      if (categoryEmptyState) {
        categoryEmptyState.classList.remove('hidden');
        if (categoryEmptyDesc) {
          categoryEmptyDesc.innerText = currentFilter === 'favs'
            ? 'No tienes canciones favoritas aún. Toca el corazón ❤️ en cualquier canción para guardarla aquí.'
            : 'No hay canciones en esta categoría por ahora.';
        }
      }
    } else {
      categoryEmptyState?.classList.add('hidden');
      searchEmptyState?.classList.add('hidden');
      emptyState?.classList.remove('hidden');
    }
    return;
  }

  categoryEmptyState?.classList.add('hidden');
  searchEmptyState?.classList.add('hidden');
  emptyState?.classList.add('hidden');
  trackTable?.classList.remove('hidden');
  if (!trackList) return;
  trackList.innerHTML = '';

  list.forEach((track, i) => {
    const tr = createTrackRow(track, i, false, searchQuery);
    trackList.appendChild(tr);
  });
}

let currentSortColumn = null; // 'index' | 'title' | 'artist' | 'album' | 'duration'
let currentSortDirection = 'asc'; // 'asc' | 'desc'

function updateSortHeaderUI() {
  document.querySelectorAll('#track-table th.th-sortable').forEach(th => {
    const col = th.dataset.sort;
    th.classList.remove('sorted-asc', 'sorted-desc');
    if (col === currentSortColumn) {
      th.classList.add(currentSortDirection === 'asc' ? 'sorted-asc' : 'sorted-desc');
    }
  });
}

function setupTableSorting() {
  document.querySelectorAll('#track-table th.th-sortable').forEach(th => {
    th.addEventListener('click', () => {
      const col = th.dataset.sort;
      if (currentSortColumn === col) {
        if (currentSortDirection === 'asc') {
          currentSortDirection = 'desc';
        } else {
          currentSortColumn = null;
          currentSortDirection = 'asc';
        }
      } else {
        currentSortColumn = col;
        currentSortDirection = 'asc';
      }
      updateSortHeaderUI();
      applyCurrentFilter();
    });
  });
}

function applyCurrentFilter() {
  const rawQ = searchInput?.value || '';
  const q = normalizeStr(rawQ);

  if (searchClearBtn) {
    searchClearBtn.style.display = rawQ ? 'block' : 'none';
  }

  // Si hay búsqueda global de texto, buscar en TODA la biblioteca y actualizar chip a "Todas"
  if (q) {
    currentFilter = 'all';
    const container = document.getElementById('category-chips');
    if (container) {
      container.querySelectorAll('.filter-chip').forEach(c => {
        c.classList.toggle('active', c.dataset.filter === 'all');
      });
    }
  }

  // Si el usuario escribe y está en otra pestaña que no es canciones ni celular, cambiar a canciones
  const activePane = document.querySelector('.tab-pane.active');
  const activeTabId = activePane ? activePane.id : '';
  if (q && activeTabId !== 'tab-songs' && activeTabId !== 'tab-phone') {
    switchTab('songs');
  }

  let list = playlist;

  if (currentFilter === 'urban') {
    list = playlist.filter(t => /bad bunny|blessd|cris mj|westcol|beéle|daddy yankee|floyymenor|louki|urbano|trap|reggaeton|después de la una|qué lío|nadie sabe|dos mil 16|somos de calle|la plena/i.test(`${t.title} ${t.artist} ${t.album}`));
  } else if (currentFilter === 'pop') {
    list = playlist.filter(t => /lady gaga|rihanna|ace of base|bôa|boa|marta s[aá]nchez|pop|dance|breakin' dishes|happy nation|paparazzi|bad romance|just dance|abracadabra|bloody mary|duvet/i.test(`${t.title} ${t.artist} ${t.album}`));
  } else if (currentFilter === 'cyber') {
    list = playlist.filter(t => /cyberpunk|edgerunners|dawid|adamczyk|phantom|topic|gigi|ziraki|forssell|electro|synthwave|sound|let you down|color your night|a phantom pain|tanzen|l'amour|ssrhd/i.test(`${t.title} ${t.artist} ${t.album}`));
  } else if (currentFilter === 'workout') {
    list = playlist.filter(t => /bad bunny|blessd|cris mj|daddy yankee|rihanna|lady gaga|gaga|abracadabra|romance|paparazzi|dishes|pose|somos de calle|qu[eé] l[íi]o|phantom|l'amour|tanzen|remix|ziraki|edm|electro|dance|dubstep|focus|gym|workout|energy|power|despu[eé]s de la una|la plena|bloody mary/i.test(`${t.title} ${t.artist} ${t.album}`));
  } else if (currentFilter === 'chill') {
    list = playlist.filter(t => /happy nation|ace of base|guardian|curly|let you down|dawid|duvet|bôa|boa|color your night|lotus juice|phantom pain|ludvig|dame fuerza|marta s[aá]nchez|synthwave|lo-fi|ambient|relax|chill|slow|calm|qora/i.test(`${t.title} ${t.artist} ${t.album}`));
  } else if (currentFilter === 'party') {
    list = playlist.filter(t => /cris mj|floyymenor|blessd|daddy yankee|gigi|tanzen|l'amour|just dance|bad romance|paparazzi|la plena|beéle|westcol|fiesta|club|party|perreo|remix|pose|dos mil 16/i.test(`${t.title} ${t.artist} ${t.album}`));
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

  // Ordenación por columnas
  if (currentSortColumn) {
    list = [...list];
    if (currentSortColumn === 'title') {
      list.sort((a, b) => currentSortDirection === 'asc' ? (a.title || '').localeCompare(b.title || '') : (b.title || '').localeCompare(a.title || ''));
    } else if (currentSortColumn === 'artist') {
      list.sort((a, b) => currentSortDirection === 'asc' ? (a.artist || '').localeCompare(b.artist || '') : (b.artist || '').localeCompare(a.artist || ''));
    } else if (currentSortColumn === 'album') {
      list.sort((a, b) => currentSortDirection === 'asc' ? (a.album || '').localeCompare(b.album || '') : (b.album || '').localeCompare(a.album || ''));
    } else if (currentSortColumn === 'duration') {
      list.sort((a, b) => currentSortDirection === 'asc' ? (a.duration || 0) - (b.duration || 0) : (b.duration || 0) - (a.duration || 0));
    }
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

  container.querySelectorAll('.filter-chip:not(.chip-shuffle)').forEach(chip => {
    chip.onclick = () => {
      container.querySelectorAll('.filter-chip:not(.chip-shuffle)').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      currentFilter = chip.dataset.filter;
      // Limpiar búsqueda de texto para respetar el filtro seleccionado
      if (searchInput && searchInput.value) {
        searchInput.value = '';
        if (searchClearBtn) searchClearBtn.style.display = 'none';
      }
      // Abrir inmediatamente la pestaña de canciones para mostrar el filtro
      switchTab('songs');
      applyCurrentFilter();
    };
  });

  const btnShuffleAll = document.getElementById('btn-shuffle-all');
  if (btnShuffleAll) {
    btnShuffleAll.onclick = () => {
      if (playlist.length === 0) {
        showToast('No hay canciones para mezclar', 'warning', 'fa-circle-exclamation');
        return;
      }
      isShuffle = true;
      document.getElementById('btn-shuffle')?.classList.add('active');
      for (let i = playlist.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [playlist[i], playlist[j]] = [playlist[j], playlist[i]];
      }
      switchTab('songs');
      renderTrackList();
      renderQueueDrawer();
      playTrack(0);
      showToast(`🎲 Mezclando toda la biblioteca (${playlist.length} canciones)`, 'success', 'fa-shuffle');
    };
  }
}

// Dropdown "Herramientas" en Topbar
const btnMoreTools = document.getElementById('btn-more-tools');
const menuMoreTools = document.getElementById('menu-more-tools');

if (btnMoreTools && menuMoreTools) {
  btnMoreTools.addEventListener('click', (e) => {
    e.stopPropagation();
    menuMoreTools.classList.toggle('show');
  });

  menuMoreTools.querySelectorAll('.more-menu-item').forEach(item => {
    item.addEventListener('click', () => {
      menuMoreTools.classList.remove('show');
    });
  });

  document.addEventListener('click', (e) => {
    if (!menuMoreTools.contains(e.target) && e.target !== btnMoreTools && !btnMoreTools.contains(e.target)) {
      menuMoreTools.classList.remove('show');
    }
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
  } else if (name === 'radio') {
    renderRadioStations();
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
  setupFilterChips();
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

  // 5. Cerrar cualquier modal abierto y detener previews de audio
  modalAuth?.classList.add('hidden');
  modalSync?.classList.add('hidden');
  document.getElementById('modal-shortcuts')?.classList.add('hidden');
  document.getElementById('modal-equalizer')?.classList.add('hidden');
  document.getElementById('modal-spatial-reverb')?.classList.add('hidden');
  document.getElementById('modal-speed')?.classList.add('hidden');
  document.getElementById('modal-ringtone-maker')?.classList.add('hidden');
  document.getElementById('modal-pitch-shifter')?.classList.add('hidden');
  document.getElementById('modal-backup')?.classList.add('hidden');
  document.getElementById('modal-tubewarmth')?.classList.add('hidden');
  document.getElementById('overlay-fullscreen-lyrics')?.classList.add('hidden');
  document.getElementById('fx-studio-tray')?.classList.add('hidden');
  document.getElementById('btn-toggle-fx-tray')?.classList.remove('active');
  if (activeRingtoneAudio) {
    try {
      activeRingtoneAudio.pause();
      activeRingtoneAudio = null;
    } catch (e) {}
  }

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
    let nameWithoutExt = file.name.replace(/\.[^/.]+$/, "");
    let artist = 'Mi Celular';
    let title = nameWithoutExt;

    // Detectar si el archivo tiene formato "Artista - Título"
    if (nameWithoutExt.includes(' - ')) {
      const parts = nameWithoutExt.split(' - ');
      artist = parts[0].replace(/_/g, ' ').trim();
      title = parts.slice(1).join(' - ').replace(/_/g, ' ').trim();
    }
    // Limpiar coletillas comunes de descargas (YouTube, Snaptube, etc.)
    title = title.replace(/\s*[\(\[](official\s*(music\s*)?video|audio|lyric(s)?|visualizer|m4a_\d+k|video|hd|hq|sub\.\s*español)[\)\]]/gi, '').trim();

    return {
      title: title || nameWithoutExt,
      artist: artist,
      album: 'Descargas de Celular',
      duration: 0,
      fileObj: file,
      streamUrl: URL.createObjectURL(file),
      coverUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80'
    };
  });

  // Integrar canciones evitando duplicados
  newTracks.forEach(nt => {
    const existingIdx = phoneTracks.findIndex(pt => pt.title.toLowerCase() === nt.title.toLowerCase() && pt.artist.toLowerCase() === nt.artist.toLowerCase());
    if (existingIdx !== -1) {
      phoneTracks[existingIdx] = nt;
    } else {
      phoneTracks.unshift(nt);
    }

    const plIdx = playlist.findIndex(pt => pt.title.toLowerCase() === nt.title.toLowerCase() && pt.artist.toLowerCase() === nt.artist.toLowerCase());
    if (plIdx !== -1) {
      playlist[plIdx] = nt;
    } else {
      playlist.unshift(nt);
    }
  });

  localStorage.setItem('dave_phone_tracks', JSON.stringify(phoneTracks.map(t => ({
    title: t.title,
    artist: t.artist,
    album: t.album,
    duration: t.duration,
    coverUrl: t.coverUrl
  }))));

  renderPhoneTracksList();
  renderTrackList();
  updateUserUI();

  showToast(`✅ ¡${newTracks.length} canción(es) nueva(s) sincronizadas desde tu celular!`, 'success', 'fa-mobile-screen-button');

  // Reproducir inmediatamente la primera si no había música sonando
  if (!isPlaying && newTracks.length > 0) {
    const idx = playlist.indexOf(newTracks[0]);
    if (idx !== -1) playTrack(idx);
  }
}

phoneFileInput?.addEventListener('change', (e) => handlePhoneBrowserFiles(Array.from(e.target.files)));
phoneFolderInput?.addEventListener('change', (e) => handlePhoneBrowserFiles(Array.from(e.target.files)));

document.getElementById('btn-phone-upload-files')?.addEventListener('click', () => phoneFileInput?.click());
document.getElementById('btn-phone-upload-folder')?.addEventListener('click', () => phoneFolderInput?.click());
document.getElementById('btn-phone-quick-add')?.addEventListener('click', () => phoneFileInput?.click());
document.getElementById('btn-empty-phone-files')?.addEventListener('click', () => phoneFileInput?.click());
document.getElementById('btn-empty-phone-folder')?.addEventListener('click', () => phoneFolderInput?.click());
document.getElementById('btn-modal-upload-phone')?.addEventListener('click', () => {
  modalSync?.classList.add('hidden');
  phoneFolderInput?.click();
});

// Modal de Ayuda: Sincronización Celular USB & WiFi
const modalPhoneSyncHelp = document.getElementById('modal-phone-sync-help');
const btnPhoneHelpGuide = document.getElementById('btn-phone-help-guide');
const btnClosePhoneHelp = document.getElementById('btn-close-phone-help');
const btnPhoneHelpGotIt = document.getElementById('btn-phone-help-got-it');

btnPhoneHelpGuide?.addEventListener('click', () => modalPhoneSyncHelp?.classList.remove('hidden'));
btnClosePhoneHelp?.addEventListener('click', () => modalPhoneSyncHelp?.classList.add('hidden'));
btnPhoneHelpGotIt?.addEventListener('click', () => modalPhoneSyncHelp?.classList.add('hidden'));
modalPhoneSyncHelp?.addEventListener('click', (e) => {
  if (e.target === modalPhoneSyncHelp) modalPhoneSyncHelp.classList.add('hidden');
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
    showHUD(isPlaying ? 'fa-pause' : 'fa-play', isPlaying ? 'Pausa' : 'Reproduciendo', playlist[currentIndex]?.title || '');
  } else if (e.code === 'ArrowRight') {
    if (e.shiftKey) nextBtn.click();
    else if (audio.duration) {
      audio.currentTime = Math.min(audio.duration, audio.currentTime + 5);
      showHUD('fa-forward', '+5s', formatTime(audio.currentTime));
    }
  } else if (e.code === 'ArrowLeft') {
    if (e.shiftKey) prevBtn.click();
    else {
      audio.currentTime = Math.max(0, audio.currentTime - 5);
      showHUD('fa-backward', '-5s', formatTime(audio.currentTime));
    }
  } else if (e.code === 'ArrowUp') {
    e.preventDefault();
    volumeSlider.value = Math.min(100, parseInt(volumeSlider.value) + 5);
    volumeSlider.dispatchEvent(new Event('input'));
    showHUD('fa-volume-high', 'Volumen', `${volumeSlider.value}%`);
  } else if (e.code === 'ArrowDown') {
    e.preventDefault();
    volumeSlider.value = Math.max(0, parseInt(volumeSlider.value) - 5);
    volumeSlider.dispatchEvent(new Event('input'));
    showHUD('fa-volume-high', 'Volumen', `${volumeSlider.value}%`);
  } else if (e.code === 'KeyM') {
    muteBtn.click();
    showHUD(isMuted ? 'fa-volume-xmark' : 'fa-volume-high', isMuted ? 'Silenciado' : 'Sonido Activo', `${volumeSlider.value}%`);
  } else if (e.code === 'KeyL') {
    switchTab('lyrics');
    showHUD('fa-microphone-lines', 'Karaoke', 'Letras Sincronizadas');
  } else if (e.code === 'KeyS') {
    shuffleBtn?.click();
    showHUD('fa-shuffle', isShuffle ? 'Aleatorio Activado' : 'Aleatorio Desactivado', '');
  } else if (e.code === 'KeyR') {
    repeatBtn?.click();
    const modes = ['Repetir Desactivado', 'Repetir Lista', 'Repetir Canción'];
    showHUD('fa-repeat', modes[repeatMode] || 'Repetir', '');
  } else if (e.code === 'KeyF') {
    document.getElementById('btn-party-fullscreen')?.click();
    showHUD('fa-expand', 'Pantalla Completa', 'Modo Visualizador');
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
  if (totalTimeEl && track.duration) {
    totalTimeEl.innerText = formatTime(track.duration);
  }
  if (currentTimeEl) {
    currentTimeEl.innerText = '0:00';
  }
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
// 27. DYNAMIC 8D AUDIO ENGINE (360° ORBIT)
// ==========================================
const btnToggle8D = document.getElementById('btn-toggle-8d');

function loop8D() {
  if (!is8DActive) return;
  orbitAngle += 0.022; // ~10 seconds per full 360-degree rotation
  if (stereoPanner && audioCtx) {
    try {
      const panVal = Math.sin(orbitAngle);
      stereoPanner.pan.setValueAtTime(panVal, audioCtx.currentTime);
    } catch (e) {}
  }
  animFrame8D = requestAnimationFrame(loop8D);
}

function toggle8DAudio() {
  initAudioEngine();
  is8DActive = !is8DActive;

  if (btnToggle8D) {
    btnToggle8D.classList.toggle('active', is8DActive);
    btnToggle8D.classList.toggle('orbit-active', is8DActive);
  }

  if (is8DActive) {
    orbitAngle = 0;
    loop8D();
    showToast('🎧 Audio 8D Órbita 360° activado (Usa audífonos)', 'success', 'fa-headphones');
  } else {
    if (animFrame8D) cancelAnimationFrame(animFrame8D);
    if (stereoPanner && audioCtx) {
      try {
        stereoPanner.pan.setValueAtTime(0, audioCtx.currentTime);
      } catch (e) {}
    }
    showToast('Audio 8D desactivado', 'info', 'fa-headphones');
  }
  if (typeof updateActiveFXBadge === 'function') updateActiveFXBadge();
}

btnToggle8D?.addEventListener('click', toggle8DAudio);

// ==========================================
// 28. SLOWED + REVERB MODE (1-CLICK NOSTALGIA)
// ==========================================
const btnSlowedReverb = document.getElementById('btn-slowed-reverb');

function toggleSlowedReverb() {
  initAudioEngine();
  isSlowedReverb = !isSlowedReverb;

  if (btnSlowedReverb) {
    btnSlowedReverb.classList.toggle('active', isSlowedReverb);
  }

  if (isSlowedReverb) {
    applyPlaybackRateAndPitch();
    setSpatialReverb('cathedral');
    showToast('🌌 Modo Slowed + Reverb activado (0.85x + Catedral)', 'success', 'fa-hourglass-start');
  } else {
    applyPlaybackRateAndPitch();
    setSpatialReverb('off');
    showToast('Modo normal restaurado (1.0x)', 'info', 'fa-hourglass-start');
  }
  if (typeof updateActiveFXBadge === 'function') updateActiveFXBadge();
}

btnSlowedReverb?.addEventListener('click', toggleSlowedReverb);

// ==========================================
// 29. THEME SWITCHER (CUSTOM COLOR PALETTES)
// ==========================================
const themes = {
  cyberpunk: {
    name: 'Cyber Glow',
    primary: '#6366f1',
    primaryGlow: 'rgba(99, 102, 241, 0.5)',
    cyan: '#06b6d4',
    pink: '#ec4899',
    borderGlow: 'rgba(99, 102, 241, 0.35)'
  },
  emerald: {
    name: 'Emerald Green',
    primary: '#10b981',
    primaryGlow: 'rgba(16, 185, 129, 0.5)',
    cyan: '#34d399',
    pink: '#6ee7b7',
    borderGlow: 'rgba(16, 185, 129, 0.35)'
  },
  crimson: {
    name: 'Crimson Red',
    primary: '#ef4444',
    primaryGlow: 'rgba(239, 68, 68, 0.5)',
    cyan: '#f87171',
    pink: '#fb7185',
    borderGlow: 'rgba(239, 68, 68, 0.35)'
  },
  sunset: {
    name: 'Sunset Wave',
    primary: '#ec4899',
    primaryGlow: 'rgba(236, 72, 153, 0.5)',
    cyan: '#f97316',
    pink: '#f43f5e',
    borderGlow: 'rgba(236, 72, 153, 0.35)'
  },
  diamond: {
    name: 'Ice Diamond',
    primary: '#38bdf8',
    primaryGlow: 'rgba(56, 189, 248, 0.5)',
    cyan: '#7dd3fc',
    pink: '#cbd5e1',
    borderGlow: 'rgba(56, 189, 248, 0.35)'
  }
};

const modalTheme = document.getElementById('modal-theme');
const btnThemeSelector = document.getElementById('btn-theme-selector');
const btnCloseTheme = document.getElementById('btn-close-theme');

function applyTheme(themeKey, notify = false) {
  const t = themes[themeKey] || themes.cyberpunk;
  currentTheme = themeKey;
  localStorage.setItem('dave_theme', themeKey);

  const root = document.documentElement;
  root.style.setProperty('--primary', t.primary);
  root.style.setProperty('--primary-glow', t.primaryGlow);
  root.style.setProperty('--accent-cyan', t.cyan);
  root.style.setProperty('--accent-pink', t.pink);
  root.style.setProperty('--border-glow', t.borderGlow);

  document.querySelectorAll('.theme-card').forEach(card => {
    card.classList.toggle('active', card.dataset.theme === themeKey);
  });

  if (notify) {
    showToast(`Tema visual: ${t.name}`, 'success', 'fa-palette');
  }
}

btnThemeSelector?.addEventListener('click', () => modalTheme?.classList.remove('hidden'));
btnCloseTheme?.addEventListener('click', () => modalTheme?.classList.add('hidden'));

document.querySelectorAll('.theme-card').forEach(card => {
  card.addEventListener('click', () => {
    applyTheme(card.dataset.theme, true);
    modalTheme?.classList.add('hidden');
  });
});

// ==========================================
// 30. DAVE STATS (PERSONAL LISTENING SUMMARY)
// ==========================================
const modalStats = document.getElementById('modal-stats');
const btnViewStats = document.getElementById('btn-view-stats');
const btnCloseStats = document.getElementById('btn-close-stats');
const statTotalTime = document.getElementById('stat-total-time');
const statTotalPlays = document.getElementById('stat-total-plays');
const statTopArtist = document.getElementById('stat-top-artist');
const statFavsCount = document.getElementById('stat-favs-count');
const statTopTracks = document.getElementById('stat-top-tracks');

function renderStatsModal() {
  if (!modalStats) return;

  // 1. Total listening time
  const totalMins = Math.floor(totalSecondsListened / 60);
  if (statTotalTime) {
    if (totalMins >= 60) {
      const hrs = Math.floor(totalMins / 60);
      const mins = totalMins % 60;
      statTotalTime.innerText = `${hrs}h ${mins}m`;
    } else {
      statTotalTime.innerText = `${totalMins} min`;
    }
  }

  // 2. Play counts and Top Tracks
  const trackEntries = Object.entries(trackPlayCounts);
  let totalPlays = 0;
  const artistCounts = {};

  trackEntries.forEach(([key, count]) => {
    totalPlays += count;
    const parts = key.split(' — ');
    const artist = parts[1] || 'Varios';
    artistCounts[artist] = (artistCounts[artist] || 0) + count;
  });

  if (statTotalPlays) statTotalPlays.innerText = totalPlays;

  // 3. Top Artist
  let topArtist = '-';
  let topArtistCount = 0;
  Object.entries(artistCounts).forEach(([art, count]) => {
    if (count > topArtistCount) {
      topArtistCount = count;
      topArtist = art;
    }
  });
  if (statTopArtist) statTopArtist.innerText = topArtist;

  // 4. Favorites count
  if (statFavsCount) statFavsCount.innerText = favorites.size;

  // 5. Top 5 Tracks List
  if (statTopTracks) {
    statTopTracks.innerHTML = '';
    const sortedTracks = trackEntries.sort((a, b) => b[1] - a[1]).slice(0, 5);

    if (sortedTracks.length === 0) {
      statTopTracks.innerHTML = '<div style="color:var(--text-muted); font-size:0.82rem; padding:0.5rem; text-align:center;">Aún no hay reproducciones registradas. ¡Escucha música para ver tus estadísticas!</div>';
    } else {
      sortedTracks.forEach(([key, count], idx) => {
        const parts = key.split(' — ');
        const title = parts[0];
        const artist = parts[1] || '';
        const item = document.createElement('div');
        item.className = 'top-track-item';
        item.innerHTML = `
          <span class="top-track-rank">#${idx + 1}</span>
          <div class="top-track-meta">
            <strong>${title}</strong>
            <span>${artist}</span>
          </div>
          <span class="top-track-count">${count} ${count === 1 ? 'play' : 'plays'}</span>
        `;
        statTopTracks.appendChild(item);
      });
    }
  }

  modalStats.classList.remove('hidden');
}

btnViewStats?.addEventListener('click', renderStatsModal);
btnCloseStats?.addEventListener('click', () => modalStats?.classList.add('hidden'));

// ==========================================
// 32. LIVE AUDIO EFFECT RECORDER (v4.0.0)
// ==========================================
const btnRecordAudio = document.getElementById('btn-record-audio');
const recordTimerBadge = document.getElementById('record-timer-badge');

function toggleAudioRecording() {
  if (isRecording) {
    stopAudioRecording();
  } else {
    startAudioRecording();
  }
}

function startAudioRecording() {
  initAudioEngine();
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }

  if (typeof MediaRecorder === 'undefined') {
    showToast('Tu navegador no soporta MediaRecorder para grabar audio.', 'warning', 'fa-triangle-exclamation');
    return;
  }

  if (!audioRecorderDestination && audioCtx.createMediaStreamDestination) {
    audioRecorderDestination = audioCtx.createMediaStreamDestination();
    masterGain.connect(audioRecorderDestination);
  }

  if (!audioRecorderDestination) {
    showToast('No se pudo inicializar el canal de captura de audio.', 'warning', 'fa-triangle-exclamation');
    return;
  }

  try {
    const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus') ? 'audio/webm;codecs=opus' : (MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : '');
    mediaRecorder = mimeType ? new MediaRecorder(audioRecorderDestination.stream, { mimeType }) : new MediaRecorder(audioRecorderDestination.stream);
    recordedChunks = [];

    mediaRecorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) {
        recordedChunks.push(e.data);
      }
    };

    mediaRecorder.onstop = () => {
      saveRecordedAudio();
    };

    mediaRecorder.start(250);
    isRecording = true;
    recordSeconds = 0;

    btnRecordAudio?.classList.add('recording-active');
    if (recordTimerBadge) {
      recordTimerBadge.innerText = '0:00';
      recordTimerBadge.classList.remove('hidden');
    }

    recordTimer = setInterval(() => {
      recordSeconds++;
      if (recordTimerBadge) {
        recordTimerBadge.innerText = formatTime(recordSeconds);
      }
    }, 1000);

    showToast('🔴 Grabando audio con efectos en vivo... Haz clic de nuevo para guardar.', 'info', 'fa-circle-dot');
  } catch (err) {
    console.error('Error starting recorder:', err);
    showToast('Error al iniciar la grabación de audio.', 'warning', 'fa-triangle-exclamation');
  }
}

function stopAudioRecording() {
  if (!isRecording) return;
  clearInterval(recordTimer);
  isRecording = false;

  btnRecordAudio?.classList.remove('recording-active');
  if (recordTimerBadge) {
    recordTimerBadge.classList.add('hidden');
  }

  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop();
  }
}

function saveRecordedAudio() {
  if (recordedChunks.length === 0) {
    showToast('No se capturó audio durante la grabación.', 'warning', 'fa-circle-exclamation');
    return;
  }

  const blob = new Blob(recordedChunks, { type: 'audio/webm' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');

  let baseName = 'Audio';
  if (activeRadioStation) {
    baseName = activeRadioStation.title.replace(/[^a-zA-Z0-9_-]/g, '_');
  } else if (currentIndex >= 0 && playlist[currentIndex]) {
    baseName = playlist[currentIndex].title.replace(/[^a-zA-Z0-9_-]/g, '_');
  }

  a.href = url;
  a.download = `DaVE_Master_${baseName}_${Date.now()}.webm`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);

  setTimeout(() => URL.revokeObjectURL(url), 10000);
  showToast(`💾 ¡Grabación guardada con éxito! (${formatTime(recordSeconds)})`, 'success', 'fa-download');
}

btnRecordAudio?.addEventListener('click', toggleAudioRecording);

// ==========================================
// 33. RADIO LO-FI & CYBERPUNK 24/7 (v4.0.0)
// ==========================================
const radioStations = [
  {
    id: 'lofi-groove',
    title: 'Groove Salad Lo-Fi',
    genre: 'Downtempo • Ambient • Lo-Fi Beats',
    desc: 'Un clásico mundial para estudiar, relajarse o trabajar. Bajos cálidos, texturas etéreas y beats orgánicos sin interrupciones.',
    streamUrl: 'https://ice5.somafm.com/groovesalad-128-mp3',
    icon: 'fa-mug-hot',
    bitrate: '128 kbps MP3',
    color: '#06b6d4'
  },
  {
    id: 'cyberpunk-defcon',
    title: 'DEF CON Cyberpunk Radio',
    genre: 'Dark Synth • Industrial • Hacker Beats',
    desc: 'Música de la conferencia hacker más grande del mundo. Sintetizadores modulares oscuros, ritmos industriales y cyberpunk.',
    streamUrl: 'https://ice5.somafm.com/defcon-128-mp3',
    icon: 'fa-terminal',
    bitrate: '128 kbps MP3',
    color: '#6366f1'
  },
  {
    id: 'vaporwave-dream',
    title: 'Vaporwaves Nostalgia',
    genre: 'Vaporwave • Synthwave • Retro 80s',
    desc: 'Paisajes sonoros nostálgicos de los 80s y 90s, cajas de ritmo reverberadas y texturas futuristas de ensueño.',
    streamUrl: 'https://ice5.somafm.com/vaporwaves-128-mp3',
    icon: 'fa-mountain-sun',
    bitrate: '128 kbps MP3',
    color: '#ec4899'
  },
  {
    id: 'beat-blender',
    title: 'Deep Beat Blender',
    genre: 'Deep-House • Chillhop • Nu-Disco',
    desc: 'Fusión electrónica de deep house elegante y chillout bailable. Ritmos continuos perfectos para sesiones largas.',
    streamUrl: 'https://ice5.somafm.com/beatblender-128-mp3',
    icon: 'fa-compact-disc',
    bitrate: '128 kbps MP3',
    color: '#10b981'
  },
  {
    id: 'secret-agent',
    title: 'Secret Agent Lounge',
    genre: 'Spy Lounge • Surf • Retro Cool',
    desc: 'Bandas sonoras de espías estilo años 60, groove de contrabajo cinematográfico y atmósfera vintage chic.',
    streamUrl: 'https://ice5.somafm.com/secretagent-128-mp3',
    icon: 'fa-glasses',
    bitrate: '128 kbps MP3',
    color: '#f59e0b'
  },
  {
    id: 'synphaera-space',
    title: 'Synphaera Space Ambient',
    genre: 'Space Ambient • Drone Cósmico',
    desc: 'Viaje interestelar de meditación profunda y atmósferas espaciales creadas por artistas de música electrónica ambiental.',
    streamUrl: 'https://ice5.somafm.com/synphaera-128-mp3',
    icon: 'fa-satellite',
    bitrate: '128 kbps MP3',
    color: '#38bdf8'
  }
];

function renderRadioStations() {
  const grid = document.getElementById('radio-station-grid');
  if (!grid) return;
  grid.innerHTML = '';

  radioStations.forEach((station) => {
    const isStationActive = activeRadioStation && activeRadioStation.id === station.id && isPlaying;
    const card = document.createElement('div');
    card.className = `radio-station-card ${isStationActive ? 'active-station' : ''}`;
    card.dataset.stationId = station.id;

    card.innerHTML = `
      <div class="radio-card-top">
        <div class="radio-icon-box" style="border-color:${station.color}55; background:linear-gradient(135deg, ${station.color}33, rgba(25,28,40,0.8));">
          <i class="fa-solid ${station.icon}" style="color:${station.color};"></i>
        </div>
        <div>
          <h4 class="radio-station-title">${station.title}</h4>
          <span class="radio-station-genre">${station.genre}</span>
        </div>
        <span class="radio-live-badge"><span class="pulse-dot"></span> EN VIVO</span>
      </div>
      <p class="radio-station-desc">${station.desc}</p>
      <div class="radio-card-footer">
        <span class="radio-bitrate"><i class="fa-solid fa-signal" style="margin-right:0.3rem;"></i> ${station.bitrate}</span>
        <button class="btn-action-pill btn-play-radio" style="font-size:0.75rem; padding:0.35rem 0.75rem; border-color:${station.color}; color:${station.color};">
          <i class="fa-solid ${isStationActive ? 'fa-pause' : 'fa-play'}"></i> <span>${isStationActive ? 'Pausar' : 'Sintonizar'}</span>
        </button>
      </div>
    `;

    card.addEventListener('click', () => {
      if (activeRadioStation && activeRadioStation.id === station.id) {
        if (isPlaying) {
          audio.pause();
          isPlaying = false;
          updatePlayPauseUI();
          renderRadioStations();
        } else {
          audio.play().then(() => {
            isPlaying = true;
            updatePlayPauseUI();
            renderRadioStations();
          });
        }
      } else {
        playRadioStation(station);
      }
    });

    grid.appendChild(card);
  });
}

function playRadioStation(station) {
  initAudioEngine();
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }

  activeRadioStation = station;
  audio.src = station.streamUrl;
  applyPlaybackRateAndPitch();

  audio.play().then(() => {
    isPlaying = true;
    updatePlayPauseUI();
    showToast(`📻 Sintonizando: ${station.title}`, 'info', 'fa-radio');
  }).catch(err => {
    console.error('Radio play error:', err);
    showToast('Error al conectar con la estación de radio.', 'warning', 'fa-triangle-exclamation');
  });

  playerTitle.innerText = station.title;
  playerArtist.innerText = `${station.genre} • Radio 24/7`;
  currentTimeEl.innerText = '🔴 EN VIVO';
  totalTimeEl.innerText = 'RADIO';
  progressFill.style.width = '100%';

  playerArt.src = `data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'><rect width='100' height='100' fill='%23191c28'/><circle cx='50' cy='50' r='32' fill='${encodeURIComponent(station.color)}'/><polygon points='44,38 64,50 44,62' fill='%23ffffff'/></svg>`;
  applyDynamicArtworkPalette(null);
  artGlow.style.opacity = '1';

  if ('mediaSession' in navigator) {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: station.title,
      artist: station.genre,
      album: 'DaVE Radio 24/7 En Vivo',
      artwork: [{ src: playerArt.src, sizes: '96x96', type: 'image/svg+xml' }]
    });
  }

  renderRadioStations();
  renderTrackList();
  renderPhoneTracksList();
}

// ==========================================
// 34. VISUALIZADOR 3D ESPECTRO NEÓN HI-FI (v4.0.0)
// ==========================================
const btnVisualizer3D = document.getElementById('btn-visualizer-3d');
const modalViz3D = document.getElementById('modal-visualizer3d');
const btnCloseViz3D = document.getElementById('btn-close-viz3d');
const btnViz3DMode = document.getElementById('btn-viz3d-mode');
const viz3DModeLabel = document.getElementById('viz3d-mode-label');
const btnViz3DFullscreen = document.getElementById('btn-viz3d-fullscreen');
const viz3DCanvas = document.getElementById('viz3d-canvas');
const viz3DTrackTitle = document.getElementById('viz3d-track-title');

let viz3DCtx = viz3DCanvas ? viz3DCanvas.getContext('2d') : null;
let viz3DStars = [];
let viz3DRotation = 0;

function init3DStars() {
  viz3DStars = [];
  for (let i = 0; i < 300; i++) {
    viz3DStars.push({
      x: (Math.random() - 0.5) * 2000,
      y: (Math.random() - 0.5) * 2000,
      z: Math.random() * 2000,
      pz: Math.random() * 2000
    });
  }
}

function resize3DCanvas() {
  if (!viz3DCanvas) return;
  viz3DCanvas.width = window.innerWidth;
  viz3DCanvas.height = window.innerHeight;
}

function open3DVisualizer() {
  if (!modalViz3D) return;
  modalViz3D.classList.remove('hidden');
  resize3DCanvas();
  init3DStars();

  if (viz3DTrackTitle) {
    if (activeRadioStation) {
      viz3DTrackTitle.innerText = `📻 ${activeRadioStation.title}`;
    } else if (currentIndex >= 0 && playlist[currentIndex]) {
      viz3DTrackTitle.innerText = `🎵 ${playlist[currentIndex].title} — ${playlist[currentIndex].artist}`;
    } else {
      viz3DTrackTitle.innerText = 'DaVE Player Studio';
    }
  }

  if (animFrame3D) cancelAnimationFrame(animFrame3D);
  loop3DVisualizer();
}

function close3DVisualizer() {
  if (!modalViz3D) return;
  modalViz3D.classList.add('hidden');
  if (animFrame3D) {
    cancelAnimationFrame(animFrame3D);
    animFrame3D = null;
  }
}

function loop3DVisualizer() {
  if (!modalViz3D || modalViz3D.classList.contains('hidden') || !viz3DCtx) return;
  animFrame3D = requestAnimationFrame(loop3DVisualizer);

  const w = viz3DCanvas.width;
  const h = viz3DCanvas.height;
  const cx = w / 2;
  const cy = h / 2;

  // Analyser audio data
  const freqData = new Uint8Array(analyserNode ? analyserNode.frequencyBinCount : 128);
  if (analyserNode && isPlaying) {
    analyserNode.getByteFrequencyData(freqData);
  }

  let bassSum = 0;
  for (let i = 0; i < 8; i++) bassSum += freqData[i];
  const bassNorm = (bassSum / 8) / 255;

  let trebleSum = 0;
  for (let i = 40; i < 70; i++) trebleSum += freqData[i];
  const trebleNorm = (trebleSum / 30) / 255;

  // Background clear with cinematic trail
  viz3DCtx.fillStyle = 'rgba(3, 4, 8, 0.28)';
  viz3DCtx.fillRect(0, 0, w, h);

  viz3DRotation += 0.008 + bassNorm * 0.025;

  if (viz3dMode === 'tunnel') {
    // 1. STARFIELD PARTICLES ZOOMING IN 3D
    const speed = 12 + bassNorm * 38;
    for (let i = 0; i < viz3DStars.length; i++) {
      const s = viz3DStars[i];
      s.z -= speed;
      if (s.z <= 10) {
        s.z = 2000;
        s.x = (Math.random() - 0.5) * 2000;
        s.y = (Math.random() - 0.5) * 2000;
      }
      const k = 450 / s.z;
      const px = cx + s.x * k;
      const py = cy + s.y * k;
      const size = Math.max(0.8, (1 - s.z / 2000) * 3.5 * (1 + trebleNorm));

      if (px >= 0 && px < w && py >= 0 && py < h) {
        const alpha = Math.min(1, Math.max(0.1, 1 - s.z / 2000));
        viz3DCtx.fillStyle = `rgba(147, 197, 253, ${alpha})`;
        viz3DCtx.beginPath();
        viz3DCtx.arc(px, py, size, 0, Math.PI * 2);
        viz3DCtx.fill();
      }
    }

    // 2. 3D VORTEX RINGS
    const ringCount = 14;
    for (let r = 0; r < ringCount; r++) {
      const ringZ = ((r * 140 - (Date.now() * 0.15) % 140) + 140) % (ringCount * 140) + 50;
      const scale = 500 / ringZ;
      const baseRadius = (160 + r * 15 + bassNorm * 120) * scale;

      const points = 36;
      viz3DCtx.beginPath();
      for (let p = 0; p <= points; p++) {
        const theta = (p / points) * Math.PI * 2 + viz3DRotation * (r % 2 === 0 ? 1 : -1);
        const freqVal = freqData[(p * 2 + r * 3) % freqData.length] / 255;
        const rad = baseRadius + freqVal * 60 * scale;
        const rx = cx + Math.cos(theta) * rad;
        const ry = cy + Math.sin(theta) * rad;

        if (p === 0) viz3DCtx.moveTo(rx, ry);
        else viz3DCtx.lineTo(rx, ry);
      }
      viz3DCtx.closePath();
      const hue = (r * 22 + Date.now() * 0.05) % 360;
      viz3DCtx.strokeStyle = `hsla(${hue}, 95%, 60%, ${Math.min(1, 1.2 - ringZ / 1500)})`;
      viz3DCtx.lineWidth = Math.max(1.2, 3.5 * scale * (1 + bassNorm));
      viz3DCtx.shadowBlur = 12 * scale;
      viz3DCtx.shadowColor = `hsl(${hue}, 100%, 65%)`;
      viz3DCtx.stroke();
    }
  } else {
    // 2. CYBER QUANTUM SPHERE 3D
    const sphereRadius = Math.min(cx, cy) * (0.45 + bassNorm * 0.22);
    const rows = 18;
    const cols = 28;

    for (let i = 0; i < rows; i++) {
      const lat = (i / (rows - 1)) * Math.PI - Math.PI / 2;
      for (let j = 0; j < cols; j++) {
        const lon = (j / cols) * Math.PI * 2 + viz3DRotation;
        const freqIdx = (i * cols + j) % freqData.length;
        const amp = (freqData[freqIdx] / 255) * 70;
        const currentR = sphereRadius + amp;

        // Spherical to Cartesian
        let x = currentR * Math.cos(lat) * Math.cos(lon);
        let y = currentR * Math.sin(lat);
        let z = currentR * Math.cos(lat) * Math.sin(lon);

        // Tilt sphere
        const tilt = 0.45;
        const yTilted = y * Math.cos(tilt) - z * Math.sin(tilt);
        const zTilted = y * Math.sin(tilt) + z * Math.cos(tilt);

        const persp = 600 / (zTilted + 650);
        const sx = cx + x * persp;
        const sy = cy + yTilted * persp;
        const dotSize = Math.max(1, 3.2 * persp * (1 + amp / 40));

        const hue = (lat * 60 + lon * 40 + Date.now() * 0.04) % 360;
        viz3DCtx.fillStyle = `hsl(${hue}, 100%, 65%)`;
        viz3DCtx.beginPath();
        viz3DCtx.arc(sx, sy, dotSize, 0, Math.PI * 2);
        viz3DCtx.fill();
      }
    }
  }
}

btnVisualizer3D?.addEventListener('click', open3DVisualizer);
btnCloseViz3D?.addEventListener('click', close3DVisualizer);

btnViz3DMode?.addEventListener('click', () => {
  viz3dMode = viz3dMode === 'tunnel' ? 'sphere' : 'tunnel';
  if (viz3DModeLabel) {
    viz3DModeLabel.innerText = viz3dMode === 'tunnel' ? 'Túnel Neón' : 'Esfera Cuántica';
  }
  showToast(`Modo 3D: ${viz3dMode === 'tunnel' ? 'Túnel Neón 3D' : 'Esfera Cuántica 3D'}`, 'info', 'fa-cube');
});

btnViz3DFullscreen?.addEventListener('click', () => {
  if (!document.fullscreenElement) {
    modalViz3D?.requestFullscreen().catch(e => console.log(e));
  } else {
    document.exitFullscreen().catch(e => console.log(e));
  }
});

window.addEventListener('resize', () => {
  if (modalViz3D && !modalViz3D.classList.contains('hidden')) {
    resize3DCanvas();
  }
});

// ==========================================
// 35. DAVE DJ AUTOMIX ENGINE (v4.0.0)
// ==========================================
const btnToggleDJ = document.getElementById('btn-toggle-dj');

function toggleDJAutomix() {
  isDJAutomix = !isDJAutomix;
  localStorage.setItem('dave_dj_automix', isDJAutomix);
  btnToggleDJ?.classList.toggle('dj-active', isDJAutomix);
  showToast(
    isDJAutomix ? '🎧 DaVE DJ Automix Activado: Mezcla continua sin silencios' : '🎧 DaVE DJ Automix Desactivado',
    isDJAutomix ? 'success' : 'info',
    'fa-compact-disc'
  );
  if (typeof updateActiveFXBadge === 'function') updateActiveFXBadge();
}

function triggerDJCrossfade() {
  if (isDJTransitioning || playlist.length <= 1) return;
  isDJTransitioning = true;

  showToast('🎧 DaVE DJ: Mezclando con la siguiente canción...', 'info', 'fa-compact-disc');

  if (masterGain && audioCtx && audioCtx.state === 'running') {
    try {
      masterGain.gain.cancelScheduledValues(audioCtx.currentTime);
      masterGain.gain.setValueAtTime(masterGain.gain.value, audioCtx.currentTime);
      masterGain.gain.linearRampToValueAtTime(0.08, audioCtx.currentTime + 3.8);
    } catch (e) {}
  }

  setTimeout(() => {
    if (isShuffle) {
      const nextIdx = Math.floor(Math.random() * playlist.length);
      playTrack(nextIdx);
    } else {
      const nextIdx = (currentIndex + 1) % playlist.length;
      playTrack(nextIdx);
    }
  }, 3600);
}

btnToggleDJ?.addEventListener('click', toggleDJAutomix);

// ==========================================
// 37. A-B LOOPER (REPETIDOR DE SEGMENTOS v4.1.0)
// ==========================================
const btnABLoop = document.getElementById('btn-ab-loop');
const abLoopLabel = document.getElementById('ab-loop-label');
const scrubberABFill = document.getElementById('scrubber-ab-fill');

function toggleABLoop() {
  if (!isPlaying && !audio.src) {
    showToast('Reproduce una canción para activar el bucle A-B.', 'warning', 'fa-play');
    return;
  }

  if (abLoopA === null) {
    // Stage 1: Set Point A
    abLoopA = audio.currentTime;
    abLoopB = null;
    isABLooping = false;
    btnABLoop?.classList.add('ab-active');
    if (abLoopLabel) abLoopLabel.innerText = 'A->?';
    if (scrubberABFill) scrubberABFill.classList.add('hidden');
    showToast(`Punto [A] fijado en ${formatTime(abLoopA)}. Haz clic de nuevo para fijar [B].`, 'info', 'fa-repeat');
  } else if (abLoopB === null) {
    // Stage 2: Set Point B & Activate Loop
    let targetB = audio.currentTime;
    if (targetB <= abLoopA + 1.5) {
      targetB = abLoopA + 3; // mínimo 3 segundos de bucle
    }
    abLoopB = targetB;
    isABLooping = true;
    if (abLoopLabel) abLoopLabel.innerText = 'A-B 🔁';

    // Position highlight fill on scrubber
    if (scrubberABFill && audio.duration && isFinite(audio.duration)) {
      const leftPercent = (abLoopA / audio.duration) * 100;
      const widthPercent = ((abLoopB - abLoopA) / audio.duration) * 100;
      scrubberABFill.style.left = `${leftPercent}%`;
      scrubberABFill.style.width = `${widthPercent}%`;
      scrubberABFill.classList.remove('hidden');
    }

    showToast(`Bucle A-B activado: ${formatTime(abLoopA)} ➔ ${formatTime(abLoopB)}`, 'success', 'fa-repeat');
  } else {
    // Stage 3: Clear Loop
    clearABLoop();
    showToast('Bucle A-B desactivado.', 'info', 'fa-xmark');
  }
  if (typeof updateActiveFXBadge === 'function') updateActiveFXBadge();
}

function clearABLoop() {
  abLoopA = null;
  abLoopB = null;
  isABLooping = false;
  btnABLoop?.classList.remove('ab-active');
  if (abLoopLabel) abLoopLabel.innerText = 'Bucle A-B';
  if (scrubberABFill) scrubberABFill.classList.add('hidden');
  if (typeof updateActiveFXBadge === 'function') updateActiveFXBadge();
}

btnABLoop?.addEventListener('click', toggleABLoop);

// ==========================================
// 38. SPEED & PITCH MULTI-GEAR SELECTOR (v4.1.0)
// ==========================================
const btnSpeedSelector = document.getElementById('btn-speed-selector');
const modalSpeed = document.getElementById('modal-speed');
const btnCloseSpeed = document.getElementById('btn-close-speed');
const speedLabel = document.getElementById('speed-label');
const speedCustomSlider = document.getElementById('speed-custom-slider');
const speedCustomVal = document.getElementById('speed-custom-val');
const btnSpeedReset = document.getElementById('btn-speed-reset');

function setPlaybackSpeed(speed) {
  currentPlaybackSpeed = parseFloat(speed);
  applyPlaybackRateAndPitch();

  if (speedLabel) speedLabel.innerText = `${currentPlaybackSpeed}x`;
  if (speedCustomSlider) speedCustomSlider.value = currentPlaybackSpeed;
  if (speedCustomVal) speedCustomVal.innerText = `${currentPlaybackSpeed.toFixed(2)}x`;

  document.querySelectorAll('.speed-chip').forEach(chip => {
    chip.classList.toggle('active', parseFloat(chip.dataset.speed) === currentPlaybackSpeed);
  });

  let modeName = `${currentPlaybackSpeed}x`;
  if (currentPlaybackSpeed === 0.85) modeName = '0.85x (Slowed)';
  else if (currentPlaybackSpeed === 1.25) modeName = '1.25x (Nightcore)';
  else if (currentPlaybackSpeed === 1.5) modeName = '1.5x (Speed Up)';
  showToast(`Velocidad ajustada: ${modeName}`, 'info', 'fa-gauge-high');
}

btnSpeedSelector?.addEventListener('click', () => {
  modalSpeed?.classList.remove('hidden');
});
btnCloseSpeed?.addEventListener('click', () => {
  modalSpeed?.classList.add('hidden');
});

document.querySelectorAll('.speed-chip').forEach(chip => {
  chip.addEventListener('click', () => {
    setPlaybackSpeed(chip.dataset.speed);
  });
});

speedCustomSlider?.addEventListener('input', (e) => {
  const val = parseFloat(e.target.value);
  if (speedCustomVal) speedCustomVal.innerText = `${val.toFixed(2)}x`;
  setPlaybackSpeed(val);
});

btnSpeedReset?.addEventListener('click', () => {
  setPlaybackSpeed(1.0);
});

// ==========================================
// 39. PROCEDURAL AMBIENCE SOUNDSCAPES (v4.1.0)
// ==========================================
const btnOpenAmbience = document.getElementById('btn-open-ambience');
const modalAmbience = document.getElementById('modal-ambience');
const btnCloseAmbience = document.getElementById('btn-close-ambience');
const ambienceVolumeSlider = document.getElementById('ambience-volume-slider');
const ambienceVolumeVal = document.getElementById('ambience-volume-val');
const btnAmbienceStop = document.getElementById('btn-ambience-stop');

function stopAmbienceSoundscapes() {
  ambienceNodes.forEach(node => {
    try {
      if (node.stop) node.stop();
      if (node.disconnect) node.disconnect();
    } catch (e) {}
  });
  ambienceNodes = [];
  activeAmbienceSound = null;
  document.querySelectorAll('.ambience-card').forEach(c => c.classList.remove('active'));
}

function startAmbienceSoundscape(type) {
  initAudioEngine();
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }

  stopAmbienceSoundscapes();
  activeAmbienceSound = type;

  // Setup Ambience Gain Node
  if (!ambienceGainNode) {
    ambienceGainNode = audioCtx.createGain();
    ambienceGainNode.connect(audioCtx.destination);
    if (audioRecorderDestination) {
      ambienceGainNode.connect(audioRecorderDestination);
    }
  }
  ambienceGainNode.gain.value = ambienceVolume;

  const sampleRate = audioCtx.sampleRate;
  const bufferSize = sampleRate * 5;
  const noiseBuffer = audioCtx.createBuffer(1, bufferSize, sampleRate);
  const output = noiseBuffer.getChannelData(0);

  // Generate pink / rain noise
  let b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0, b6 = 0;
  for (let i = 0; i < bufferSize; i++) {
    const white = Math.random() * 2 - 1;
    b0 = 0.99886 * b0 + white * 0.0555179;
    b1 = 0.99332 * b1 + white * 0.0750759;
    b2 = 0.96900 * b2 + white * 0.1538520;
    b3 = 0.86650 * b3 + white * 0.3104856;
    b4 = 0.55000 * b4 + white * 0.5329522;
    b5 = -0.7616 * b5 - white * 0.0168980;
    output[i] = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.11;
    b6 = white * 0.115926;
  }

  const whiteNoiseSource = audioCtx.createBufferSource();
  whiteNoiseSource.buffer = noiseBuffer;
  whiteNoiseSource.loop = true;

  if (type === 'rain') {
    // Rain: Lowpass + Highpass
    const lp = audioCtx.createBiquadFilter();
    lp.type = 'lowpass';
    lp.frequency.value = 1400;

    const hp = audioCtx.createBiquadFilter();
    hp.type = 'highpass';
    hp.frequency.value = 350;

    whiteNoiseSource.connect(lp);
    lp.connect(hp);
    hp.connect(ambienceGainNode);

    ambienceNodes.push(whiteNoiseSource, lp, hp);
    whiteNoiseSource.start();
    showToast('🌧️ Atmósfera de Lluvia Suave activada', 'info', 'fa-cloud-rain');
  } else if (type === 'waves') {
    // Ocean Waves: Deep lowpass + 0.09Hz sine wave swelling LFO
    const lp = audioCtx.createBiquadFilter();
    lp.type = 'lowpass';
    lp.frequency.value = 650;

    const waveGain = audioCtx.createGain();
    waveGain.gain.value = 0.5;

    const lfo = audioCtx.createOscillator();
    lfo.type = 'sine';
    lfo.frequency.value = 0.09;

    const lfoGain = audioCtx.createGain();
    lfoGain.gain.value = 0.45;

    lfo.connect(lfoGain);
    lfoGain.connect(waveGain.gain);

    whiteNoiseSource.connect(lp);
    lp.connect(waveGain);
    waveGain.connect(ambienceGainNode);

    ambienceNodes.push(whiteNoiseSource, lp, waveGain, lfo, lfoGain);
    lfo.start();
    whiteNoiseSource.start();
    showToast('🌊 Atmósfera de Marea Marina activada', 'info', 'fa-water');
  } else if (type === 'fire') {
    // Campfire: Low warm rumble
    const lp = audioCtx.createBiquadFilter();
    lp.type = 'lowpass';
    lp.frequency.value = 450;

    const fireGain = audioCtx.createGain();
    fireGain.gain.value = 0.7;

    whiteNoiseSource.connect(lp);
    lp.connect(fireGain);
    fireGain.connect(ambienceGainNode);

    ambienceNodes.push(whiteNoiseSource, lp, fireGain);
    whiteNoiseSource.start();
    showToast('🪵 Atmósfera de Hoguera Cálida activada', 'info', 'fa-fire');
  } else if (type === 'coffee') {
    // Coffee shop ambience: Bandpass filter
    const bp = audioCtx.createBiquadFilter();
    bp.type = 'bandpass';
    bp.frequency.value = 900;
    bp.Q.value = 0.8;

    whiteNoiseSource.connect(bp);
    bp.connect(ambienceGainNode);

    ambienceNodes.push(whiteNoiseSource, bp);
    whiteNoiseSource.start();
    showToast('☕ Atmósfera de Cafetería Lo-Fi activada', 'info', 'fa-mug-saucer');
  }

  document.querySelectorAll('.ambience-card').forEach(c => {
    c.classList.toggle('active', c.dataset.sound === type);
  });
}

btnOpenAmbience?.addEventListener('click', () => modalAmbience?.classList.remove('hidden'));
btnCloseAmbience?.addEventListener('click', () => modalAmbience?.classList.add('hidden'));

document.querySelectorAll('.ambience-card').forEach(card => {
  card.addEventListener('click', () => {
    const sound = card.dataset.sound;
    if (activeAmbienceSound === sound) {
      stopAmbienceSoundscapes();
      showToast('Atmósfera pausada.', 'info', 'fa-pause');
    } else {
      startAmbienceSoundscape(sound);
    }
  });
});

ambienceVolumeSlider?.addEventListener('input', (e) => {
  const val = parseInt(e.target.value, 10);
  ambienceVolume = val / 100;
  if (ambienceGainNode) {
    ambienceGainNode.gain.value = ambienceVolume;
  }
  if (ambienceVolumeVal) ambienceVolumeVal.innerText = `${val}%`;
});

btnAmbienceStop?.addEventListener('click', () => {
  stopAmbienceSoundscapes();
  showToast('Atmósfera de sonido detenida.', 'info', 'fa-stop');
});

// ==========================================
// 40. SPOTLIGHT COMMAND PALETTE (CTRL+K v4.1.0)
// ==========================================
const btnOpenPalette = document.getElementById('btn-open-palette');
const modalPalette = document.getElementById('modal-command-palette');
const btnClosePalette = document.getElementById('btn-close-palette');
const paletteSearchInput = document.getElementById('palette-search-input');
const paletteResultsList = document.getElementById('palette-results-list');

let paletteActiveIndex = 0;
let paletteItems = [];

const quickPaletteActions = [
  {
    type: 'action',
    title: '🎧 Audio 8D Órbita 360°',
    desc: 'Activar o desactivar paneo binaural envolvente',
    icon: 'fa-headphones',
    action: () => toggle8DAudio()
  },
  {
    type: 'action',
    title: '🌌 Modo Slowed + Reverb',
    desc: 'Bajar velocidad a 0.85x y aplicar acústica celestial',
    icon: 'fa-hourglass-start',
    action: () => toggleSlowedReverb()
  },
  {
    type: 'action',
    title: '🎙️ Grabar Audio Procesado',
    desc: 'Iniciar o detener grabación con efectos aplicados en vivo',
    icon: 'fa-circle-dot',
    action: () => toggleAudioRecording()
  },
  {
    type: 'action',
    title: '🎛️ Abrir Ecualizador 10-Band',
    desc: 'Ajustar bandas de frecuencia y presets de audio',
    icon: 'fa-chart-simple',
    action: () => switchTab('equalizer')
  },
  {
    type: 'action',
    title: '🎤 Karaoke & Letras Sincronizadas',
    desc: 'Ver lyrics en vivo de la canción actual',
    icon: 'fa-microphone-lines',
    action: () => switchTab('lyrics')
  },
  {
    type: 'action',
    title: '🎚️ AI Stem Mixer Studio',
    desc: 'Aislar voces, baterías, bajos o melodías en tiempo real',
    icon: 'fa-sliders',
    action: () => switchTab('stem-mixer')
  },
  {
    type: 'action',
    title: '📻 Abrir Radio Lo-Fi & Cyberpunk 24/7',
    desc: 'Escuchar estaciones de streaming continuo en vivo',
    icon: 'fa-radio',
    action: () => switchTab('radio')
  },
  {
    type: 'action',
    title: '🧊 Visualizador 3D Espectro Neón',
    desc: 'Abrir túnel 3D reactivo a pantalla completa',
    icon: 'fa-cube',
    action: () => open3DVisualizer()
  },
  {
    type: 'action',
    title: '🌙 Temporizador de Apagado (Sleep Timer)',
    desc: 'Programar apagado automático de la música',
    icon: 'fa-moon',
    action: () => openSleepTimerModal()
  },
  {
    type: 'action',
    title: '🌧️ Activar Sonido de Lluvia Suave',
    desc: 'Generar atmósfera relajante de lluvia de fondo',
    icon: 'fa-cloud-rain',
    action: () => startAmbienceSoundscape('rain')
  },
  {
    type: 'action',
    title: '🎨 Cambiar Tema Neón',
    desc: 'Abrir paleta de temas visuales',
    icon: 'fa-palette',
    action: () => modalTheme?.classList.remove('hidden')
  },
  {
    type: 'action',
    title: '📊 Ver Mis Estadísticas de Música',
    desc: 'Resumen personal de horas, reproducciones y top canciones',
    icon: 'fa-chart-pie',
    action: () => renderStatsModal()
  }
];

function openCommandPalette() {
  if (!modalPalette) return;
  modalPalette.classList.remove('hidden');
  if (paletteSearchInput) {
    paletteSearchInput.value = '';
    paletteSearchInput.focus();
  }
  renderPaletteResults('');
}

function closeCommandPalette() {
  if (!modalPalette) return;
  modalPalette.classList.add('hidden');
}

function renderPaletteResults(query) {
  if (!paletteResultsList) return;
  paletteResultsList.innerHTML = '';
  paletteItems = [];
  paletteActiveIndex = 0;

  const q = query.trim().toLowerCase();

  // 1. Matching Actions
  const matchedActions = quickPaletteActions.filter(a =>
    a.title.toLowerCase().includes(q) || a.desc.toLowerCase().includes(q)
  );

  matchedActions.forEach(a => {
    paletteItems.push({
      title: a.title,
      subtitle: a.desc,
      icon: a.icon,
      handler: a.action
    });
  });

  // 2. Matching Cloud Tracks
  const allTracks = playlist.length > 0 ? playlist : realPhoneTracks;
  const matchedTracks = allTracks.filter(t =>
    t.title.toLowerCase().includes(q) || t.artist.toLowerCase().includes(q)
  ).slice(0, 8);

  matchedTracks.forEach(t => {
    paletteItems.push({
      title: `🎵 ${t.title}`,
      subtitle: `${t.artist} • ${t.album}`,
      icon: 'fa-play',
      handler: () => {
        const idx = playlist.findIndex(p => p.title === t.title);
        if (idx >= 0) playTrack(idx);
        else {
          playlist = [t, ...playlist];
          renderTrackList();
          playTrack(0);
        }
      }
    });
  });

  // 3. Matching Radio Stations
  const matchedRadios = radioStations.filter(r =>
    r.title.toLowerCase().includes(q) || r.genre.toLowerCase().includes(q)
  );
  matchedRadios.forEach(r => {
    paletteItems.push({
      title: `📻 ${r.title}`,
      subtitle: `${r.genre} • En Vivo`,
      icon: 'fa-radio',
      handler: () => playRadioStation(r)
    });
  });

  if (paletteItems.length === 0) {
    paletteResultsList.innerHTML = `
      <div style="text-align:center; padding:1.5rem; color:var(--text-muted); font-size:0.85rem;">
        No se encontraron canciones, radios o comandos para "<strong>${escapeHtml(query)}</strong>".
      </div>
    `;
    return;
  }

  paletteItems.forEach((item, index) => {
    const div = document.createElement('div');
    div.className = `command-palette-item ${index === 0 ? 'active' : ''}`;
    div.innerHTML = `
      <i class="fa-solid ${item.icon}"></i>
      <div style="flex:1; overflow:hidden;">
        <strong>${item.title}</strong>
        <span>${item.subtitle}</span>
      </div>
      <i class="fa-solid fa-arrow-turn-down" style="font-size:0.75rem; color:var(--text-dim); transform:rotate(90deg);"></i>
    `;

    div.addEventListener('click', () => {
      closeCommandPalette();
      item.handler();
    });

    paletteResultsList.appendChild(div);
  });
}

function updatePaletteActiveItem() {
  const domItems = paletteResultsList.querySelectorAll('.command-palette-item');
  domItems.forEach((el, idx) => {
    el.classList.toggle('active', idx === paletteActiveIndex);
    if (idx === paletteActiveIndex) {
      el.scrollIntoView({ block: 'nearest' });
    }
  });
}

paletteSearchInput?.addEventListener('input', (e) => {
  renderPaletteResults(e.target.value);
});

paletteSearchInput?.addEventListener('keydown', (e) => {
  if (paletteItems.length === 0) return;

  if (e.key === 'ArrowDown') {
    e.preventDefault();
    paletteActiveIndex = (paletteActiveIndex + 1) % paletteItems.length;
    updatePaletteActiveItem();
  } else if (e.key === 'ArrowUp') {
    e.preventDefault();
    paletteActiveIndex = (paletteActiveIndex - 1 + paletteItems.length) % paletteItems.length;
    updatePaletteActiveItem();
  } else if (e.key === 'Enter') {
    e.preventDefault();
    if (paletteItems[paletteActiveIndex]) {
      closeCommandPalette();
      paletteItems[paletteActiveIndex].handler();
    }
  }
});

btnOpenPalette?.addEventListener('click', openCommandPalette);
btnClosePalette?.addEventListener('click', closeCommandPalette);

// Global Keyboard Shortcut: Ctrl + K or Cmd + K
window.addEventListener('keydown', (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault();
    if (modalPalette?.classList.contains('hidden')) {
      openCommandPalette();
    } else {
      closeCommandPalette();
    }
  } else if (e.key === 'Escape') {
    if (!modalPalette?.classList.contains('hidden')) {
      closeCommandPalette();
    }
  }
});

// ==========================================
// 42. CALOR A TUBOS VINTAGE & SUB-BASS EXCITER (v4.2.0 APEX)
// ==========================================
function makeTubeCurve(k = 2) {
  const n_samples = 44100;
  const curve = new Float32Array(n_samples);
  const deg = Math.PI / 180;
  for (let i = 0; i < n_samples; ++i) {
    const x = (i * 2) / n_samples - 1;
    curve[i] = ((3 + k) * x * 20 * deg) / (Math.PI + k * Math.abs(x));
  }
  return curve;
}

function setTubeWarmth(mode = 'off', silent = false) {
  currentTubeMode = mode;
  localStorage.setItem('dave_tube_warmth', mode);
  const btnToggle = document.getElementById('btn-toggle-tubewarmth');
  const exciterStatus = document.getElementById('tube-exciter-status');

  document.querySelectorAll('.tube-chip').forEach(chip => {
    chip.classList.toggle('active', chip.dataset.tube === mode);
  });

  if (!tubeSaturationNode || !bassExciterFilter) return;

  if (mode === 'off') {
    tubeSaturationNode.curve = null;
    bassExciterFilter.gain.value = 0;
    btnToggle?.classList.remove('tube-active');
    if (exciterStatus) exciterStatus.innerText = 'Inactivo';
    if (!silent) showToast('Calor a Tubos: Bypass (Desactivado)', 'info', 'fa-fire-flame-curved');
  } else if (mode === 'warm') {
    tubeSaturationNode.curve = makeTubeCurve(2);
    bassExciterFilter.gain.value = 3.5;
    btnToggle?.classList.add('tube-active');
    if (exciterStatus) exciterStatus.innerText = '+3.5 dB @ 55Hz (Cálido Vinilo)';
    if (!silent) showToast('Calor a Tubos: Cálido Vinilo (+2dB Sat)', 'success', 'fa-fire-flame-curved');
  } else if (mode === 'punch') {
    tubeSaturationNode.curve = makeTubeCurve(5);
    bassExciterFilter.gain.value = 6.0;
    btnToggle?.classList.add('tube-active');
    if (exciterStatus) exciterStatus.innerText = '+6.0 dB @ 55Hz (Punch Analógico)';
    if (!silent) showToast('Calor a Tubos: Punch Analógico (+5dB Sat)', 'success', 'fa-fire-flame-curved');
  } else if (mode === 'beast') {
    tubeSaturationNode.curve = makeTubeCurve(10);
    bassExciterFilter.gain.value = 9.0;
    btnToggle?.classList.add('tube-active');
    if (exciterStatus) exciterStatus.innerText = '+9.0 dB @ 55Hz (Bestia a Válvulas)';
    if (!silent) showToast('Calor a Tubos: ¡Bestia a Válvulas al Máximo!', 'warning', 'fa-fire-flame-curved');
  }
  if (typeof updateActiveFXBadge === 'function') updateActiveFXBadge();
}

const modalTubeWarmth = document.getElementById('modal-tubewarmth');
const btnOpenTube = document.getElementById('btn-toggle-tubewarmth');
const btnCloseTube = document.getElementById('btn-close-tubewarmth');

btnOpenTube?.addEventListener('click', () => {
  initAudioEngine();
  modalTubeWarmth?.classList.remove('hidden');
});
btnCloseTube?.addEventListener('click', () => {
  modalTubeWarmth?.classList.add('hidden');
});

document.querySelectorAll('.tube-chip').forEach(chip => {
  chip.addEventListener('click', () => {
    initAudioEngine();
    const mode = chip.dataset.tube || 'off';
    setTubeWarmth(mode);
  });
});

// ==========================================
// 43. RECORTADOR & CREADOR DE RINGTONES (v4.2.0 APEX)
// ==========================================
const modalRingtone = document.getElementById('modal-ringtone-maker');
const btnOpenRingtone = document.getElementById('btn-open-ringtone');
const btnCloseRingtone = document.getElementById('btn-close-ringtone');
const btnRtCurrent = document.getElementById('btn-rt-current');
const btnRtPreview = document.getElementById('btn-rt-preview');
const btnRtDownload = document.getElementById('btn-rt-download');
const rtStartInput = document.getElementById('rt-start-sec');
const rtDurationSel = document.getElementById('rt-duration-sel');

function openRingtoneModal() {
  if (currentIndex < 0 || !playlist[currentIndex]) {
    showNotification('Selecciona o reproduce una canción primero para crear tu ringtone.', 'info');
    return;
  }
  const track = playlist[currentIndex];
  const tTitle = document.getElementById('rt-song-title');
  const tArtist = document.getElementById('rt-song-artist');
  if (tTitle) tTitle.innerText = track.title;
  if (tArtist) tArtist.innerText = track.artist;
  if (rtStartInput) rtStartInput.value = Math.floor(audio.currentTime || 30);
  modalRingtone?.classList.remove('hidden');
}

btnOpenRingtone?.addEventListener('click', openRingtoneModal);
btnCloseRingtone?.addEventListener('click', () => {
  modalRingtone?.classList.add('hidden');
  if (activeRingtoneAudio) {
    activeRingtoneAudio.pause();
    activeRingtoneAudio = null;
    if (btnRtPreview) btnRtPreview.innerHTML = '<i class="fa-solid fa-play"></i> Escuchar Tono';
  }
});

btnRtCurrent?.addEventListener('click', () => {
  if (rtStartInput) {
    rtStartInput.value = Math.floor(audio.currentTime || 0);
    showToast(`Punto de inicio establecido en ${formatTime(audio.currentTime || 0)}`, 'info', 'fa-clock');
  }
});

btnRtPreview?.addEventListener('click', () => {
  if (activeRingtoneAudio) {
    activeRingtoneAudio.pause();
    activeRingtoneAudio = null;
    btnRtPreview.innerHTML = '<i class="fa-solid fa-play"></i> Escuchar Tono';
    return;
  }
  const track = playlist[currentIndex];
  if (!track) return;
  const src = track.streamUrl || track.demoUrl || (track.fileObj ? URL.createObjectURL(track.fileObj) : audio.src);
  if (!src) return;

  const startSec = Math.max(0, parseFloat(rtStartInput?.value) || 0);
  const durSec = parseFloat(rtDurationSel?.value) || 25;

  activeRingtoneAudio = new Audio(src);
  activeRingtoneAudio.currentTime = startSec;
  btnRtPreview.innerHTML = '<i class="fa-solid fa-stop"></i> Detener Preview';

  activeRingtoneAudio.play().then(() => {
    setTimeout(() => {
      if (activeRingtoneAudio) {
        activeRingtoneAudio.pause();
        activeRingtoneAudio = null;
        if (btnRtPreview) btnRtPreview.innerHTML = '<i class="fa-solid fa-play"></i> Escuchar Tono';
      }
    }, durSec * 1000);
  }).catch(e => {
    console.warn(e);
    btnRtPreview.innerHTML = '<i class="fa-solid fa-play"></i> Escuchar Tono';
  });
});

function audioBufferToWavBlob(buffer) {
  const numOfChan = buffer.numberOfChannels;
  const length = buffer.length * numOfChan * 2 + 44;
  const out = new DataView(new ArrayBuffer(length));
  const channels = [];
  let sample = 0;
  let offset = 0;
  let pos = 0;

  function setUint16(data) { out.setUint16(pos, data, true); pos += 2; }
  function setUint32(data) { out.setUint32(pos, data, true); pos += 4; }

  setUint32(0x46464952); // "RIFF"
  setUint32(length - 8);
  setUint32(0x45564157); // "WAVE"
  setUint32(0x20746d66); // "fmt "
  setUint32(16);
  setUint16(1); // PCM
  setUint16(numOfChan);
  setUint32(buffer.sampleRate);
  setUint32(buffer.sampleRate * 2 * numOfChan);
  setUint16(numOfChan * 2);
  setUint16(16);
  setUint32(0x61746164); // "data"
  setUint32(length - pos - 4);

  for (let i = 0; i < buffer.numberOfChannels; i++) {
    channels.push(buffer.getChannelData(i));
  }

  while (offset < buffer.length) {
    for (let i = 0; i < numOfChan; i++) {
      sample = Math.max(-1, Math.min(1, channels[i][offset]));
      sample = (0.5 + sample < 0 ? sample * 32768 : sample * 32767) | 0;
      out.setInt16(pos, sample, true);
      pos += 2;
    }
    offset++;
  }

  return new Blob([out.buffer], { type: 'audio/wav' });
}

btnRtDownload?.addEventListener('click', async () => {
  const track = playlist[currentIndex];
  if (!track) return;
  const src = track.streamUrl || track.demoUrl || (track.fileObj ? URL.createObjectURL(track.fileObj) : audio.src);
  if (!src) return;

  const startSec = Math.max(0, parseFloat(rtStartInput?.value) || 0);
  const durSec = parseFloat(rtDurationSel?.value) || 25;

  btnRtDownload.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Recortando...';

  try {
    const resp = await fetch(src);
    const arrayBuf = await resp.arrayBuffer();
    const tempCtx = new (window.AudioContext || window.webkitAudioContext)();
    const audioBuffer = await tempCtx.decodeAudioData(arrayBuf);

    const sampleRate = audioBuffer.sampleRate;
    const startSample = Math.floor(startSec * sampleRate);
    const endSample = Math.min(audioBuffer.length, Math.floor((startSec + durSec) * sampleRate));
    const lengthSamples = endSample - startSample;

    if (lengthSamples <= 0) throw new Error('Rango de tiempo fuera de límites');

    const offlineCtx = new OfflineAudioContext(audioBuffer.numberOfChannels, lengthSamples, sampleRate);
    const bufferSource = offlineCtx.createBufferSource();
    bufferSource.buffer = audioBuffer;
    bufferSource.connect(offlineCtx.destination);
    bufferSource.start(0, startSec, durSec);

    const rendered = await offlineCtx.startRendering();
    const wavBlob = audioBufferToWavBlob(rendered);

    const url = URL.createObjectURL(wavBlob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = url;
    const cleanName = (track.title || 'Ringtone').replace(/[^\w\s-]/gi, '').trim().replace(/\s+/g, '_');
    a.download = `${cleanName}_Ringtone.wav`;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      URL.revokeObjectURL(url);
      a.remove();
    }, 2500);

    showNotification(`¡Ringtone "${track.title}" descargado con éxito!`, 'success');
  } catch (err) {
    console.warn('Fallback a descarga directa de audio:', err);
    const a = document.createElement('a');
    a.href = src;
    a.download = `${track.title || 'Ringtone'}.mp3`;
    a.target = '_blank';
    a.click();
    showNotification('Descargando pista completa como tono.', 'info');
  } finally {
    btnRtDownload.innerHTML = '<i class="fa-solid fa-download"></i> Descargar Ringtone';
  }
});

// ==========================================
// 44. LETRAS EN PANTALLA COMPLETA CINEMATOGRÁFICAS (v4.2.0 APEX)
// ==========================================
const overlayFullscreenLyrics = document.getElementById('overlay-fullscreen-lyrics');
const btnFullscreenLyrics = document.getElementById('btn-fullscreen-lyrics');
const btnCloseFsLyrics = document.getElementById('btn-close-fs-lyrics');

function openFullscreenLyrics() {
  if (!overlayFullscreenLyrics) return;
  const track = (currentIndex >= 0 && playlist[currentIndex]) ? playlist[currentIndex] : {
    title: 'DaVE Player',
    artist: 'Música en vivo',
    coverUrl: ''
  };

  const titleEl = document.getElementById('fs-lyrics-title');
  const artistEl = document.getElementById('fs-lyrics-artist');
  const artEl = document.getElementById('fs-lyrics-art');
  const bgEl = document.getElementById('fs-lyrics-bg');

  if (titleEl) titleEl.innerText = track.title;
  if (artistEl) artistEl.innerText = track.artist;
  if (artEl) artEl.src = track.coverUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="54" height="54"><rect width="54" height="54" fill="%23191c28"/></svg>';
  if (bgEl && track.coverUrl) bgEl.style.backgroundImage = `url("${track.coverUrl}")`;

  overlayFullscreenLyrics.classList.remove('hidden');
  renderLyrics(audio.currentTime || 0);
}

function closeFullscreenLyrics() {
  overlayFullscreenLyrics?.classList.add('hidden');
}

btnFullscreenLyrics?.addEventListener('click', openFullscreenLyrics);
btnCloseFsLyrics?.addEventListener('click', closeFullscreenLyrics);

// ==========================================
// 45. RESPALDO Y RESTAURACIÓN DE BIBLIOTECA JSON (v4.2.0 APEX)
// ==========================================
const modalBackup = document.getElementById('modal-backup');
const btnOpenBackup = document.getElementById('btn-open-backup');
const btnCloseBackup = document.getElementById('btn-close-backup');
const btnDoExport = document.getElementById('btn-do-export-backup');
const inputRestore = document.getElementById('input-restore-backup');

btnOpenBackup?.addEventListener('click', () => {
  modalBackup?.classList.remove('hidden');
});
btnCloseBackup?.addEventListener('click', () => {
  modalBackup?.classList.add('hidden');
});

btnDoExport?.addEventListener('click', () => {
  try {
    const backupData = {
      app: 'DaVE Player',
      version: '4.2.0',
      exportedAt: new Date().toISOString(),
      favorites: Array.from(favorites),
      userPlaylists: userPlaylists,
      totalSecondsListened: totalSecondsListened,
      trackPlayCounts: trackPlayCounts,
      currentTheme: currentTheme,
      isNormalizerActive: isNormalizerActive,
      currentTubeMode: currentTubeMode
    };

    const jsonStr = JSON.stringify(backupData, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const dateStr = new Date().toISOString().slice(0, 10);
    a.download = `DaVE_Player_Backup_${dateStr}.json`;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      URL.revokeObjectURL(url);
      a.remove();
    }, 2000);

    showNotification('¡Copia de seguridad JSON exportada con éxito!', 'success');
  } catch (err) {
    console.error(err);
    showNotification('Error al exportar la copia de seguridad.', 'error');
  }
});

inputRestore?.addEventListener('change', (e) => {
  const file = e.target.files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = (event) => {
    try {
      const data = JSON.parse(event.target.result);
      if (!data || typeof data !== 'object') throw new Error('Formato inválido');

      if (Array.isArray(data.favorites)) {
        favorites = new Set(data.favorites);
        localStorage.setItem('dave_favorites', JSON.stringify(data.favorites));
        if (favoritesCount) favoritesCount.innerText = favorites.size;
        const filterFav = document.getElementById('filter-favs-count');
        if (filterFav) filterFav.innerText = favorites.size;
        if (pstatFavs) pstatFavs.innerText = favorites.size;
      }

      if (Array.isArray(data.userPlaylists)) {
        userPlaylists = data.userPlaylists;
        saveUserPlaylists();
        renderPlaylists();
      }

      if (data.totalSecondsListened) {
        totalSecondsListened = parseInt(data.totalSecondsListened, 10) || totalSecondsListened;
        localStorage.setItem('dave_total_seconds', totalSecondsListened.toString());
      }

      if (data.trackPlayCounts && typeof data.trackPlayCounts === 'object') {
        trackPlayCounts = data.trackPlayCounts;
        localStorage.setItem('dave_track_plays', JSON.stringify(trackPlayCounts));
      }

      if (data.currentTheme) {
        currentTheme = data.currentTheme;
        applyTheme(currentTheme);
      }

      if (data.currentTubeMode) {
        setTubeWarmth(data.currentTubeMode);
      }

      renderTrackList();
      renderPhoneTracksList();
      showNotification(`¡Restauración exitosa! (${userPlaylists.length} playlists, ${favorites.size} favoritas)`, 'success');
      modalBackup?.classList.add('hidden');
    } catch (err) {
      console.error(err);
      showNotification('Error al leer el archivo JSON de respaldo. Verifica que sea un archivo válido.', 'error');
    }
  };
  reader.readAsText(file);
});

// ==========================================
// 47. EXPANSOR ESTÉREO 3D & HAAS SOUNDSTAGE (v4.3.0 INFINITY)
// ==========================================
function setStereoWidener(mode = 'normal', silent = false) {
  stereoWidenerMode = mode;
  localStorage.setItem('dave_stereo_widener', mode);
  const btn = document.getElementById('btn-toggle-widener');

  if (!widenerCrossGainL || !widenerCrossGainR) return;

  if (mode === 'normal') {
    widenerCrossGainL.gain.value = 0;
    widenerCrossGainR.gain.value = 0;
    btn?.classList.remove('widener-active');
    btn?.setAttribute('title', 'Expansor Estéreo: Normal (100%)');
    if (!silent) showHUD('fa-arrows-left-right-to-line', 'Estéreo 3D', 'Bypass (Normal 100%)');
  } else if (mode === 'wide') {
    widenerCrossDelayL.delayTime.value = 0.014;
    widenerCrossDelayR.delayTime.value = 0.014;
    widenerCrossGainL.gain.value = -0.35;
    widenerCrossGainR.gain.value = -0.35;
    btn?.classList.add('widener-active');
    btn?.setAttribute('title', 'Expansor Estéreo: Amplio (150%)');
    if (!silent) showHUD('fa-arrows-left-right-to-line', 'Estéreo 3D', 'Escenario Amplio (150%)');
  } else if (mode === 'superwide') {
    widenerCrossDelayL.delayTime.value = 0.024;
    widenerCrossDelayR.delayTime.value = 0.024;
    widenerCrossGainL.gain.value = -0.55;
    widenerCrossGainR.gain.value = -0.55;
    btn?.classList.add('widener-active');
    btn?.setAttribute('title', 'Expansor Estéreo: 3D Holográfico (200%)');
    if (!silent) showHUD('fa-arrows-left-right-to-line', 'Estéreo 3D', '¡Inmersión 3D Total (200%)!');
  }
  if (typeof updateActiveFXBadge === 'function') updateActiveFXBadge();
}

document.getElementById('btn-toggle-widener')?.addEventListener('click', () => {
  initAudioEngine();
  const nextMode = stereoWidenerMode === 'normal' ? 'wide' : (stereoWidenerMode === 'wide' ? 'superwide' : 'normal');
  setStereoWidener(nextMode);
});

// ==========================================
// 48. TRANSPOSITOR DE TONO / KARAOKE KEY SHIFTER (v4.3.0 INFINITY)
// ==========================================
const modalPitch = document.getElementById('modal-pitch-shifter');
const btnOpenPitch = document.getElementById('btn-pitch-shifter');
const btnClosePitch = document.getElementById('btn-close-pitch');
const btnPitchDown = document.getElementById('btn-pitch-down');
const btnPitchUp = document.getElementById('btn-pitch-up');
const btnPitchReset = document.getElementById('btn-pitch-reset');
const pitchValDisplay = document.getElementById('pitch-val-display');
const pitchSemitoneDesc = document.getElementById('pitch-semitone-desc');
const pitchBadgeLabel = document.getElementById('pitch-label');

function applyPlaybackRateAndPitch() {
  const baseSpeed = isSlowedReverb ? 0.85 : currentPlaybackSpeed;
  if (currentPitchSemitones === 0) {
    audio.playbackRate = baseSpeed;
    if ('preservesPitch' in audio) audio.preservesPitch = true;
    if ('mozPreservesPitch' in audio) audio.mozPreservesPitch = true;
  } else {
    if ('preservesPitch' in audio) audio.preservesPitch = false;
    if ('mozPreservesPitch' in audio) audio.mozPreservesPitch = false;
    const pitchFactor = Math.pow(2, currentPitchSemitones / 12);
    audio.playbackRate = baseSpeed * pitchFactor;
  }
}

function setPitchSemitones(semitones, skipHUD = false) {
  currentPitchSemitones = Math.max(-6, Math.min(6, semitones));
  localStorage.setItem('dave_pitch_semitones', currentPitchSemitones.toString());

  if (pitchBadgeLabel) {
    pitchBadgeLabel.innerText = currentPitchSemitones > 0 ? `+${currentPitchSemitones}` : `${currentPitchSemitones}`;
  }
  if (pitchValDisplay) {
    pitchValDisplay.innerText = currentPitchSemitones > 0 ? `+${currentPitchSemitones}` : `${currentPitchSemitones}`;
  }
  if (pitchSemitoneDesc) {
    if (currentPitchSemitones === 0) {
      pitchSemitoneDesc.innerText = 'Tonalidad Original';
    } else if (currentPitchSemitones < 0) {
      pitchSemitoneDesc.innerText = `${currentPitchSemitones} Semitonos (Grave / Bajo)`;
    } else {
      pitchSemitoneDesc.innerText = `+${currentPitchSemitones} Semitonos (Agudo / Alto)`;
    }
  }

  document.querySelectorAll('.pitch-preset-chip').forEach(chip => {
    chip.classList.toggle('active', parseInt(chip.dataset.pitch, 10) === currentPitchSemitones);
  });

  applyPlaybackRateAndPitch();

  if (!skipHUD) {
    const sub = currentPitchSemitones === 0 ? 'Original (0)' : (currentPitchSemitones > 0 ? `+${currentPitchSemitones} Semitonos` : `${currentPitchSemitones} Semitonos`);
    showHUD('fa-music', 'Tonalidad Karaoke', sub);
  }
}

btnOpenPitch?.addEventListener('click', () => {
  modalPitch?.classList.remove('hidden');
});
btnClosePitch?.addEventListener('click', () => {
  modalPitch?.classList.add('hidden');
});
btnPitchDown?.addEventListener('click', () => {
  setPitchSemitones(currentPitchSemitones - 1);
});
btnPitchUp?.addEventListener('click', () => {
  setPitchSemitones(currentPitchSemitones + 1);
});
btnPitchReset?.addEventListener('click', () => {
  setPitchSemitones(0);
});

document.querySelectorAll('.pitch-preset-chip').forEach(chip => {
  chip.addEventListener('click', () => {
    const val = parseInt(chip.dataset.pitch, 10);
    setPitchSemitones(isNaN(val) ? 0 : val);
  });
});

// ==========================================
// 49. ON-SCREEN DISPLAY (CYBERPUNK HUD OVERLAY) (v4.3.0 INFINITY)
// ==========================================
let hudTimeout = null;
function showHUD(icon, title, sub = '') {
  const hud = document.getElementById('player-hud');
  const hudIcon = document.getElementById('hud-icon');
  const hudTitle = document.getElementById('hud-title');
  const hudSub = document.getElementById('hud-sub');
  if (!hud) return;

  if (hudIcon) hudIcon.className = `fa-solid ${icon}`;
  if (hudTitle) hudTitle.innerText = title;
  if (hudSub) {
    hudSub.innerText = sub;
    hudSub.style.display = sub ? 'block' : 'none';
  }

  hud.classList.remove('hidden');
  hud.style.display = 'flex';
  hud.style.opacity = '1';
  hud.style.transform = 'translate(-50%, -50%) scale(1)';

  if (hudTimeout) clearTimeout(hudTimeout);
  hudTimeout = setTimeout(() => {
    hud.style.opacity = '0';
    hud.style.transform = 'translate(-50%, -50%) scale(0.92)';
    setTimeout(() => {
      hud.classList.add('hidden');
      hud.style.display = 'none';
    }, 260);
  }, 1100);
}

// ==========================================
// 49.5. FX STUDIO TRAY CONTROLLER (v4.3.2)
// ==========================================
const fxStudioTray = document.getElementById('fx-studio-tray');
const btnToggleFxTray = document.getElementById('btn-toggle-fx-tray');
const btnCloseFxTray = document.getElementById('btn-close-fx-tray');
const activeFxBadge = document.getElementById('active-fx-badge');

function toggleFxStudioTray() {
  if (!fxStudioTray) return;
  const isHidden = fxStudioTray.classList.toggle('hidden');
  btnToggleFxTray?.classList.toggle('active', !isHidden);
}

btnToggleFxTray?.addEventListener('click', (e) => {
  e.stopPropagation();
  toggleFxStudioTray();
});

btnCloseFxTray?.addEventListener('click', () => {
  fxStudioTray?.classList.add('hidden');
  btnToggleFxTray?.classList.remove('active');
});

// Click outside closes the tray
document.addEventListener('click', (e) => {
  if (fxStudioTray && !fxStudioTray.classList.contains('hidden')) {
    if (!fxStudioTray.contains(e.target) && !btnToggleFxTray?.contains(e.target)) {
      fxStudioTray.classList.add('hidden');
      btnToggleFxTray?.classList.remove('active');
    }
  }
});

function updateActiveFXBadge() {
  let count = 0;
  if (typeof isSlowedReverb !== 'undefined' && isSlowedReverb) count++;
  if (typeof animFrame8D !== 'undefined' && animFrame8D) count++;
  if (typeof currentTubeMode !== 'undefined' && currentTubeMode !== 'off') count++;
  if (typeof stereoWidenerMode !== 'undefined' && stereoWidenerMode !== 'normal') count++;
  if (typeof isDJAutomix !== 'undefined' && isDJAutomix) count++;
  if (typeof isNormalizerActive !== 'undefined' && isNormalizerActive) count++;
  if (typeof isABLooping !== 'undefined' && isABLooping) count++;
  if (typeof sleepTimerRemaining !== 'undefined' && sleepTimerRemaining > 0) count++;
  if (typeof isRecordingAudio !== 'undefined' && isRecordingAudio) count++;

  if (activeFxBadge) {
    if (count > 0) {
      activeFxBadge.innerText = count;
      activeFxBadge.classList.remove('hidden');
      btnToggleFxTray?.classList.add('active');
    } else {
      activeFxBadge.classList.add('hidden');
      btnToggleFxTray?.classList.remove('active');
    }
  }
}

// ==========================================
// 50. STARTUP INITIALIZATION (v4.3.0 INFINITY)
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

  // Apply Theme
  applyTheme(currentTheme);

  // Normalizer button state
  const btnNormalizer = document.getElementById('btn-toggle-normalizer');
  if (btnNormalizer) btnNormalizer.classList.toggle('active', isNormalizerActive);

  // DJ Automix button state
  btnToggleDJ?.classList.toggle('dj-active', isDJAutomix);

  // Populate Radio Stations
  renderRadioStations();

  // Restore sound enhancement settings from localStorage
  const savedTube = localStorage.getItem('dave_tube_warmth');
  if (savedTube) {
    currentTubeMode = savedTube;
    document.querySelectorAll('.tube-chip').forEach(chip => {
      chip.classList.toggle('active', chip.dataset.tube === currentTubeMode);
    });
    const btnToggleTube = document.getElementById('btn-toggle-tubewarmth');
    btnToggleTube?.classList.toggle('tube-active', currentTubeMode !== 'off');
  }

  const savedWidener = localStorage.getItem('dave_stereo_widener');
  if (savedWidener) {
    stereoWidenerMode = savedWidener;
    const btnWidener = document.getElementById('btn-toggle-widener');
    btnWidener?.classList.toggle('widener-active', stereoWidenerMode !== 'normal');
  }

  const savedPitch = localStorage.getItem('dave_pitch_semitones');
  if (savedPitch !== null) {
    const pVal = parseInt(savedPitch, 10);
    if (!isNaN(pVal) && pVal !== 0) {
      setPitchSemitones(pVal, true);
    }
  }

  updateActiveFXBadge();

  // 3. Versioning y catálogo
  const CATALOG_VERSION = '4.3.0';
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
  setupFilterChips();

  // 5. Verificar si había un servidor de teléfono previamente conectado
  const savedConnectedIp = localStorage.getItem('dave_connected_phone_ip');
  if (savedConnectedIp && hasActiveSession) {
    startPhoneHeartbeat(savedConnectedIp);
  }

  // 6. Inicializar ordenación de columnas, scroll de volumen y atajos de teclado
  setupTableSorting();
  setupVolumeScrollAndBadge();
  setupKeyboardShortcuts();
}

function setupVolumeScrollAndBadge() {
  const volumeCluster = document.querySelector('.volume-cluster');
  const volumeSlider = document.getElementById('volume-slider');
  if (!volumeCluster || !volumeSlider) return;

  let volTooltip = document.getElementById('volume-tooltip-badge');
  if (!volTooltip) {
    volTooltip = document.createElement('div');
    volTooltip.id = 'volume-tooltip-badge';
    volTooltip.className = 'volume-tooltip-badge';
    volumeCluster.appendChild(volTooltip);
  }

  let hideTimeout = null;
  const showVolBadge = (val, isMute = false) => {
    volTooltip.innerText = isMute ? 'MUTE' : `${Math.round(val)}%`;
    volTooltip.classList.add('visible');
    clearTimeout(hideTimeout);
    hideTimeout = setTimeout(() => {
      volTooltip.classList.remove('visible');
    }, 1200);
  };

  volumeCluster.addEventListener('wheel', (e) => {
    e.preventDefault();
    const step = e.deltaY < 0 ? 5 : -5;
    let newVol = parseInt(volumeSlider.value, 10) + step;
    newVol = Math.max(0, Math.min(100, newVol));
    volumeSlider.value = newVol;
    setVolume(newVol / 100);
    showVolBadge(newVol, newVol === 0 || isMuted);
  }, { passive: false });

  volumeSlider.addEventListener('input', () => {
    const val = parseInt(volumeSlider.value, 10);
    showVolBadge(val, val === 0 || isMuted);
  });
}

function setupKeyboardShortcuts() {
  document.getElementById('btn-quick-pip')?.addEventListener('click', () => {
    if (typeof togglePictureInPicture === 'function') togglePictureInPicture();
  });

  document.addEventListener('keydown', (e) => {
    // Si el usuario escribe en un campo de texto, solo procesar Escape
    if (['INPUT', 'TEXTAREA'].includes(document.activeElement?.tagName)) {
      if (e.key === 'Escape' && document.activeElement === searchInput) {
        clearGlobalSearch();
        searchInput.blur();
      }
      return;
    }

    if (e.key === '/') {
      e.preventDefault();
      if (searchInput) {
        searchInput.focus();
        searchInput.select();
      }
    } else if (e.code === 'Space') {
      e.preventDefault();
      togglePlay();
    } else if (e.key === 'ArrowRight' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      playNext();
    } else if (e.key === 'ArrowLeft' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      playPrev();
    } else if (e.key === 'm' || e.key === 'M') {
      toggleMute();
    } else if (e.key === 'f' || e.key === 'F') {
      if (currentIndex !== -1 && playlist[currentIndex]) {
        toggleFavorite(playlist[currentIndex]);
      }
    } else if (e.key === 'l' || e.key === 'L') {
      document.getElementById('btn-fullscreen-lyrics')?.click();
    }
  });
}

initApp();
