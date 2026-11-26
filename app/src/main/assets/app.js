/* ==========================================================================
   KYTHERA VIDEO STUDIO — Interactive Motion Graphics Studio Engine
   Vanilla JS (No frameworks, runs natively in Android WebView & Chrome)
   ========================================================================== */

(function () {
  "use strict";

  // --------------------------------------------------------------------------
  // 1. STATE & CONSTANTS
  // --------------------------------------------------------------------------
  const STORAGE_KEY = "kythera_studio_projects_v1";
  const ASPECT_MAP = {
    "9:16": { w: 9, h: 16 },
    "16:9": { w: 16, h: 9 },
    "1:1":  { w: 1, h: 1 },
    "4:5":  { w: 4, h: 5 },
  };

  const EASING_PRESETS = {
    linear:    { label: "Linear",      p1x: 0.25, p1y: 0.25, p2x: 0.75, p2y: 0.75 },
    easeInOut: { label: "Ease In-Out", p1x: 0.42, p1y: 0.0,  p2x: 0.58, p2y: 1.0 },
    easeOut:   { label: "Ease Out",    p1x: 0.16, p1y: 1.0,  p2x: 0.30, p2y: 1.0 },
    easeIn:    { label: "Ease In",     p1x: 0.55, p1y: 0.05, p2x: 0.75, p2y: 0.2 },
    overshoot: { label: "Spring",      p1x: 0.34, p1y: 1.35, p2x: 0.64, p2y: 1.0 },
  };

  const EFFECTS_CATALOG = [
    { id: "gaussian_blur",   cat: "Blur",             name: "Gaussian Blur",       desc: "Smooth isotropic lens blur",       param: "Radius",    min: 0, max: 30,  val: 8 },
    { id: "motion_blur_fx",  cat: "Blur",             name: "Directional Blur",    desc: "High-speed directional streak",    param: "Amount",    min: 0, max: 30,  val: 10 },
    { id: "electric_glow",   cat: "Glow",             name: "Electric Glow",       desc: "Cyan-blue neon bloom aura",        param: "Intensity", min: 0, max: 40,  val: 18 },
    { id: "neon_bloom",      cat: "Glow",             name: "Soft Bloom",          desc: "Thresholded highlight bloom",      param: "Spread",    min: 0, max: 40,  val: 14 },
    { id: "drop_shadow",     cat: "Shadow",           name: "Drop Shadow",         desc: "Elevated soft depth shadow",       param: "Depth",     min: 0, max: 36,  val: 16 },
    { id: "vignette",        cat: "Shadow",           name: "Cinema Vignette",     desc: "Edge framing shadow falloff",      param: "Strength",  min: 0, max: 100, val: 45 },
    { id: "chromatic_shift", cat: "Distortion",       name: "RGB Split",           desc: "Chromatic lens aberration",        param: "Offset",    min: 0, max: 24,  val: 7 },
    { id: "wave_warp",       cat: "Distortion",       name: "Wave Warp",           desc: "Dynamic sinusoidal displacement",  param: "Amplitude", min: 0, max: 25,  val: 8 },
    { id: "color_grade",     cat: "Color Correction", name: "Contrast & Grade",    desc: "Punchy tonal contrast curve",      param: "Contrast",  min: 50, max: 180, val: 124 },
    { id: "hue_shift",       cat: "Color Correction", name: "Hue Shift",           desc: "Spectrum color rotation",          param: "Degrees",   min: 0, max: 360, val: 45 },
    { id: "chroma_key",      cat: "Chroma Key",       name: "Chroma Key",          desc: "Key out green/blue backdrop",      param: "Tolerance", min: 0, max: 100, val: 35 },
    { id: "oscillate",       cat: "Motion Effects",   name: "Oscillate",           desc: "Continuous harmonic float",        param: "Speed",     min: 1, max: 10,  val: 4 },
    { id: "camera_shake",    cat: "Motion Effects",   name: "Handheld Shake",      desc: "Organic micro-jitter impulse",     param: "Jitter",    min: 0, max: 20,  val: 5 },
    { id: "cyber_particles", cat: "Particle Effects", name: "Cyber Sparks",        desc: "Floating glowing micro particles", param: "Density",   min: 5, max: 60,  val: 24 },
    { id: "cross_dissolve",  cat: "Transitions",      name: "Edge Dissolve",       desc: "Smooth opacity fade curve",        param: "Fade",      min: 0, max: 100, val: 60 },
  ];

  const JS_PRESETS = {
    cyberPulse: {
      name: "Cyberpunk Pulse Ring",
      files: {
        "index.html": `<!DOCTYPE html>
<html>
<head>
  <link rel="stylesheet" href="style.css" />
</head>
<body>
  <canvas id="stage"></canvas>
  <div class="hud">KYTHERA // SHADER PULSE</div>
  <script src="app.js"></script>
</body>
</html>`,
        "style.css": `html, body {
  margin: 0; width: 100%; height: 100%;
  background: #060911; overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  font-family: system-ui, sans-serif;
}
canvas { width: 100%; height: 100%; display: block; }
.hud {
  position: fixed; bottom: 14px; left: 16px;
  color: #00E5FF; font-size: 11px; letter-spacing: 0.18em;
  font-weight: 700; opacity: 0.85;
}`,
        "app.js": `const canvas = document.getElementById('stage');
const ctx = canvas.getContext('2d');
let t = 0;
console.log('Initialized Cyber Pulse shader');

function resize() {
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;
}
window.addEventListener('resize', resize);
resize();

function loop() {
  t += 0.03;
  const { width: w, height: h } = canvas;
  ctx.fillStyle = 'rgba(6, 9, 17, 0.22)';
  ctx.fillRect(0, 0, w, h);

  const cx = w / 2, cy = h / 2;
  for (let i = 0; i < 5; i++) {
    const r = 34 + i * 24 + Math.sin(t * 2 + i * 0.7) * 10;
    ctx.beginPath();
    ctx.arc(cx, cy, Math.max(4, r), 0, Math.PI * 2);
    ctx.strokeStyle = i % 2 === 0 ? '#2979FF' : '#00E5FF';
    ctx.lineWidth = 2.5;
    ctx.shadowColor = '#00E5FF';
    ctx.shadowBlur = 14;
    ctx.stroke();
  }
  requestAnimationFrame(loop);
}
loop();`
      }
    },
    kineticBars: {
      name: "Audio Spectrum Bars",
      files: {
        "index.html": `<!DOCTYPE html>
<html>
<head><link rel="stylesheet" href="style.css" /></head>
<body>
  <div id="bars"></div>
  <script src="app.js"></script>
</body>
</html>`,
        "style.css": `body {
  margin: 0; height: 100vh; background: radial-gradient(circle, #101A2F, #05070C);
  display: flex; align-items: center; justify-content: center;
}
#bars { display: flex; align-items: flex-end; gap: 6px; height: 120px; }
.bar {
  width: 10px; border-radius: 99px;
  background: linear-gradient(180deg, #00E5FF, #2979FF);
  box-shadow: 0 0 12px rgba(0,229,255,0.45);
}`,
        "app.js": `const container = document.getElementById('bars');
const bars = Array.from({ length: 16 }, () => {
  const el = document.createElement('div');
  el.className = 'bar';
  container.appendChild(el);
  return el;
});
console.log('Mounted 16 spectrum bars');
let tick = 0;
function animate() {
  tick += 0.08;
  bars.forEach((b, i) => {
    const h = 24 + Math.abs(Math.sin(tick + i * 0.42)) * 90;
    b.style.height = h.toFixed(1) + 'px';
  });
  requestAnimationFrame(animate);
}
animate();`
      }
    }
  };

  const state = {
    currentScreen: "home",
    previousScreen: "home",
    projects: [],
    activeProjectId: null,
    selectedLayerId: null,
    playhead: 1.4,
    isPlaying: false,
    rafId: null,
    lastTick: 0,
    timelineZoom: 95, // px per second
    previewQuality: "FULL",
    showGuides: false,
    drawingMode: false,
    activeDrawer: null, // 'animation' | 'effects' | 'layer' | null
    activeAnimProp: "position",
    activeFxCategory: "All",
    fxSearch: "",
    undoStack: [],
    redoStack: [],
    exportConfig: {
      resolution: "1080p",
      fps: 60,
      format: "MP4",
      bitrate: 24,
    },
    exportTimer: null,
    jsStudio: {
      files: structuredClone(JS_PRESETS.cyberPulse.files),
      activeFile: "app.js",
      presetKey: "cyberPulse",
    },
    imageCache: new Map(),
  };

  // --------------------------------------------------------------------------
  // 2. SAMPLE REALISTIC PROJECTS
  // --------------------------------------------------------------------------
  function makeLayer(overrides) {
    return Object.assign({
      id: "lyr_" + Math.random().toString(36).slice(2, 8),
      name: "Layer",
      type: "shape", // video | audio | image | text | shape | drawing
      start: 0,
      duration: 5.0,
      visible: true,
      locked: false,
      blendMode: "source-over",
      color: "#2979FF",
      secondaryColor: "#00E5FF",
      text: "KYTHERA",
      fontSize: 48,
      shapeKind: "ring", // rect | circle | star | ring | hex
      imageData: null,
      strokes: [],
      motionBlur: { enabled: false, shutter: 180, samples: 8 },
      easing: { ...EASING_PRESETS.easeInOut },
      effects: [],
      keyframes: {
        position: [
          { t: 0, x: 0, y: 20 },
          { t: 1.4, x: 0, y: 0 },
        ],
        rotation: [
          { t: 0, v: -8 },
          { t: 1.4, v: 0 },
        ],
        scale: [
          { t: 0, v: 85 },
          { t: 1.4, v: 100 },
        ],
        opacity: [
          { t: 0, v: 0 },
          { t: 0.6, v: 100 },
        ],
        anchor: [
          { t: 0, x: 0, y: 0 },
        ],
      },
    }, overrides);
  }

  function createDefaultProjects() {
    return [
      {
        id: "proj_neon_pulse",
        name: "Neon Pulse Opener",
        resolution: "1080p",
        aspectRatio: "9:16",
        fps: 60,
        duration: 8.0,
        bgColor: "#070B14",
        updatedAt: "Today · 08:30",
        layers: [
          makeLayer({
            id: "l_vid_1",
            name: "Backdrop Plate.mp4",
            type: "video",
            start: 0,
            duration: 8.0,
            color: "#11244A",
            secondaryColor: "#00E5FF",
            keyframes: {
              position: [{ t: 0, x: 0, y: 0 }],
              rotation: [{ t: 0, v: 0 }],
              scale: [{ t: 0, v: 100 }, { t: 8, v: 112 }],
              opacity: [{ t: 0, v: 100 }],
              anchor: [{ t: 0, x: 0, y: 0 }],
            },
            effects: [{ ...EFFECTS_CATALOG[13], val: 28 }],
          }),
          makeLayer({
            id: "l_shp_1",
            name: "Prism Halo Ring",
            type: "shape",
            shapeKind: "ring",
            start: 0.2,
            duration: 6.8,
            color: "#2979FF",
            secondaryColor: "#00E5FF",
            motionBlur: { enabled: true, shutter: 180, samples: 8 },
            keyframes: {
              position: [{ t: 0.2, x: 0, y: -30 }, { t: 2.0, x: 0, y: -15 }],
              rotation: [{ t: 0.2, v: 0 }, { t: 7.0, v: 180 }],
              scale: [{ t: 0.2, v: 55 }, { t: 1.5, v: 115 }, { t: 4.5, v: 102 }],
              opacity: [{ t: 0.2, v: 0 }, { t: 0.9, v: 100 }],
              anchor: [{ t: 0, x: 0, y: 0 }],
            },
            effects: [{ ...EFFECTS_CATALOG[2], val: 22 }],
          }),
          makeLayer({
            id: "l_txt_1",
            name: "Title: KYTHERA",
            type: "text",
            text: "KYTHERA",
            fontSize: 56,
            start: 0.5,
            duration: 6.2,
            color: "#FFFFFF",
            secondaryColor: "#00E5FF",
            keyframes: {
              position: [{ t: 0.5, x: 0, y: 35 }, { t: 1.6, x: 0, y: 0 }],
              rotation: [{ t: 0.5, v: 0 }],
              scale: [{ t: 0.5, v: 82 }, { t: 1.6, v: 100 }],
              opacity: [{ t: 0.5, v: 0 }, { t: 1.2, v: 100 }],
              anchor: [{ t: 0, x: 0, y: 0 }],
            },
            effects: [{ ...EFFECTS_CATALOG[4], val: 14 }],
          }),
          makeLayer({
            id: "l_txt_2",
            name: "Tagline Subtext",
            type: "text",
            text: "CREATE WITHOUT LIMITS",
            fontSize: 20,
            start: 1.1,
            duration: 5.5,
            color: "#00E5FF",
            keyframes: {
              position: [{ t: 1.1, x: 0, y: 76 }, { t: 1.9, x: 0, y: 52 }],
              rotation: [{ t: 1.1, v: 0 }],
              scale: [{ t: 1.1, v: 95 }, { t: 1.9, v: 100 }],
              opacity: [{ t: 1.1, v: 0 }, { t: 1.8, v: 92 }],
              anchor: [{ t: 0, x: 0, y: 0 }],
            },
          }),
          makeLayer({
            id: "l_aud_1",
            name: "Synth_Sub_Drop.wav",
            type: "audio",
            start: 0,
            duration: 7.6,
            color: "#00E676",
          }),
        ],
      },
      {
        id: "proj_cyber_reel",
        name: "Cyberpunk Kinetic Reel",
        resolution: "4K",
        aspectRatio: "16:9",
        fps: 24,
        duration: 12.0,
        bgColor: "#090612",
        updatedAt: "Yesterday",
        layers: [
          makeLayer({
            name: "Anamorphic Grid",
            type: "video",
            duration: 12.0,
            color: "#221048",
            secondaryColor: "#FF3D71",
          }),
          makeLayer({
            name: "Kinetic Headline",
            type: "text",
            text: "VELOCITY 24FPS",
            fontSize: 52,
            start: 0.4,
            duration: 8.5,
            color: "#F2F6FF",
          }),
          makeLayer({
            name: "Accent Hexagon",
            type: "shape",
            shapeKind: "hex",
            start: 0.8,
            duration: 9.0,
            color: "#FF3D71",
          }),
          makeLayer({
            name: "Bassline_Stem.mp3",
            type: "audio",
            duration: 11.2,
            color: "#00E676",
          }),
        ],
      },
      {
        id: "proj_glass_reveal",
        name: "Glassmorphic Reveal",
        resolution: "1080p",
        aspectRatio: "1:1",
        fps: 30,
        duration: 6.0,
        bgColor: "#061018",
        updatedAt: "Nov 24",
        layers: [
          makeLayer({
            name: "Studio Gradient",
            type: "shape",
            shapeKind: "circle",
            duration: 6.0,
            color: "#00B0FF",
          }),
          makeLayer({
            name: "Hero Star Crest",
            type: "shape",
            shapeKind: "star",
            start: 0.3,
            duration: 5.4,
            color: "#FFB300",
          }),
          makeLayer({
            name: "Brand Mark",
            type: "text",
            text: "STUDIO PRO",
            fontSize: 42,
            start: 0.6,
            duration: 5.0,
            color: "#FFFFFF",
          }),
        ],
      },
    ];
  }

  function loadProjects() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed) && parsed.length > 0) {
          state.projects = parsed;
          state.activeProjectId = parsed[0].id;
          return;
        }
      }
    } catch (_) { /* ignore storage errors */ }
    state.projects = createDefaultProjects();
    state.activeProjectId = state.projects[0].id;
    saveProjects();
  }

  function saveProjects() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state.projects));
    } catch (_) { /* ignore quota errors */ }
  }

  function getActiveProject() {
    return state.projects.find((p) => p.id === state.activeProjectId) || state.projects[0];
  }

  function getSelectedLayer() {
    const proj = getActiveProject();
    if (!proj) return null;
    return proj.layers.find((l) => l.id === state.selectedLayerId) || proj.layers[0] || null;
  }

  // --------------------------------------------------------------------------
  // 3. UNDO / REDO HISTORY
  // --------------------------------------------------------------------------
  function pushUndo(label) {
    const proj = getActiveProject();
    if (!proj) return;
    state.undoStack.push(JSON.stringify({ project: proj, selectedLayerId: state.selectedLayerId, label }));
    if (state.undoStack.length > 30) state.undoStack.shift();
    state.redoStack = [];
    updateHistoryButtons();
  }

  function undo() {
    if (state.undoStack.length === 0) return;
    const proj = getActiveProject();
    state.redoStack.push(JSON.stringify({ project: proj, selectedLayerId: state.selectedLayerId }));
    const snap = JSON.parse(state.undoStack.pop());
    replaceActiveProject(snap.project, snap.selectedLayerId);
    showToast("Undid last edit");
  }

  function redo() {
    if (state.redoStack.length === 0) return;
    const proj = getActiveProject();
    state.undoStack.push(JSON.stringify({ project: proj, selectedLayerId: state.selectedLayerId }));
    const snap = JSON.parse(state.redoStack.pop());
    replaceActiveProject(snap.project, snap.selectedLayerId);
    showToast("Redid edit");
  }

  function replaceActiveProject(updatedProj, selectedId) {
    const idx = state.projects.findIndex((p) => p.id === updatedProj.id);
    if (idx !== -1) state.projects[idx] = updatedProj;
    state.selectedLayerId = selectedId || (updatedProj.layers[0] && updatedProj.layers[0].id) || null;
    saveProjects();
    updateHistoryButtons();
    refreshEditorAll();
  }

  function updateHistoryButtons() {
    const undoBtn = document.getElementById("btn-undo");
    const redoBtn = document.getElementById("btn-redo");
    if (undoBtn) undoBtn.disabled = state.undoStack.length === 0;
    if (redoBtn) redoBtn.disabled = state.redoStack.length === 0;
  }

  // --------------------------------------------------------------------------
  // 4. KEYFRAME INTERPOLATION & EASING ENGINE
  // --------------------------------------------------------------------------
  function cubicBezierY(t, p1x, p1y, p2x, p2y) {
    // Approximate t->u via Newton-Raphson then evaluate Y(u)
    let u = t;
    for (let i = 0; i < 6; i++) {
      const x = 3 * (1 - u) * (1 - u) * u * p1x + 3 * (1 - u) * u * u * p2x + u * u * u;
      const dx = 3 * (1 - u) * (1 - u) * p1x + 6 * (1 - u) * u * (p2x - p1x) + 3 * u * u * (1 - p2x);
      if (Math.abs(dx) < 1e-5) break;
      u = Math.max(0, Math.min(1, u - (x - t) / dx));
    }
    return 3 * (1 - u) * (1 - u) * u * p1y + 3 * (1 - u) * u * u * p2y + u * u * u;
  }

  function evalScalarTrack(kfs, time, fallback, easing) {
    if (!kfs || kfs.length === 0) return fallback;
    const sorted = [...kfs].sort((a, b) => a.t - b.t);
    if (time <= sorted[0].t) return sorted[0].v;
    if (time >= sorted[sorted.length - 1].t) return sorted[sorted.length - 1].v;
    for (let i = 0; i < sorted.length - 1; i++) {
      const a = sorted[i], b = sorted[i + 1];
      if (time >= a.t && time <= b.t) {
        const span = Math.max(0.0001, b.t - a.t);
        const rawT = (time - a.t) / span;
        const e = easing || EASING_PRESETS.easeInOut;
        const eased = cubicBezierY(rawT, e.p1x, e.p1y, e.p2x, e.p2y);
        return a.v + (b.v - a.v) * eased;
      }
    }
    return fallback;
  }

  function evalVecTrack(kfs, time, easing) {
    if (!kfs || kfs.length === 0) return { x: 0, y: 0 };
    const sorted = [...kfs].sort((a, b) => a.t - b.t);
    if (time <= sorted[0].t) return { x: sorted[0].x, y: sorted[0].y };
    if (time >= sorted[sorted.length - 1].t) {
      const last = sorted[sorted.length - 1];
      return { x: last.x, y: last.y };
    }
    for (let i = 0; i < sorted.length - 1; i++) {
      const a = sorted[i], b = sorted[i + 1];
      if (time >= a.t && time <= b.t) {
        const span = Math.max(0.0001, b.t - a.t);
        const rawT = (time - a.t) / span;
        const e = easing || EASING_PRESETS.easeInOut;
        const eased = cubicBezierY(rawT, e.p1x, e.p1y, e.p2x, e.p2y);
        return {
          x: a.x + (b.x - a.x) * eased,
          y: a.y + (b.y - a.y) * eased,
        };
      }
    }
    return { x: 0, y: 0 };
  }

  // --------------------------------------------------------------------------
  // 5. LIVE PREVIEW RENDERER
  // --------------------------------------------------------------------------
  function renderProjectFrame(canvas, project, timeSec, quality = "FULL") {
    if (!canvas || !project) return;
    const rect = canvas.getBoundingClientRect();
    const scaleFactor = quality === "DRAFT" ? 0.5 : quality === "HALF" ? 0.72 : Math.min(window.devicePixelRatio || 1, 2);
    const targetW = Math.max(160, Math.round((rect.width || 320) * scaleFactor));
    const targetH = Math.max(160, Math.round((rect.height || 320) * scaleFactor));

    if (canvas.width !== targetW || canvas.height !== targetH) {
      canvas.width = targetW;
      canvas.height = targetH;
    }

    const ctx = canvas.getContext("2d");
    const w = canvas.width;
    const h = canvas.height;

    ctx.save();
    ctx.clearRect(0, 0, w, h);

    // Background fill + subtle studio radial aura
    ctx.fillStyle = project.bgColor || "#070B14";
    ctx.fillRect(0, 0, w, h);
    const bgGrad = ctx.createRadialGradient(w * 0.5, h * 0.42, 10, w * 0.5, h * 0.5, Math.max(w, h) * 0.7);
    bgGrad.addColorStop(0, "rgba(41, 121, 255, 0.14)");
    bgGrad.addColorStop(1, "rgba(0, 0, 0, 0)");
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, w, h);

    // Subtle grid backdrop
    ctx.strokeStyle = "rgba(255, 255, 255, 0.03)";
    ctx.lineWidth = 1;
    const step = Math.max(24, Math.round(w / 10));
    for (let x = step; x < w; x += step) {
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, h); ctx.stroke();
    }
    for (let y = step; y < h; y += step) {
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
    }

    // Draw layers in stack order (bottom to top)
    for (const layer of project.layers) {
      if (!layer.visible || layer.type === "audio") continue;
      if (timeSec < layer.start || timeSec > layer.start + layer.duration) continue;

      const samples = layer.motionBlur && layer.motionBlur.enabled ? Math.min(4, Math.max(2, Math.round((layer.motionBlur.samples || 8) / 3))) : 1;
      for (let s = samples - 1; s >= 0; s--) {
        const dt = s === 0 ? 0 : -(s * 0.018 * ((layer.motionBlur.shutter || 180) / 180));
        const sampleTime = Math.max(layer.start, timeSec + dt);
        const ghostAlpha = s === 0 ? 1 : 0.22 / samples;
        drawSingleLayer(ctx, layer, sampleTime, w, h, ghostAlpha);
      }
    }

    ctx.restore();
  }

  function drawSingleLayer(ctx, layer, timeSec, w, h, alphaMultiplier = 1) {
    const kfs = layer.keyframes || {};
    const pos = evalVecTrack(kfs.position, timeSec, layer.easing);
    const rot = evalScalarTrack(kfs.rotation, timeSec, 0, layer.easing);
    const scl = evalScalarTrack(kfs.scale, timeSec, 100, layer.easing) / 100;
    const opa = (evalScalarTrack(kfs.opacity, timeSec, 100, layer.easing) / 100) * alphaMultiplier;
    const anc = evalVecTrack(kfs.anchor, timeSec, layer.easing);

    if (opa <= 0.01) return;

    // Effect helpers
    const fxList = layer.effects || [];
    let filterStr = "";
    let oscY = 0;
    let shakeX = 0, shakeY = 0;
    let glowAmt = 0;
    let shadowAmt = 0;
    let rgbSplit = 0;
    let particleDensity = 0;

    for (const fx of fxList) {
      if (fx.id === "gaussian_blur" || fx.id === "motion_blur_fx") filterStr += `blur(${(fx.val * 0.35).toFixed(1)}px) `;
      if (fx.id === "color_grade") filterStr += `contrast(${fx.val}%) `;
      if (fx.id === "hue_shift") filterStr += `hue-rotate(${fx.val}deg) `;
      if (fx.id === "electric_glow" || fx.id === "neon_bloom") glowAmt = Math.max(glowAmt, fx.val);
      if (fx.id === "drop_shadow") shadowAmt = Math.max(shadowAmt, fx.val);
      if (fx.id === "oscillate") oscY = Math.sin(timeSec * fx.val * 1.6) * 14;
      if (fx.id === "camera_shake") {
        shakeX = Math.sin(timeSec * 37) * fx.val * 0.7;
        shakeY = Math.cos(timeSec * 43) * fx.val * 0.7;
      }
      if (fx.id === "chromatic_shift") rgbSplit = fx.val;
      if (fx.id === "cyber_particles") particleDensity = fx.val;
    }

    ctx.save();
    ctx.globalAlpha = Math.max(0, Math.min(1, opa));
    ctx.globalCompositeOperation = layer.blendMode || "source-over";
    if (filterStr.trim()) ctx.filter = filterStr.trim();

    const cx = w * 0.5 + ((pos.x + shakeX) / 100) * (w * 0.45);
    const cy = h * 0.5 + ((pos.y + oscY + shakeY) / 100) * (h * 0.45);

    ctx.translate(cx, cy);
    ctx.rotate((rot * Math.PI) / 180);
    ctx.scale(scl, scl);
    ctx.translate(-(anc.x / 100) * w * 0.25, -(anc.y / 100) * h * 0.25);

    if (glowAmt > 0) {
      ctx.shadowColor = layer.secondaryColor || layer.color || "#00E5FF";
      ctx.shadowBlur = glowAmt * 1.4;
    } else if (shadowAmt > 0) {
      ctx.shadowColor = "rgba(0, 0, 0, 0.75)";
      ctx.shadowBlur = shadowAmt;
      ctx.shadowOffsetY = shadowAmt * 0.4;
    }

    const baseUnit = Math.min(w, h);

    if (layer.type === "video") {
      // Animated motion graphics plate
      const pw = w * 0.88;
      const ph = h * 0.88;
      const grad = ctx.createLinearGradient(-pw / 2, -ph / 2, pw / 2, ph / 2);
      grad.addColorStop(0, layer.color || "#11244A");
      grad.addColorStop(1, "#070B14");
      ctx.fillStyle = grad;
      roundRectPath(ctx, -pw / 2, -ph / 2, pw, ph, 14);
      ctx.fill();

      // Animated horizon sweep
      ctx.strokeStyle = layer.secondaryColor || "#00E5FF";
      ctx.globalAlpha *= 0.35;
      ctx.lineWidth = 2;
      const waveY = Math.sin(timeSec * 2.2) * (ph * 0.12);
      ctx.beginPath();
      ctx.moveTo(-pw * 0.45, waveY);
      ctx.bezierCurveTo(-pw * 0.15, waveY - 30, pw * 0.15, waveY + 30, pw * 0.45, waveY);
      ctx.stroke();
    } else if (layer.type === "shape") {
      const radius = baseUnit * 0.22;
      ctx.strokeStyle = layer.secondaryColor || "#00E5FF";
      ctx.fillStyle = layer.color || "#2979FF";
      ctx.lineWidth = Math.max(3, baseUnit * 0.018);

      if (layer.shapeKind === "ring") {
        ctx.beginPath();
        ctx.arc(0, 0, radius, 0, Math.PI * 2);
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(0, 0, radius * 0.72, timeSec, timeSec + Math.PI * 1.35);
        ctx.strokeStyle = layer.color || "#2979FF";
        ctx.stroke();
      } else if (layer.shapeKind === "circle") {
        const g = ctx.createRadialGradient(-radius * 0.3, -radius * 0.3, 4, 0, 0, radius);
        g.addColorStop(0, layer.secondaryColor || "#00E5FF");
        g.addColorStop(1, layer.color || "#2979FF");
        ctx.fillStyle = g;
        ctx.beginPath();
        ctx.arc(0, 0, radius, 0, Math.PI * 2);
        ctx.fill();
      } else if (layer.shapeKind === "star") {
        drawPolygonStar(ctx, 0, 0, 5, radius, radius * 0.46);
        ctx.fill();
      } else if (layer.shapeKind === "hex") {
        drawPolygonStar(ctx, 0, 0, 6, radius, radius);
        ctx.stroke();
      } else {
        roundRectPath(ctx, -radius, -radius * 0.65, radius * 2, radius * 1.3, 14);
        ctx.fill();
      }
    } else if (layer.type === "text") {
      const fSize = Math.max(12, Math.round((layer.fontSize || 42) * (baseUnit / 360)));
      ctx.font = `800 ${fSize}px "Inter", system-ui, sans-serif`;
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      const txt = layer.text || "KYTHERA";
      if (rgbSplit > 0) {
        ctx.fillStyle = "rgba(255, 61, 113, 0.75)";
        ctx.fillText(txt, -rgbSplit * 0.5, 0);
        ctx.fillStyle = "rgba(0, 229, 255, 0.75)";
        ctx.fillText(txt, rgbSplit * 0.5, 0);
      }
      ctx.fillStyle = layer.color || "#FFFFFF";
      ctx.fillText(txt, 0, 0);
    } else if (layer.type === "image" && layer.imageData) {
      let img = state.imageCache.get(layer.id);
      if (!img) {
        img = new Image();
        img.src = layer.imageData;
        img.onload = () => renderActivePreview();
        state.imageCache.set(layer.id, img);
      }
      if (img.complete && img.naturalWidth > 0) {
        const aspect = img.naturalWidth / img.naturalHeight;
        const drawW = baseUnit * 0.65;
        const drawH = drawW / aspect;
        ctx.drawImage(img, -drawW / 2, -drawH / 2, drawW, drawH);
      }
    } else if (layer.type === "drawing" && Array.isArray(layer.strokes)) {
      ctx.lineCap = "round";
      ctx.lineJoin = "round";
      for (const stroke of layer.strokes) {
        if (!stroke.points || stroke.points.length < 2) continue;
        ctx.strokeStyle = stroke.color || "#00E5FF";
        ctx.lineWidth = Math.max(2, (stroke.size || 5) * (baseUnit / 360));
        ctx.beginPath();
        stroke.points.forEach((pt, idx) => {
          const px = (pt.x - 0.5) * w;
          const py = (pt.y - 0.5) * h;
          if (idx === 0) ctx.moveTo(px, py);
          else ctx.lineTo(px, py);
        });
        ctx.stroke();
      }
    }

    if (particleDensity > 0) {
      ctx.fillStyle = "#00E5FF";
      for (let i = 0; i < particleDensity; i++) {
        const px = Math.sin(i * 19.3 + timeSec * 1.2) * (w * 0.42);
        const py = Math.cos(i * 7.7 - timeSec * 1.5) * (h * 0.42);
        ctx.beginPath();
        ctx.arc(px, py, (i % 3) + 1.2, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    ctx.restore();
  }

  function roundRectPath(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  }

  function drawPolygonStar(ctx, cx, cy, spikes, outerR, innerR) {
    let rot = (Math.PI / 2) * 3;
    const step = Math.PI / spikes;
    ctx.beginPath();
    ctx.moveTo(cx, cy - outerR);
    for (let i = 0; i < spikes; i++) {
      ctx.lineTo(cx + Math.cos(rot) * outerR, cy + Math.sin(rot) * outerR);
      rot += step;
      if (innerR !== outerR) {
        ctx.lineTo(cx + Math.cos(rot) * innerR, cy + Math.sin(rot) * innerR);
      }
      rot += step;
    }
    ctx.closePath();
  }

  function renderActivePreview() {
    const proj = getActiveProject();
    const canvas = document.getElementById("preview-canvas");
    renderProjectFrame(canvas, proj, state.playhead, state.previewQuality);
  }

  // --------------------------------------------------------------------------
  // 6. SCREEN ROUTING & HOME SCREEN
  // --------------------------------------------------------------------------
  function navigateTo(screenName) {
    if (screenName !== "editor" && state.isPlaying) togglePlayback(false);
    state.previousScreen = state.currentScreen;
    state.currentScreen = screenName;
    document.querySelectorAll(".screen").forEach((el) => {
      el.classList.toggle("active", el.id === `screen-${screenName}`);
    });

    if (screenName === "home") {
      renderHomeProjects();
    } else if (screenName === "editor") {
      closeDrawers();
      refreshEditorAll();
    } else if (screenName === "export") {
      refreshExportScreen();
    } else if (screenName === "js-studio") {
      renderJsStudioFiles();
      runJsStudioPreview();
    }
  }

  function renderHomeProjects() {
    const grid = document.getElementById("recent-projects-grid");
    const countPill = document.getElementById("project-count-pill");
    if (!grid) return;
    countPill.textContent = String(state.projects.length);
    grid.innerHTML = "";

    state.projects.forEach((proj) => {
      const card = document.createElement("article");
      card.className = "project-card";

      const thumb = document.createElement("div");
      thumb.className = "project-thumb";
      const cvs = document.createElement("canvas");
      thumb.appendChild(cvs);

      const badges = document.createElement("div");
      badges.className = "thumb-overlay-badges";
      badges.innerHTML = `
        <span class="spec-pill">${proj.resolution} · ${proj.fps} FPS</span>
        <span class="spec-pill">${proj.aspectRatio} · ${formatTimecode(proj.duration, proj.fps)}</span>
      `;
      thumb.appendChild(badges);
      thumb.addEventListener("click", () => openProjectInEditor(proj.id));

      const body = document.createElement("div");
      body.className = "project-card-body";

      const info = document.createElement("div");
      info.className = "project-card-info";
      info.innerHTML = `
        <h3 class="project-card-title">${escapeHtml(proj.name)}</h3>
        <p class="project-card-sub">${proj.layers.length} layers · ${proj.updatedAt || "Edited"}</p>
      `;
      info.addEventListener("click", () => openProjectInEditor(proj.id));

      const gearBtn = document.createElement("button");
      gearBtn.className = "tool-icon-btn";
      gearBtn.type = "button";
      gearBtn.title = "Project Settings";
      gearBtn.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>`;
      gearBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        openProjectSettingsModal(proj.id);
      });

      body.appendChild(info);
      body.appendChild(gearBtn);
      card.appendChild(thumb);
      card.appendChild(body);
      grid.appendChild(card);

      requestAnimationFrame(() => renderProjectFrame(cvs, proj, 1.4, "HALF"));
    });
  }

  function openProjectInEditor(projectId) {
    state.activeProjectId = projectId;
    const proj = getActiveProject();
    state.selectedLayerId = proj && proj.layers[0] ? proj.layers[0].id : null;
    state.playhead = Math.min(1.4, proj ? proj.duration * 0.25 : 0);
    state.undoStack = [];
    state.redoStack = [];
    updateHistoryButtons();
    navigateTo("editor");
  }

  // --------------------------------------------------------------------------
  // 7. EDITOR TIMELINE, SCRUBBING & CLIP DRAG
  // --------------------------------------------------------------------------
  function refreshEditorAll() {
    const proj = getActiveProject();
    if (!proj) return;

    document.getElementById("editor-project-name").textContent = proj.name;
    document.getElementById("editor-project-specs").textContent = `${proj.resolution} · ${proj.fps}fps`;
    document.getElementById("aspect-ratio-label").textContent = proj.aspectRatio;
    document.getElementById("hud-aspect-label").textContent = proj.aspectRatio;

    const frameBox = document.getElementById("preview-frame-box");
    const ratio = ASPECT_MAP[proj.aspectRatio] || ASPECT_MAP["9:16"];
    frameBox.style.aspectRatio = `${ratio.w} / ${ratio.h}`;

    const sel = getSelectedLayer();
    document.getElementById("hud-layer-label").textContent = sel ? sel.name : "No Layer";
    document.getElementById("tl-selected-indicator").textContent = sel
      ? `${sel.name} (${sel.type.toUpperCase()})`
      : "Select a layer";

    updateTimecodeUI();
    renderTimeline();
    renderActivePreview();
    syncQuickKeyframeIcon();
  }

  function formatTimecode(sec, fps = 60) {
    const clamped = Math.max(0, sec);
    const mins = Math.floor(clamped / 60);
    const secs = Math.floor(clamped % 60);
    const frames = Math.floor((clamped % 1) * fps);
    return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}:${String(frames).padStart(2, "0")}`;
  }

  function updateTimecodeUI() {
    const proj = getActiveProject();
    if (!proj) return;
    document.getElementById("current-timecode").textContent = formatTimecode(state.playhead, proj.fps);
    document.getElementById("total-duration").textContent = formatTimecode(proj.duration, proj.fps);
    const playheadEl = document.getElementById("timeline-playhead");
    if (playheadEl) {
      playheadEl.style.left = `${state.playhead * state.timelineZoom}px`;
    }
  }

  function renderTimeline() {
    const proj = getActiveProject();
    if (!proj) return;

    const zoom = state.timelineZoom;
    const totalPx = Math.max(600, Math.ceil((proj.duration + 1.5) * zoom));

    const wrap = document.getElementById("timeline-canvas-wrap");
    wrap.style.width = `${totalPx}px`;

    // Ruler Canvas
    const rulerCanvas = document.getElementById("ruler-canvas");
    rulerCanvas.width = totalPx;
    rulerCanvas.height = 26;
    const rctx = rulerCanvas.getContext("2d");
    rctx.clearRect(0, 0, totalPx, 26);
    rctx.fillStyle = "#98A6BE";
    rctx.font = "600 10px monospace";

    const secStep = zoom < 65 ? 2 : 1;
    for (let s = 0; s <= proj.duration + 1; s += 0.25) {
      const x = Math.round(s * zoom) + 0.5;
      const isMajor = Math.abs(s % secStep) < 0.01;
      rctx.strokeStyle = isMajor ? "rgba(152, 166, 190, 0.45)" : "rgba(152, 166, 190, 0.16)";
      rctx.beginPath();
      rctx.moveTo(x, isMajor ? 10 : 18);
      rctx.lineTo(x, 26);
      rctx.stroke();
      if (isMajor) {
        rctx.fillText(`${Math.round(s)}s`, x + 4, 11);
      }
    }

    const headersList = document.getElementById("track-headers-list");
    const tracksList = document.getElementById("timeline-tracks-list");
    headersList.innerHTML = "";
    tracksList.innerHTML = "";

    const typeColors = {
      video: "#2979FF",
      audio: "#00E676",
      image: "#00E5FF",
      text: "#B388FF",
      shape: "#FFB300",
      drawing: "#FF3D71",
    };

    // Render top-most layer at top of timeline (reverse display order)
    const displayLayers = [...proj.layers].reverse();
    displayLayers.forEach((layer) => {
      const isSelected = layer.id === state.selectedLayerId;

      // Track Header
      const hdr = document.createElement("div");
      hdr.className = `track-header-item ${isSelected ? "selected" : ""}`;
      hdr.innerHTML = `
        <div class="track-header-meta">
          <span class="track-type-dot" style="background:${typeColors[layer.type] || "#2979FF"}"></span>
          <span class="track-header-name">${escapeHtml(layer.name)}</span>
        </div>
        <button class="track-eye-btn ${layer.visible ? "" : "muted"}" type="button" title="Toggle Visibility">
          ${layer.visible ? "◉" : "○"}
        </button>
      `;
      hdr.addEventListener("click", () => selectLayer(layer.id));
      hdr.querySelector(".track-eye-btn").addEventListener("click", (e) => {
        e.stopPropagation();
        pushUndo("Toggle visibility");
        layer.visible = !layer.visible;
        saveProjects();
        refreshEditorAll();
      });
      headersList.appendChild(hdr);

      // Track Row
      const row = document.createElement("div");
      row.className = `timeline-track-row ${isSelected ? "selected" : ""}`;
      row.style.backgroundSize = `${zoom}px 100%`;

      const clip = document.createElement("div");
      clip.className = `timeline-clip clip-type-${layer.type} ${isSelected ? "selected" : ""}`;
      clip.style.left = `${layer.start * zoom}px`;
      clip.style.width = `${Math.max(24, layer.duration * zoom)}px`;

      if (layer.type === "audio") {
        clip.appendChild(createWaveformSvg(layer.duration));
      }

      const label = document.createElement("span");
      label.className = "clip-label";
      label.textContent = layer.name;
      clip.appendChild(label);

      const handleL = document.createElement("div");
      handleL.className = "clip-handle left";
      const handleR = document.createElement("div");
      handleR.className = "clip-handle right";
      clip.appendChild(handleL);
      clip.appendChild(handleR);

      // Keyframe diamonds directly on track
      const allTimes = collectLayerKeyframeTimes(layer);
      allTimes.forEach((kfTime) => {
        if (kfTime < layer.start || kfTime > layer.start + layer.duration) return;
        const dot = document.createElement("div");
        dot.className = "kf-diamond-marker";
        dot.style.left = `${(kfTime - layer.start) * zoom}px`;
        dot.title = `Keyframe @ ${kfTime.toFixed(2)}s`;
        dot.addEventListener("click", (e) => {
          e.stopPropagation();
          selectLayer(layer.id);
          setPlayhead(kfTime);
        });
        clip.appendChild(dot);
      });

      attachClipDrag(clip, handleL, handleR, layer);
      row.appendChild(clip);
      tracksList.appendChild(row);
    });

    // Sync scroll between track list and header list
    const scrollBox = document.getElementById("timeline-scroll");
    scrollBox.onscroll = () => {
      headersList.scrollTop = scrollBox.scrollTop;
    };

    updateTimecodeUI();
  }

  function createWaveformSvg(duration) {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("class", "clip-waveform");
    svg.setAttribute("viewBox", "0 0 200 28");
    svg.setAttribute("preserveAspectRatio", "none");
    let d = "";
    const bars = 42;
    for (let i = 0; i < bars; i++) {
      const h = 4 + Math.abs(Math.sin(i * 0.72 + duration) * 18);
      const x = (i / bars) * 200;
      const y = (28 - h) / 2;
      d += `M${x.toFixed(1)},${y.toFixed(1)} v${h.toFixed(1)} `;
    }
    const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    path.setAttribute("d", d);
    path.setAttribute("stroke", "#00E676");
    path.setAttribute("stroke-width", "2.2");
    path.setAttribute("fill", "none");
    svg.appendChild(path);
    return svg;
  }

  function collectLayerKeyframeTimes(layer) {
    const times = new Set();
    if (!layer.keyframes) return [];
    Object.values(layer.keyframes).forEach((arr) => {
      if (Array.isArray(arr)) arr.forEach((kf) => times.add(Number(kf.t.toFixed(2))));
    });
    return [...times].sort((a, b) => a - b);
  }

  function attachClipDrag(clipEl, handleL, handleR, layer) {
    let mode = null;
    let startX = 0;
    let origStart = 0;
    let origDur = 0;
    let moved = false;

    const onDown = (e, dragMode) => {
      e.stopPropagation();
      selectLayer(layer.id);
      mode = dragMode;
      startX = e.clientX;
      origStart = layer.start;
      origDur = layer.duration;
      moved = false;
      clipEl.setPointerCapture(e.pointerId);
    };

    clipEl.addEventListener("pointerdown", (e) => {
      if (e.target === handleL) onDown(e, "trimL");
      else if (e.target === handleR) onDown(e, "trimR");
      else onDown(e, "move");
    });

    clipEl.addEventListener("pointermove", (e) => {
      if (!mode || layer.locked) return;
      const dx = e.clientX - startX;
      if (Math.abs(dx) > 3 && !moved) {
        moved = true;
        pushUndo("Adjust clip timing");
      }
      if (!moved) return;
      const dt = dx / state.timelineZoom;
      const proj = getActiveProject();

      if (mode === "move") {
        layer.start = Math.max(0, Math.min(proj.duration - 0.3, Number((origStart + dt).toFixed(2))));
      } else if (mode === "trimR") {
        layer.duration = Math.max(0.3, Math.min(proj.duration - origStart, Number((origDur + dt).toFixed(2))));
      } else if (mode === "trimL") {
        const clampedDt = Math.max(-origStart, Math.min(origDur - 0.3, dt));
        layer.start = Number((origStart + clampedDt).toFixed(2));
        layer.duration = Number((origDur - clampedDt).toFixed(2));
      }
      clipEl.style.left = `${layer.start * state.timelineZoom}px`;
      clipEl.style.width = `${Math.max(24, layer.duration * state.timelineZoom)}px`;
      renderActivePreview();
    });

    const onUp = () => {
      if (moved) {
        saveProjects();
        renderTimeline();
      }
      mode = null;
    };
    clipEl.addEventListener("pointerup", onUp);
    clipEl.addEventListener("pointercancel", onUp);
  }

  function selectLayer(layerId) {
    state.selectedLayerId = layerId;
    refreshEditorAll();
    if (state.activeDrawer === "animation") renderAnimationDrawer();
    if (state.activeDrawer === "effects") renderEffectsDrawer();
    if (state.activeDrawer === "layer") renderLayerSettingsDrawer();
  }

  function setPlayhead(sec) {
    const proj = getActiveProject();
    if (!proj) return;
    state.playhead = Math.max(0, Math.min(proj.duration, Number(sec.toFixed(3))));
    updateTimecodeUI();
    renderActivePreview();
    syncQuickKeyframeIcon();
    if (state.activeDrawer === "animation") renderAnimationDrawer();
  }

  function togglePlayback(forceState) {
    const next = typeof forceState === "boolean" ? forceState : !state.isPlaying;
    state.isPlaying = next;
    document.getElementById("icon-play").classList.toggle("hidden", next);
    document.getElementById("icon-pause").classList.toggle("hidden", !next);

    if (next) {
      state.lastTick = performance.now();
      const step = (now) => {
        if (!state.isPlaying) return;
        const dt = (now - state.lastTick) / 1000;
        state.lastTick = now;
        const proj = getActiveProject();
        let nextTime = state.playhead + dt;
        if (nextTime >= proj.duration) nextTime = 0;
        state.playhead = nextTime;
        updateTimecodeUI();
        renderActivePreview();
        state.rafId = requestAnimationFrame(step);
      };
      state.rafId = requestAnimationFrame(step);
    } else if (state.rafId) {
      cancelAnimationFrame(state.rafId);
      state.rafId = null;
    }
  }

  // --------------------------------------------------------------------------
  // 8. DRAWERS: ANIMATION, EFFECTS, LAYER SETTINGS
  // --------------------------------------------------------------------------
  function openDrawer(name) {
    const layer = getSelectedLayer();
    if (!layer) {
      showToast("Select or add a layer first");
      return;
    }
    state.activeDrawer = name;
    document.querySelectorAll(".studio-drawer").forEach((d) => d.classList.add("hidden"));
    document.querySelectorAll(".dock-btn").forEach((b) => b.classList.toggle("active", b.dataset.tool === name));

    if (name === "animation") {
      document.getElementById("panel-animation").classList.remove("hidden");
      renderAnimationDrawer();
    } else if (name === "effects") {
      document.getElementById("panel-effects").classList.remove("hidden");
      renderEffectsDrawer();
    } else if (name === "layer") {
      document.getElementById("panel-layer-settings").classList.remove("hidden");
      renderLayerSettingsDrawer();
    }
  }

  function closeDrawers() {
    state.activeDrawer = null;
    document.querySelectorAll(".studio-drawer").forEach((d) => d.classList.add("hidden"));
    document.querySelectorAll(".dock-btn").forEach((b) => b.classList.remove("active"));
  }

  function syncQuickKeyframeIcon() {
    const layer = getSelectedLayer();
    const btn = document.getElementById("btn-add-keyframe-quick");
    if (!layer || !btn) return;
    const times = collectLayerKeyframeTimes(layer);
    const hasAtHead = times.some((t) => Math.abs(t - state.playhead) < 0.08);
    btn.classList.toggle("active", hasAtHead);
  }

  function toggleKeyframeAtPlayhead(propName) {
    const layer = getSelectedLayer();
    if (!layer) return;
    pushUndo("Toggle keyframe");
    const t = Number(state.playhead.toFixed(2));
    const prop = propName || state.activeAnimProp;
    const arr = layer.keyframes[prop] || (layer.keyframes[prop] = []);
    const existingIdx = arr.findIndex((k) => Math.abs(k.t - t) < 0.08);

    if (existingIdx !== -1 && arr.length > 1) {
      arr.splice(existingIdx, 1);
      showToast(`Removed ${prop} keyframe`);
    } else if (existingIdx === -1) {
      if (prop === "position" || prop === "anchor") {
        const cur = evalVecTrack(arr, t, layer.easing);
        arr.push({ t, x: Math.round(cur.x), y: Math.round(cur.y) });
      } else {
        const cur = evalScalarTrack(arr, t, prop === "rotation" ? 0 : 100, layer.easing);
        arr.push({ t, v: Math.round(cur) });
      }
      arr.sort((a, b) => a.t - b.t);
      showToast(`Added ${prop} keyframe @ ${t}s`);
    }
    saveProjects();
    refreshEditorAll();
    if (state.activeDrawer === "animation") renderAnimationDrawer();
  }

  function renderAnimationDrawer() {
    const layer = getSelectedLayer();
    if (!layer) return;
    document.getElementById("anim-layer-badge").textContent = layer.name;

    const slidersBox = document.getElementById("anim-property-sliders");
    const prop = state.activeAnimProp;
    slidersBox.innerHTML = "";

    if (prop === "position" || prop === "anchor") {
      const vec = evalVecTrack(layer.keyframes[prop], state.playhead, layer.easing);
      slidersBox.appendChild(createSliderControl(`${prop === "position" ? "Pos X" : "Anchor X"}`, -100, 100, Math.round(vec.x), "%", (val) => {
        upsertKeyframeVec(layer, prop, state.playhead, val, null);
      }));
      slidersBox.appendChild(createSliderControl(`${prop === "position" ? "Pos Y" : "Anchor Y"}`, -100, 100, Math.round(vec.y), "%", (val) => {
        upsertKeyframeVec(layer, prop, state.playhead, null, val);
      }));
    } else if (prop === "rotation") {
      const rot = evalScalarTrack(layer.keyframes.rotation, state.playhead, 0, layer.easing);
      slidersBox.appendChild(createSliderControl("Rotation Angle", -360, 360, Math.round(rot), "°", (val) => {
        upsertKeyframeScalar(layer, "rotation", state.playhead, val);
      }));
    } else if (prop === "scale") {
      const scl = evalScalarTrack(layer.keyframes.scale, state.playhead, 100, layer.easing);
      slidersBox.appendChild(createSliderControl("Uniform Scale", 10, 260, Math.round(scl), "%", (val) => {
        upsertKeyframeScalar(layer, "scale", state.playhead, val);
      }));
    } else if (prop === "opacity") {
      const opa = evalScalarTrack(layer.keyframes.opacity, state.playhead, 100, layer.easing);
      slidersBox.appendChild(createSliderControl("Layer Opacity", 0, 100, Math.round(opa), "%", (val) => {
        upsertKeyframeScalar(layer, "opacity", state.playhead, val);
      }));
    } else if (prop === "motionblur") {
      const mb = layer.motionBlur || (layer.motionBlur = { enabled: false, shutter: 180, samples: 8 });
      const row = document.createElement("div");
      row.className = "card-row-between";
      row.innerHTML = `<span>Enable Motion Blur</span><button class="mini-pill-btn ${mb.enabled ? "primary" : ""}" type="button">${mb.enabled ? "ON" : "OFF"}</button>`;
      row.querySelector("button").addEventListener("click", () => {
        pushUndo("Toggle motion blur");
        mb.enabled = !mb.enabled;
        saveProjects();
        renderAnimationDrawer();
        renderActivePreview();
      });
      slidersBox.appendChild(row);
      slidersBox.appendChild(createSliderControl("Shutter Angle", 30, 360, mb.shutter, "°", (val) => {
        mb.shutter = val;
        saveProjects();
        renderActivePreview();
      }));
      slidersBox.appendChild(createSliderControl("Sub-frame Samples", 4, 24, mb.samples, "x", (val) => {
        mb.samples = val;
        saveProjects();
        renderActivePreview();
      }));
    }

    // Mini Keyframe Strip
    const strip = document.getElementById("kf-mini-track");
    strip.innerHTML = "";
    const proj = getActiveProject();
    const phPct = Math.min(100, Math.max(0, (state.playhead / proj.duration) * 100));
    const ph = document.createElement("div");
    ph.className = "kf-mini-playhead";
    ph.style.left = `${phPct}%`;
    strip.appendChild(ph);

    const kfs = layer.keyframes[prop === "motionblur" ? "position" : prop] || [];
    kfs.forEach((k) => {
      const dot = document.createElement("div");
      dot.className = "kf-mini-dot";
      dot.style.left = `${Math.min(98, Math.max(2, (k.t / proj.duration) * 100))}%`;
      dot.addEventListener("click", () => setPlayhead(k.t));
      strip.appendChild(dot);
    });

    drawEasingGraph(layer.easing || EASING_PRESETS.easeInOut);
  }

  function createSliderControl(label, min, max, value, unit, onChange) {
    const wrap = document.createElement("div");
    wrap.className = "slider-field";
    wrap.innerHTML = `
      <div class="slider-field-top">
        <span>${label}</span>
        <span class="slider-val">${value}${unit}</span>
      </div>
      <input class="studio-slider" type="range" min="${min}" max="${max}" value="${value}" />
    `;
    const input = wrap.querySelector("input");
    const valSpan = wrap.querySelector(".slider-val");
    input.addEventListener("pointerdown", () => pushUndo(`Edit ${label}`));
    input.addEventListener("input", () => {
      const v = Number(input.value);
      valSpan.textContent = `${v}${unit}`;
      onChange(v);
    });
    return wrap;
  }

  function upsertKeyframeScalar(layer, prop, timeSec, value) {
    const t = Number(timeSec.toFixed(2));
    const arr = layer.keyframes[prop] || (layer.keyframes[prop] = []);
    const hit = arr.find((k) => Math.abs(k.t - t) < 0.08);
    if (hit) hit.v = value;
    else {
      arr.push({ t, v: value });
      arr.sort((a, b) => a.t - b.t);
    }
    saveProjects();
    renderActivePreview();
    renderTimeline();
  }

  function upsertKeyframeVec(layer, prop, timeSec, nextX, nextY) {
    const t = Number(timeSec.toFixed(2));
    const arr = layer.keyframes[prop] || (layer.keyframes[prop] = []);
    const cur = evalVecTrack(arr, t, layer.easing);
    const hit = arr.find((k) => Math.abs(k.t - t) < 0.08);
    if (hit) {
      if (nextX !== null) hit.x = nextX;
      if (nextY !== null) hit.y = nextY;
    } else {
      arr.push({ t, x: nextX !== null ? nextX : Math.round(cur.x), y: nextY !== null ? nextY : Math.round(cur.y) });
      arr.sort((a, b) => a.t - b.t);
    }
    saveProjects();
    renderActivePreview();
    renderTimeline();
  }

  function drawEasingGraph(easing) {
    const cvs = document.getElementById("easing-graph-canvas");
    if (!cvs) return;
    const ctx = cvs.getContext("2d");
    const w = cvs.width, h = cvs.height;
    const pad = 22;
    const gw = w - pad * 2, gh = h - pad * 2;

    ctx.clearRect(0, 0, w, h);
    ctx.strokeStyle = "rgba(255,255,255,0.06)";
    ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
      const gx = pad + (gw / 4) * i;
      const gy = pad + (gh / 4) * i;
      ctx.beginPath(); ctx.moveTo(gx, pad); ctx.lineTo(gx, h - pad); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(pad, gy); ctx.lineTo(w - pad, gy); ctx.stroke();
    }

    const x0 = pad, y0 = h - pad;
    const x3 = w - pad, y3 = pad;
    const x1 = pad + easing.p1x * gw, y1 = h - pad - easing.p1y * gh;
    const x2 = pad + easing.p2x * gw, y2 = h - pad - easing.p2y * gh;

    // Handle arms
    ctx.strokeStyle = "rgba(0, 229, 255, 0.45)";
    ctx.setLineDash([3, 3]);
    ctx.beginPath(); ctx.moveTo(x0, y0); ctx.lineTo(x1, y1); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(x3, y3); ctx.lineTo(x2, y2); ctx.stroke();
    ctx.setLineDash([]);

    // Bezier curve
    ctx.strokeStyle = "#2979FF";
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(x0, y0);
    ctx.bezierCurveTo(x1, y1, x2, y2, x3, y3);
    ctx.stroke();

    // Control handles
    [[x1, y1], [x2, y2]].forEach(([hx, hy]) => {
      ctx.fillStyle = "#00E5FF";
      ctx.beginPath();
      ctx.arc(hx, hy, 5.5, 0, Math.PI * 2);
      ctx.fill();
    });

    document.getElementById("easing-preset-label").textContent = easing.label || "Custom Bezier";
  }

  function setupEasingGraphDrag() {
    const cvs = document.getElementById("easing-graph-canvas");
    if (!cvs) return;
    let draggingHandle = null;

    const getCoords = (e) => {
      const r = cvs.getBoundingClientRect();
      const pad = 22;
      const nx = Math.max(0, Math.min(1, ((e.clientX - r.left) * (cvs.width / r.width) - pad) / (cvs.width - pad * 2)));
      const ny = Math.max(-0.35, Math.min(1.35, (cvs.height - pad - (e.clientY - r.top) * (cvs.height / r.height)) / (cvs.height - pad * 2)));
      return { nx, ny };
    };

    cvs.addEventListener("pointerdown", (e) => {
      const layer = getSelectedLayer();
      if (!layer) return;
      const { nx, ny } = getCoords(e);
      const eCfg = layer.easing || (layer.easing = { ...EASING_PRESETS.easeInOut });
      const d1 = Math.hypot(nx - eCfg.p1x, ny - eCfg.p1y);
      const d2 = Math.hypot(nx - eCfg.p2x, ny - eCfg.p2y);
      draggingHandle = d1 <= d2 ? "p1" : "p2";
      pushUndo("Edit easing curve");
      cvs.setPointerCapture(e.pointerId);
    });

    cvs.addEventListener("pointermove", (e) => {
      if (!draggingHandle) return;
      const layer = getSelectedLayer();
      if (!layer) return;
      const { nx, ny } = getCoords(e);
      if (draggingHandle === "p1") {
        layer.easing.p1x = Number(nx.toFixed(2));
        layer.easing.p1y = Number(ny.toFixed(2));
      } else {
        layer.easing.p2x = Number(nx.toFixed(2));
        layer.easing.p2y = Number(ny.toFixed(2));
      }
      layer.easing.label = "Custom";
      drawEasingGraph(layer.easing);
      renderActivePreview();
    });

    cvs.addEventListener("pointerup", () => {
      if (draggingHandle) saveProjects();
      draggingHandle = null;
    });
  }

  // Effects Browser
  function renderEffectsDrawer() {
    const layer = getSelectedLayer();
    if (!layer) return;
    document.getElementById("fx-layer-badge").textContent = layer.name;

    const categories = ["All", "Blur", "Glow", "Shadow", "Distortion", "Color Correction", "Chroma Key", "Motion Effects", "Particle Effects", "Transitions"];
    const catBar = document.getElementById("fx-categories-bar");
    catBar.innerHTML = "";
    categories.forEach((cat) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = `fx-cat-pill ${state.activeFxCategory === cat ? "active" : ""}`;
      btn.textContent = cat;
      btn.addEventListener("click", () => {
        state.activeFxCategory = cat;
        renderEffectsDrawer();
      });
      catBar.appendChild(btn);
    });

    const catalogGrid = document.getElementById("fx-catalog-grid");
    catalogGrid.innerHTML = "";
    const q = state.fxSearch.trim().toLowerCase();
    const filtered = EFFECTS_CATALOG.filter((fx) => {
      const matchCat = state.activeFxCategory === "All" || fx.cat === state.activeFxCategory;
      const matchQ = !q || fx.name.toLowerCase().includes(q) || fx.cat.toLowerCase().includes(q);
      return matchCat && matchQ;
    });

    filtered.forEach((fx) => {
      const card = document.createElement("button");
      card.type = "button";
      card.className = "fx-item-card";
      card.innerHTML = `
        <span class="fx-item-cat">${fx.cat}</span>
        <span class="fx-item-name">${fx.name}</span>
        <span class="fx-item-desc">${fx.desc}</span>
      `;
      card.addEventListener("click", () => {
        pushUndo(`Add ${fx.name}`);
        layer.effects = layer.effects || [];
        layer.effects.push({ ...fx });
        saveProjects();
        renderEffectsDrawer();
        renderActivePreview();
        showToast(`Applied ${fx.name}`);
      });
      catalogGrid.appendChild(card);
    });

    const activeList = document.getElementById("fx-active-list");
    const activeFx = layer.effects || [];
    document.getElementById("fx-stack-count").textContent = String(activeFx.length);
    activeList.innerHTML = "";

    if (activeFx.length === 0) {
      activeList.innerHTML = `<p style="color:var(--text-muted);font-size:11.5px;margin:4px 0;">Tap any effect on the left to add it to this layer.</p>`;
      return;
    }

    activeFx.forEach((fx, idx) => {
      const row = document.createElement("div");
      row.className = "fx-active-row";
      row.innerHTML = `
        <div class="fx-active-row-top">
          <strong style="font-size:12px;">${fx.name}</strong>
          <button class="tl-mini-btn text" type="button">Remove</button>
        </div>
        <div class="slider-field">
          <div class="slider-field-top">
            <span>${fx.param}</span>
            <span class="slider-val">${fx.val}</span>
          </div>
          <input class="studio-slider" type="range" min="${fx.min}" max="${fx.max}" value="${fx.val}" />
        </div>
      `;
      row.querySelector("button").addEventListener("click", () => {
        pushUndo(`Remove ${fx.name}`);
        layer.effects.splice(idx, 1);
        saveProjects();
        renderEffectsDrawer();
        renderActivePreview();
      });
      const slider = row.querySelector("input");
      const valSpan = row.querySelector(".slider-val");
      slider.addEventListener("input", () => {
        fx.val = Number(slider.value);
        valSpan.textContent = String(fx.val);
        saveProjects();
        renderActivePreview();
      });
      activeList.appendChild(row);
    });
  }

  // Layer Settings Drawer
  function renderLayerSettingsDrawer() {
    const layer = getSelectedLayer();
    const proj = getActiveProject();
    if (!layer || !proj) return;
    document.getElementById("ls-layer-badge").textContent = layer.name;
    const body = document.getElementById("layer-settings-body");
    body.innerHTML = "";

    // Card 1: General & Blend
    const card1 = document.createElement("div");
    card1.className = "ls-card";
    card1.innerHTML = `
      <div class="form-field">
        <label>Layer Name</label>
        <input id="ls-name-input" class="ls-input" type="text" value="${escapeHtml(layer.name)}" />
      </div>
      <div class="form-field">
        <label>Blend Mode</label>
        <select id="ls-blend-select" class="ls-select">
          <option value="source-over" ${layer.blendMode === "source-over" ? "selected" : ""}>Normal</option>
          <option value="screen" ${layer.blendMode === "screen" ? "selected" : ""}>Screen</option>
          <option value="lighter" ${layer.blendMode === "lighter" ? "selected" : ""}>Linear Dodge (Add)</option>
          <option value="multiply" ${layer.blendMode === "multiply" ? "selected" : ""}>Multiply</option>
          <option value="overlay" ${layer.blendMode === "overlay" ? "selected" : ""}>Overlay</option>
        </select>
      </div>
    `;
    card1.querySelector("#ls-name-input").addEventListener("change", (e) => {
      pushUndo("Rename layer");
      layer.name = e.target.value.trim() || "Layer";
      saveProjects();
      refreshEditorAll();
    });
    card1.querySelector("#ls-blend-select").addEventListener("change", (e) => {
      pushUndo("Change blend mode");
      layer.blendMode = e.target.value;
      saveProjects();
      renderActivePreview();
    });
    body.appendChild(card1);

    // Card 2: Content & Color
    const card2 = document.createElement("div");
    card2.className = "ls-card";
    card2.innerHTML = `
      ${layer.type === "text" ? `
      <div class="form-field">
        <label>Text Content</label>
        <input id="ls-text-input" class="ls-input" type="text" value="${escapeHtml(layer.text || "")}" />
      </div>` : ""}
      <div class="form-row-2">
        <div class="form-field">
          <label>Primary Color</label>
          <input id="ls-color-input" class="ls-input" type="color" value="${layer.color || "#2979FF"}" />
        </div>
        <div class="form-field">
          <label>Lock Layer</label>
          <button id="ls-lock-btn" class="mini-pill-btn ${layer.locked ? "primary" : ""}" type="button">${layer.locked ? "Locked" : "Unlocked"}</button>
        </div>
      </div>
      <div class="bitrate-presets" style="margin-top:4px;">
        <button id="ls-dup-btn" class="mini-pill-btn" type="button">Duplicate</button>
        <button id="ls-up-btn" class="mini-pill-btn" type="button">Bring Forward</button>
        <button id="ls-del-btn" class="mini-pill-btn" style="color:var(--accent-crimson);border-color:rgba(255,61,113,0.4);" type="button">Delete</button>
      </div>
    `;
    if (card2.querySelector("#ls-text-input")) {
      card2.querySelector("#ls-text-input").addEventListener("input", (e) => {
        layer.text = e.target.value;
        saveProjects();
        renderActivePreview();
      });
    }
    card2.querySelector("#ls-color-input").addEventListener("input", (e) => {
      layer.color = e.target.value;
      saveProjects();
      renderActivePreview();
    });
    card2.querySelector("#ls-lock-btn").addEventListener("click", () => {
      layer.locked = !layer.locked;
      saveProjects();
      renderLayerSettingsDrawer();
    });
    card2.querySelector("#ls-dup-btn").addEventListener("click", () => {
      pushUndo("Duplicate layer");
      const copy = structuredClone(layer);
      copy.id = "lyr_" + Math.random().toString(36).slice(2, 8);
      copy.name = `${layer.name} Copy`;
      proj.layers.push(copy);
      state.selectedLayerId = copy.id;
      saveProjects();
      refreshEditorAll();
      renderLayerSettingsDrawer();
      showToast("Layer duplicated");
    });
    card2.querySelector("#ls-up-btn").addEventListener("click", () => {
      const idx = proj.layers.findIndex((l) => l.id === layer.id);
      if (idx < proj.layers.length - 1) {
        pushUndo("Reorder layer");
        [proj.layers[idx], proj.layers[idx + 1]] = [proj.layers[idx + 1], proj.layers[idx]];
        saveProjects();
        refreshEditorAll();
      }
    });
    card2.querySelector("#ls-del-btn").addEventListener("click", () => {
      if (proj.layers.length <= 1) {
        showToast("Keep at least 1 layer in project");
        return;
      }
      pushUndo("Delete layer");
      proj.layers = proj.layers.filter((l) => l.id !== layer.id);
      state.selectedLayerId = proj.layers[0].id;
      saveProjects();
      closeDrawers();
      refreshEditorAll();
      showToast("Layer removed");
    });
    body.appendChild(card2);
  }

  // --------------------------------------------------------------------------
  // 9. ADDING LAYERS & VECTOR DRAWING
  // --------------------------------------------------------------------------
  function addNewLayerToProject(overrides) {
    const proj = getActiveProject();
    if (!proj) return;
    pushUndo("Add layer");
    const layer = makeLayer(Object.assign({
      start: Number(Math.min(proj.duration - 1.0, state.playhead).toFixed(2)),
      duration: Number(Math.max(1.2, Math.min(4.0, proj.duration - state.playhead)).toFixed(2)),
    }, overrides));
    proj.layers.push(layer);
    state.selectedLayerId = layer.id;
    saveProjects();
    refreshEditorAll();
    showToast(`Added ${layer.name}`);
  }

  function setupVectorDrawingTool() {
    const overlay = document.getElementById("drawing-overlay-canvas");
    const bar = document.getElementById("drawing-toolbar");
    let activeStroke = null;
    let currentStrokes = [];

    function syncOverlaySize() {
      const r = overlay.getBoundingClientRect();
      overlay.width = Math.max(160, Math.round(r.width));
      overlay.height = Math.max(160, Math.round(r.height));
      redrawOverlay();
    }

    function redrawOverlay() {
      const ctx = overlay.getContext("2d");
      ctx.clearRect(0, 0, overlay.width, overlay.height);
      ctx.lineCap = "round";
      ctx.lineJoin = "round";
      currentStrokes.forEach((s) => {
        if (s.points.length < 2) return;
        ctx.strokeStyle = s.color;
        ctx.lineWidth = s.size;
        ctx.beginPath();
        s.points.forEach((p, i) => {
          const x = p.x * overlay.width, y = p.y * overlay.height;
          if (i === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        });
        ctx.stroke();
      });
    }

    window.activateDrawingMode = () => {
      closeDrawers();
      state.drawingMode = true;
      currentStrokes = [];
      overlay.classList.remove("hidden");
      bar.classList.remove("hidden");
      syncOverlaySize();
      showToast("Draw directly on preview canvas");
    };

    overlay.addEventListener("pointerdown", (e) => {
      if (!state.drawingMode) return;
      const r = overlay.getBoundingClientRect();
      activeStroke = {
        color: document.getElementById("draw-color-input").value,
        size: Number(document.getElementById("draw-size-input").value),
        points: [{ x: (e.clientX - r.left) / r.width, y: (e.clientY - r.top) / r.height }],
      };
      currentStrokes.push(activeStroke);
      overlay.setPointerCapture(e.pointerId);
    });

    overlay.addEventListener("pointermove", (e) => {
      if (!activeStroke) return;
      const r = overlay.getBoundingClientRect();
      activeStroke.points.push({ x: (e.clientX - r.left) / r.width, y: (e.clientY - r.top) / r.height });
      redrawOverlay();
    });

    overlay.addEventListener("pointerup", () => { activeStroke = null; });

    document.getElementById("btn-draw-clear").addEventListener("click", () => {
      currentStrokes = [];
      redrawOverlay();
    });

    document.getElementById("btn-draw-done").addEventListener("click", () => {
      state.drawingMode = false;
      overlay.classList.add("hidden");
      bar.classList.add("hidden");
      if (currentStrokes.length > 0) {
        addNewLayerToProject({
          name: "Vector Brush Stroke",
          type: "drawing",
          strokes: currentStrokes,
        });
      }
    });
  }

  // --------------------------------------------------------------------------
  // 10. EXPORT SCREEN & HARDWARE CODEC DIAGNOSTICS
  // --------------------------------------------------------------------------
  function getNativeDiagnostics() {
    if (window.AndroidBridge && typeof window.AndroidBridge.getDeviceDiagnosticsJson === "function") {
      try {
        return JSON.parse(window.AndroidBridge.getDeviceDiagnosticsJson());
      } catch (_) { /* ignore */ }
    }
    return {
      runtime: "Chrome / Web Browser",
      abis: ["arm64-v8a (Target APK)"],
      isArm64Ready: true,
      hardwareEncoders: ["Prototype Browser Preview (Native MediaCodec available in APK)"],
    };
  }

  function refreshExportScreen() {
    const proj = getActiveProject();
    if (!proj) return;
    document.getElementById("export-project-subtitle").textContent =
      `${proj.name} · ${proj.aspectRatio} · ${formatTimecode(proj.duration, proj.fps)}`;
    state.exportConfig.resolution = proj.resolution || "1080p";
    state.exportConfig.fps = proj.fps || 60;
    syncExportOptionUI();

    const diag = getNativeDiagnostics();
    document.getElementById("codec-abi-tag").textContent = (diag.abis && diag.abis[0]) || "arm64-v8a";
    document.getElementById("codec-summary-text").textContent =
      `${diag.runtime} · Encoders: ${(diag.hardwareEncoders || []).join(", ")}`;
  }

  function syncExportOptionUI() {
    const cfg = state.exportConfig;
    document.querySelectorAll("#export-res-options .seg-pill").forEach((b) =>
      b.classList.toggle("active", b.dataset.res === cfg.resolution));
    document.querySelectorAll("#export-fps-options .seg-pill").forEach((b) =>
      b.classList.toggle("active", Number(b.dataset.fps) === cfg.fps));
    document.querySelectorAll("#export-format-options .seg-pill").forEach((b) =>
      b.classList.toggle("active", b.dataset.format === cfg.format));

    document.getElementById("export-bitrate-slider").value = String(cfg.bitrate);
    document.getElementById("export-bitrate-value").textContent = `${cfg.bitrate} Mbps`;

    const proj = getActiveProject();
    const dur = proj ? proj.duration : 8;
    const estMb = ((cfg.bitrate * dur) / 8).toFixed(1);
    document.getElementById("export-size-estimate").textContent = `~${estMb} MB`;
  }

  function startSimulatedExport() {
    const proj = getActiveProject();
    if (!proj) return;
    const card = document.getElementById("export-progress-card");
    const fill = document.getElementById("export-progress-fill");
    const pctText = document.getElementById("export-percent-text");
    const frameText = document.getElementById("export-frame-counter");
    const statusTitle = document.getElementById("export-status-title");
    const completeBox = document.getElementById("export-complete-actions");
    const btnStart = document.getElementById("btn-start-export");
    const btnCancel = document.getElementById("btn-cancel-export");

    card.classList.remove("hidden");
    completeBox.classList.add("hidden");
    btnStart.classList.add("hidden");
    btnCancel.classList.remove("hidden");

    const totalFrames = Math.max(24, Math.round(proj.duration * state.exportConfig.fps));
    let progress = 0;
    statusTitle.textContent = `Simulating ${state.exportConfig.resolution} ${state.exportConfig.format} Render...`;

    clearInterval(state.exportTimer);
    state.exportTimer = setInterval(() => {
      progress = Math.min(100, progress + 3.5);
      const curFrame = Math.round((progress / 100) * totalFrames);
      fill.style.width = `${progress}%`;
      pctText.textContent = `${Math.round(progress)}%`;
      frameText.textContent = `Frame ${curFrame} / ${totalFrames}`;

      if (progress >= 100) {
        clearInterval(state.exportTimer);
        state.exportTimer = null;
        statusTitle.textContent = "Simulated Render Pass Complete (Prototype)";
        btnCancel.classList.add("hidden");
        btnStart.classList.remove("hidden");
        completeBox.classList.remove("hidden");
        showToast("Simulated render complete — snapshot & JSON ready");
      }
    }, 65);
  }

  function cancelSimulatedExport() {
    clearInterval(state.exportTimer);
    state.exportTimer = null;
    document.getElementById("export-progress-card").classList.add("hidden");
    document.getElementById("btn-cancel-export").classList.add("hidden");
    document.getElementById("btn-start-export").classList.remove("hidden");
    showToast("Simulated render cancelled");
  }

  // --------------------------------------------------------------------------
  // 11. JAVASCRIPT ANIMATION STUDIO & REAL ZIP IMPORT / EXPORT
  // --------------------------------------------------------------------------
  function renderJsStudioFiles() {
    const list = document.getElementById("js-file-list");
    const files = state.jsStudio.files;
    list.innerHTML = "";
    Object.keys(files).forEach((fname) => {
      const li = document.createElement("li");
      li.className = `js-file-item ${state.jsStudio.activeFile === fname ? "active" : ""}`;
      li.textContent = fname;
      li.addEventListener("click", () => {
        state.jsStudio.activeFile = fname;
        renderJsStudioFiles();
      });
      list.appendChild(li);
    });

    const active = state.jsStudio.activeFile;
    document.getElementById("js-active-filename").textContent = active;
    const ext = active.split(".").pop().toUpperCase();
    document.getElementById("js-lang-badge").textContent = ext;

    const codeArea = document.getElementById("js-code-input");
    codeArea.value = files[active] || "";
    updateCodeLineNumbers();
  }

  function updateCodeLineNumbers() {
    const code = document.getElementById("js-code-input").value;
    const count = code.split("\n").length;
    document.getElementById("js-line-numbers").textContent =
      Array.from({ length: count }, (_, i) => i + 1).join("\n");
  }

  function buildSandboxedSrcdoc() {
    const files = state.jsStudio.files;
    let html = files["index.html"] || "<html><body></body></html>";
    const css = files["style.css"] || "";
    const js = files["app.js"] || "";

    const consoleShim = `<script>
      (function(){
        const send = (lvl, args) => parent.postMessage({ __kytheraConsole: true, lvl, text: args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ') }, '*');
        ['log','warn','error'].forEach(l => {
          const orig = console[l];
          console[l] = function(...a){ send(l, a); orig.apply(console, a); };
        });
        window.onerror = (msg, src, line) => send('error', [msg + ' (line ' + line + ')']);
      })();
    <\/script>`;

    html = html.replace(/<link[^>]*style\.css[^>]*>/i, `<style>${css}</style>`);
    html = html.replace(/<script[^>]*app\.js[^>]*><\/script>/i, `<script>${js}<\/script>`);
    if (!html.includes("<style>")) html = html.replace("</head>", `<style>${css}</style></head>`);
    if (!html.includes(js) && js.trim()) html = html.replace("</body>", `<script>${js}<\/script></body>`);
    return consoleShim + html;
  }

  function runJsStudioPreview() {
    const frame = document.getElementById("js-preview-frame");
    frame.srcdoc = buildSandboxedSrcdoc();
  }

  function appendConsoleLine(lvl, text) {
    const out = document.getElementById("js-console-output");
    const line = document.createElement("div");
    line.className = `console-line ${lvl}`;
    line.textContent = `[${lvl.toUpperCase()}] ${text}`;
    out.appendChild(line);
    out.scrollTop = out.scrollHeight;
  }

  // Pure JS CRC32 + STORE ZIP Builder (works offline in Chrome & Android!)
  function createZipBlob(filesMap) {
    const enc = new TextEncoder();
    const crcTable = new Uint32Array(256);
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
      crcTable[n] = c >>> 0;
    }
    const crc32 = (bytes) => {
      let c = 0xFFFFFFFF;
      for (let i = 0; i < bytes.length; i++) c = crcTable[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8);
      return (c ^ 0xFFFFFFFF) >>> 0;
    };

    const localParts = [];
    const centralParts = [];
    let offset = 0;

    for (const [name, content] of Object.entries(filesMap)) {
      const nameBytes = enc.encode(name);
      const dataBytes = enc.encode(content);
      const crc = crc32(dataBytes);

      const lh = new Uint8Array(30 + nameBytes.length);
      const lv = new DataView(lh.buffer);
      lv.setUint32(0, 0x04034b50, true);
      lv.setUint16(4, 20, true);
      lv.setUint16(8, 0, true); // STORE
      lv.setUint32(14, crc, true);
      lv.setUint32(18, dataBytes.length, true);
      lv.setUint32(22, dataBytes.length, true);
      lv.setUint16(26, nameBytes.length, true);
      lh.set(nameBytes, 30);

      localParts.push(lh, dataBytes);

      const ch = new Uint8Array(46 + nameBytes.length);
      const cv = new DataView(ch.buffer);
      cv.setUint32(0, 0x02014b50, true);
      cv.setUint16(4, 20, true);
      cv.setUint16(6, 20, true);
      cv.setUint16(10, 0, true);
      cv.setUint32(16, crc, true);
      cv.setUint32(20, dataBytes.length, true);
      cv.setUint32(24, dataBytes.length, true);
      cv.setUint16(28, nameBytes.length, true);
      cv.setUint32(42, offset, true);
      ch.set(nameBytes, 46);

      centralParts.push(ch);
      offset += lh.length + dataBytes.length;
    }

    const centralSize = centralParts.reduce((s, p) => s + p.length, 0);
    const eocd = new Uint8Array(22);
    const ev = new DataView(eocd.buffer);
    ev.setUint32(0, 0x06054b50, true);
    ev.setUint16(8, Object.keys(filesMap).length, true);
    ev.setUint16(10, Object.keys(filesMap).length, true);
    ev.setUint32(12, centralSize, true);
    ev.setUint32(16, offset, true);

    return new Blob([...localParts, ...centralParts, eocd], { type: "application/zip" });
  }

  async function parseZipArrayBuffer(buf) {
    const view = new DataView(buf);
    const bytes = new Uint8Array(buf);
    const dec = new TextDecoder();
    const out = {};
    let i = 0;
    while (i + 30 <= bytes.length) {
      if (view.getUint32(i, true) !== 0x04034b50) break;
      const comp = view.getUint16(i + 8, true);
      const compSize = view.getUint32(i + 18, true);
      const nameLen = view.getUint16(i + 26, true);
      const extraLen = view.getUint16(i + 28, true);
      const name = dec.decode(bytes.slice(i + 30, i + 30 + nameLen));
      const dataStart = i + 30 + nameLen + extraLen;
      const slice = bytes.slice(dataStart, dataStart + compSize);
      if (!name.endsWith("/")) {
        if (comp === 0) {
          out[name] = dec.decode(slice);
        } else if (comp === 8 && typeof DecompressionStream !== "undefined") {
          const ds = new DecompressionStream("deflate-raw");
          const text = await new Response(new Blob([slice]).stream().pipeThrough(ds)).text();
          out[name] = text;
        }
      }
      i = dataStart + compSize;
    }
    return out;
  }

  function triggerZipImport() {
    if (window.AndroidBridge && typeof window.AndroidBridge.importZipProject === "function") {
      window.AndroidBridge.importZipProject();
    } else {
      document.getElementById("input-import-zip").click();
    }
  }

  function triggerZipExport() {
    const files = state.jsStudio.files;
    if (window.AndroidBridge && typeof window.AndroidBridge.exportZipProject === "function") {
      window.AndroidBridge.exportZipProject("kythera-js-animation.zip", JSON.stringify(files));
    } else {
      const blob = createZipBlob(files);
      downloadBlob(blob, "kythera-js-animation.zip");
      showToast("Exported kythera-js-animation.zip");
    }
  }

  function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  // --------------------------------------------------------------------------
  // 12. MODALS (NEW PROJECT, PROJECT SETTINGS, SHAPES, DIAGNOSTICS)
  // --------------------------------------------------------------------------
  function openModal(title, renderFn) {
    document.getElementById("modal-title").textContent = title;
    const body = document.getElementById("modal-body");
    body.innerHTML = "";
    renderFn(body);
    document.getElementById("modal-backdrop").classList.remove("hidden");
  }

  function closeModal() {
    document.getElementById("modal-backdrop").classList.add("hidden");
  }

  function openNewProjectModal() {
    openModal("New Motion Project", (body) => {
      body.innerHTML = `
        <div class="form-field">
          <label>Project Name</label>
          <input id="np-name" class="ls-input" type="text" value="Kinetic Motion ${state.projects.length + 1}" />
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Aspect Ratio</label>
            <select id="np-aspect" class="ls-select">
              <option value="9:16">9:16 (Reels / Shorts)</option>
              <option value="16:9">16:9 (Widescreen)</option>
              <option value="1:1">1:1 (Square)</option>
              <option value="4:5">4:5 (Portrait Feed)</option>
            </select>
          </div>
          <div class="form-field">
            <label>Resolution</label>
            <select id="np-res" class="ls-select">
              <option value="720p">720p HD</option>
              <option value="1080p" selected>1080p FHD</option>
              <option value="1440p">1440p QHD</option>
              <option value="4K">4K UHD</option>
            </select>
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Frame Rate</label>
            <select id="np-fps" class="ls-select">
              <option value="24">24 FPS</option>
              <option value="30">30 FPS</option>
              <option value="60" selected>60 FPS</option>
            </select>
          </div>
          <div class="form-field">
            <label>Duration (sec)</label>
            <input id="np-dur" class="ls-input" type="number" min="2" max="60" value="8" />
          </div>
        </div>
        <button id="np-create-btn" class="primary-cta-btn" type="button">Create &amp; Open Editor</button>
      `;
      body.querySelector("#np-create-btn").addEventListener("click", () => {
        const name = body.querySelector("#np-name").value.trim() || "Untitled Motion";
        const aspectRatio = body.querySelector("#np-aspect").value;
        const resolution = body.querySelector("#np-res").value;
        const fps = Number(body.querySelector("#np-fps").value);
        const duration = Math.max(2, Math.min(60, Number(body.querySelector("#np-dur").value) || 8));

        const newProj = {
          id: "proj_" + Date.now().toString(36),
          name,
          resolution,
          aspectRatio,
          fps,
          duration,
          bgColor: "#070B14",
          updatedAt: "Just now",
          layers: [
            makeLayer({ name: "Background Plate", type: "video", duration, color: "#11244A" }),
            makeLayer({ name: "Hero Ring", type: "shape", shapeKind: "ring", start: 0.2, duration: duration - 0.5 }),
            makeLayer({ name: "Title Layer", type: "text", text: name.toUpperCase().slice(0, 14), fontSize: 40, start: 0.4, duration: duration - 0.8 }),
          ],
        };
        state.projects.unshift(newProj);
        saveProjects();
        closeModal();
        openProjectInEditor(newProj.id);
      });
    });
  }

  function openProjectSettingsModal(projectId) {
    const proj = state.projects.find((p) => p.id === projectId);
    if (!proj) return;
    openModal("Project Settings", (body) => {
      body.innerHTML = `
        <div class="form-field">
          <label>Project Name</label>
          <input id="ps-name" class="ls-input" type="text" value="${escapeHtml(proj.name)}" />
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Resolution</label>
            <select id="ps-res" class="ls-select">
              <option value="720p" ${proj.resolution === "720p" ? "selected" : ""}>720p</option>
              <option value="1080p" ${proj.resolution === "1080p" ? "selected" : ""}>1080p</option>
              <option value="1440p" ${proj.resolution === "1440p" ? "selected" : ""}>1440p</option>
              <option value="4K" ${proj.resolution === "4K" ? "selected" : ""}>4K</option>
            </select>
          </div>
          <div class="form-field">
            <label>Frame Rate</label>
            <select id="ps-fps" class="ls-select">
              <option value="24" ${proj.fps === 24 ? "selected" : ""}>24 FPS</option>
              <option value="30" ${proj.fps === 30 ? "selected" : ""}>30 FPS</option>
              <option value="60" ${proj.fps === 60 ? "selected" : ""}>60 FPS</option>
            </select>
          </div>
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Aspect Ratio</label>
            <select id="ps-aspect" class="ls-select">
              <option value="9:16" ${proj.aspectRatio === "9:16" ? "selected" : ""}>9:16</option>
              <option value="16:9" ${proj.aspectRatio === "16:9" ? "selected" : ""}>16:9</option>
              <option value="1:1" ${proj.aspectRatio === "1:1" ? "selected" : ""}>1:1</option>
              <option value="4:5" ${proj.aspectRatio === "4:5" ? "selected" : ""}>4:5</option>
            </select>
          </div>
          <div class="form-field">
            <label>Canvas Color</label>
            <input id="ps-bg" class="ls-input" type="color" value="${proj.bgColor || "#070B14"}" />
          </div>
        </div>
        <button id="ps-save-btn" class="primary-cta-btn" type="button">Save Settings</button>
      `;
      body.querySelector("#ps-save-btn").addEventListener("click", () => {
        proj.name = body.querySelector("#ps-name").value.trim() || proj.name;
        proj.resolution = body.querySelector("#ps-res").value;
        proj.fps = Number(body.querySelector("#ps-fps").value);
        proj.aspectRatio = body.querySelector("#ps-aspect").value;
        proj.bgColor = body.querySelector("#ps-bg").value;
        saveProjects();
        closeModal();
        if (state.currentScreen === "home") renderHomeProjects();
        else refreshEditorAll();
        showToast("Project settings updated");
      });
    });
  }

  function openShapePickerModal() {
    openModal("Add Vector Shape Layer", (body) => {
      const shapes = [
        { kind: "ring", label: "Neon Halo Ring", color: "#2979FF" },
        { kind: "circle", label: "Gradient Orb", color: "#00E5FF" },
        { kind: "star", label: "5-Point Star", color: "#FFB300" },
        { kind: "hex", label: "Hexagon Frame", color: "#FF3D71" },
        { kind: "rect", label: "Rounded Card", color: "#B388FF" },
      ];
      shapes.forEach((s) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "action-card";
        btn.innerHTML = `<span class="track-type-dot" style="width:14px;height:14px;background:${s.color}"></span><span class="action-title">${s.label}</span>`;
        btn.addEventListener("click", () => {
          closeModal();
          addNewLayerToProject({ name: s.label, type: "shape", shapeKind: s.kind, color: s.color });
        });
        body.appendChild(btn);
      });
    });
  }

  function openDiagnosticsModal() {
    const diag = getNativeDiagnostics();
    openModal("Native Media & ARM64 Diagnostics", (body) => {
      body.innerHTML = `
        <div class="ls-card">
          <div class="card-row-between"><span>Runtime Environment</span><strong>${escapeHtml(diag.runtime || "Web")}</strong></div>
          <div class="card-row-between"><span>Supported ABIs</span><strong style="color:var(--accent-cyan)">${escapeHtml((diag.abis || []).join(", "))}</strong></div>
          <div class="card-row-between"><span>ARM64 Ready</span><strong>${diag.isArm64Ready ? "YES" : "Fallback"}</strong></div>
        </div>
        <div class="ls-card">
          <label style="font-size:11px;color:var(--text-secondary);font-weight:700;">Hardware MediaCodec Encoders</label>
          <p style="margin:0;font-family:var(--font-mono);font-size:11px;color:var(--text-primary);">${escapeHtml((diag.hardwareEncoders || []).join("\n"))}</p>
        </div>
        <p style="margin:0;font-size:11.5px;color:var(--text-secondary);">
          Interactive UI prototype mode: timeline compositing runs in real time on HTML5 Canvas; full video muxing is architected via Kotlin MediaEngineGateway for future FFmpeg / MediaCodec integration.
        </p>
      `;
    });
  }

  // --------------------------------------------------------------------------
  // 13. NATIVE ANDROID BRIDGE CALLBACKS
  // --------------------------------------------------------------------------
  window.__kytheraNativeMediaPicked = (payloadJson) => {
    try {
      const payload = typeof payloadJson === "string" ? JSON.parse(payloadJson) : payloadJson;
      addNewLayerToProject({
        name: payload.name || "Picked Media",
        type: payload.isVideo ? "video" : "image",
        imageData: payload.dataUrl || null,
      });
    } catch (_) {
      showToast("Could not load picked media");
    }
  };

  window.__kytheraNativeZipImported = (filesJson) => {
    try {
      const files = typeof filesJson === "string" ? JSON.parse(filesJson) : filesJson;
      if (Object.keys(files).length === 0) {
        showToast("ZIP archive contained no readable text/web files");
        return;
      }
      state.jsStudio.files = files;
      state.jsStudio.activeFile = Object.keys(files)[0];
      navigateTo("js-studio");
      showToast(`Imported ${Object.keys(files).length} files from ZIP`);
    } catch (_) {
      showToast("Invalid ZIP payload");
    }
  };

  window.__kytheraNativeProjectImported = (projectJson) => {
    try {
      const proj = JSON.parse(projectJson);
      if (!proj || !Array.isArray(proj.layers)) throw new Error("Invalid project");
      proj.id = "proj_" + Date.now().toString(36);
      state.projects.unshift(proj);
      saveProjects();
      openProjectInEditor(proj.id);
      showToast(`Imported ${proj.name}`);
    } catch (_) {
      showToast("Invalid .kythera.json project file");
    }
  };

  window.kytheraHandleBack = () => {
    if (!document.getElementById("modal-backdrop").classList.contains("hidden")) {
      closeModal();
      return true;
    }
    if (state.activeDrawer) {
      closeDrawers();
      return true;
    }
    if (state.currentScreen === "export" || state.currentScreen === "js-studio") {
      navigateTo(state.previousScreen === "editor" ? "editor" : "home");
      return true;
    }
    if (state.currentScreen === "editor") {
      navigateTo("home");
      return true;
    }
    return false;
  };

  // --------------------------------------------------------------------------
  // 14. EVENT WIRING & INITIALIZATION
  // --------------------------------------------------------------------------
  function initInteractions() {
    // Home actions
    document.getElementById("btn-new-project").addEventListener("click", openNewProjectModal);
    document.getElementById("btn-continue-last").addEventListener("click", () => {
      const proj = getActiveProject();
      if (proj) openProjectInEditor(proj.id);
    });
    document.getElementById("btn-open-js-studio").addEventListener("click", () => navigateTo("js-studio"));
    document.getElementById("btn-import-zip").addEventListener("click", triggerZipImport);
    document.getElementById("btn-import-project").addEventListener("click", () => {
      if (window.AndroidBridge && typeof window.AndroidBridge.importKytheraProject === "function") {
        window.AndroidBridge.importKytheraProject();
      } else {
        document.getElementById("input-import-project").click();
      }
    });
    document.getElementById("btn-engine-diagnostics").addEventListener("click", openDiagnosticsModal);
    document.getElementById("btn-home-settings").addEventListener("click", openDiagnosticsModal);

    // Editor Top Bar
    document.getElementById("btn-editor-back").addEventListener("click", () => navigateTo("home"));
    document.getElementById("btn-editor-project-settings").addEventListener("click", () => openProjectSettingsModal(state.activeProjectId));
    document.getElementById("btn-undo").addEventListener("click", undo);
    document.getElementById("btn-redo").addEventListener("click", redo);
    document.getElementById("btn-preview-quality").addEventListener("click", () => {
      const order = ["FULL", "HALF", "DRAFT"];
      state.previewQuality = order[(order.indexOf(state.previewQuality) + 1) % order.length];
      document.getElementById("preview-quality-label").textContent = state.previewQuality;
      renderActivePreview();
    });
    document.getElementById("btn-editor-js-studio").addEventListener("click", () => navigateTo("js-studio"));
    document.getElementById("btn-open-export").addEventListener("click", () => navigateTo("export"));

    // Transport Controls
    document.getElementById("btn-play-pause").addEventListener("click", () => togglePlayback());
    document.getElementById("btn-step-back").addEventListener("click", () => {
      const proj = getActiveProject();
      setPlayhead(state.playhead - 1 / (proj ? proj.fps : 60));
    });
    document.getElementById("btn-step-fwd").addEventListener("click", () => {
      const proj = getActiveProject();
      setPlayhead(state.playhead + 1 / (proj ? proj.fps : 60));
    });
    document.getElementById("btn-add-keyframe-quick").addEventListener("click", () => toggleKeyframeAtPlayhead());
    document.getElementById("btn-split-layer").addEventListener("click", () => {
      const layer = getSelectedLayer();
      const proj = getActiveProject();
      if (!layer || !proj) return;
      if (state.playhead <= layer.start + 0.15 || state.playhead >= layer.start + layer.duration - 0.15) {
        showToast("Move playhead inside the selected clip to split");
        return;
      }
      pushUndo("Split layer");
      const splitOffset = Number((state.playhead - layer.start).toFixed(2));
      const rightPart = structuredClone(layer);
      rightPart.id = "lyr_" + Math.random().toString(36).slice(2, 8);
      rightPart.name = `${layer.name} (2)`;
      rightPart.start = Number(state.playhead.toFixed(2));
      rightPart.duration = Number((layer.duration - splitOffset).toFixed(2));
      layer.duration = splitOffset;
      proj.layers.push(rightPart);
      saveProjects();
      refreshEditorAll();
      showToast("Split clip at playhead");
    });
    document.getElementById("btn-aspect-cycle").addEventListener("click", () => {
      const proj = getActiveProject();
      const keys = Object.keys(ASPECT_MAP);
      proj.aspectRatio = keys[(keys.indexOf(proj.aspectRatio) + 1) % keys.length];
      saveProjects();
      refreshEditorAll();
    });
    document.getElementById("btn-toggle-guides").addEventListener("click", () => {
      state.showGuides = !state.showGuides;
      document.getElementById("safe-guides-overlay").classList.toggle("hidden", !state.showGuides);
      document.getElementById("btn-toggle-guides").classList.toggle("active", state.showGuides);
    });

    // Ruler Scrubbing
    const ruler = document.getElementById("timeline-ruler");
    let scrubbing = false;
    const scrubFromEvent = (e) => {
      const scrollBox = document.getElementById("timeline-scroll");
      const r = scrollBox.getBoundingClientRect();
      const x = e.clientX - r.left + scrollBox.scrollLeft;
      setPlayhead(x / state.timelineZoom);
    };
    ruler.addEventListener("pointerdown", (e) => {
      scrubbing = true;
      ruler.setPointerCapture(e.pointerId);
      scrubFromEvent(e);
    });
    ruler.addEventListener("pointermove", (e) => { if (scrubbing) scrubFromEvent(e); });
    ruler.addEventListener("pointerup", () => { scrubbing = false; });

    // Timeline Zoom
    const zoomSlider = document.getElementById("timeline-zoom-slider");
    const applyZoom = (val) => {
      state.timelineZoom = Math.max(40, Math.min(220, val));
      zoomSlider.value = String(state.timelineZoom);
      renderTimeline();
    };
    zoomSlider.addEventListener("input", () => applyZoom(Number(zoomSlider.value)));
    document.getElementById("btn-zoom-in").addEventListener("click", () => applyZoom(state.timelineZoom + 20));
    document.getElementById("btn-zoom-out").addEventListener("click", () => applyZoom(state.timelineZoom - 20));
    document.getElementById("btn-zoom-fit").addEventListener("click", () => {
      const proj = getActiveProject();
      const scrollW = document.getElementById("timeline-scroll").clientWidth || 300;
      applyZoom(Math.round((scrollW - 30) / Math.max(2, proj.duration)));
    });

    // Workspace Splitter Drag
    const splitter = document.getElementById("workspace-splitter");
    const previewPane = document.getElementById("preview-pane");
    const workspace = document.getElementById("editor-workspace");
    let resizingPane = false;
    splitter.addEventListener("pointerdown", (e) => {
      resizingPane = true;
      splitter.setPointerCapture(e.pointerId);
    });
    splitter.addEventListener("pointermove", (e) => {
      if (!resizingPane) return;
      const wr = workspace.getBoundingClientRect();
      const pct = Math.max(24, Math.min(68, ((e.clientY - wr.top) / wr.height) * 100));
      previewPane.style.height = `${pct.toFixed(1)}%`;
      renderActivePreview();
    });
    splitter.addEventListener("pointerup", () => { resizingPane = false; });

    // Bottom Toolbar
    document.querySelectorAll(".dock-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        const tool = btn.dataset.tool;
        if (tool === "media") {
          if (window.AndroidBridge && typeof window.AndroidBridge.pickMedia === "function") {
            window.AndroidBridge.pickMedia("image_and_video");
          } else {
            document.getElementById("input-pick-media").click();
          }
        } else if (tool === "audio") {
          addNewLayerToProject({ name: "Audio_Track.wav", type: "audio", color: "#00E676" });
        } else if (tool === "text") {
          addNewLayerToProject({ name: "Title Text", type: "text", text: "MOTION", fontSize: 46, color: "#FFFFFF" });
        } else if (tool === "shape") {
          openShapePickerModal();
        } else if (tool === "drawing") {
          window.activateDrawingMode();
        } else if (tool === "effects" || tool === "animation" || tool === "layer") {
          if (state.activeDrawer === tool) closeDrawers();
          else openDrawer(tool);
        }
      });
    });

    // Animation Drawer events
    document.getElementById("btn-anim-close").addEventListener("click", closeDrawers);
    document.getElementById("btn-fx-close").addEventListener("click", closeDrawers);
    document.getElementById("btn-ls-close").addEventListener("click", closeDrawers);
    document.getElementById("btn-anim-toggle-kf").addEventListener("click", () => toggleKeyframeAtPlayhead());

    document.querySelectorAll(".prop-tab").forEach((tab) => {
      tab.addEventListener("click", () => {
        document.querySelectorAll(".prop-tab").forEach((t) => t.classList.remove("active"));
        tab.classList.add("active");
        state.activeAnimProp = tab.dataset.prop;
        renderAnimationDrawer();
      });
    });

    document.querySelectorAll(".easing-chip").forEach((chip) => {
      chip.addEventListener("click", () => {
        const layer = getSelectedLayer();
        if (!layer) return;
        const preset = EASING_PRESETS[chip.dataset.easing];
        if (!preset) return;
        pushUndo("Change easing");
        layer.easing = { ...preset };
        document.querySelectorAll(".easing-chip").forEach((c) => c.classList.remove("active"));
        chip.classList.add("active");
        saveProjects();
        drawEasingGraph(layer.easing);
        renderActivePreview();
      });
    });

    document.getElementById("btn-kf-prev").addEventListener("click", () => {
      const layer = getSelectedLayer();
      const times = collectLayerKeyframeTimes(layer);
      const prev = [...times].reverse().find((t) => t < state.playhead - 0.05);
      if (prev !== undefined) setPlayhead(prev);
    });
    document.getElementById("btn-kf-next").addEventListener("click", () => {
      const layer = getSelectedLayer();
      const times = collectLayerKeyframeTimes(layer);
      const next = times.find((t) => t > state.playhead + 0.05);
      if (next !== undefined) setPlayhead(next);
    });

    // Effects search
    document.getElementById("fx-search-input").addEventListener("input", (e) => {
      state.fxSearch = e.target.value;
      renderEffectsDrawer();
    });

    // Export Screen events
    document.getElementById("btn-export-back").addEventListener("click", () => navigateTo("editor"));
    document.querySelectorAll("#export-res-options .seg-pill").forEach((b) =>
      b.addEventListener("click", () => { state.exportConfig.resolution = b.dataset.res; syncExportOptionUI(); }));
    document.querySelectorAll("#export-fps-options .seg-pill").forEach((b) =>
      b.addEventListener("click", () => { state.exportConfig.fps = Number(b.dataset.fps); syncExportOptionUI(); }));
    document.querySelectorAll("#export-format-options .seg-pill").forEach((b) =>
      b.addEventListener("click", () => { state.exportConfig.format = b.dataset.format; syncExportOptionUI(); }));
    document.getElementById("export-bitrate-slider").addEventListener("input", (e) => {
      state.exportConfig.bitrate = Number(e.target.value);
      syncExportOptionUI();
    });
    document.querySelectorAll(".bitrate-presets .mini-pill-btn").forEach((b) =>
      b.addEventListener("click", () => { state.exportConfig.bitrate = Number(b.dataset.bitrate); syncExportOptionUI(); }));
    document.getElementById("btn-start-export").addEventListener("click", startSimulatedExport);
    document.getElementById("btn-cancel-export").addEventListener("click", cancelSimulatedExport);
    document.getElementById("btn-export-snapshot").addEventListener("click", () => {
      const canvas = document.getElementById("preview-canvas");
      renderActivePreview();
      canvas.toBlob((blob) => {
        if (!blob) return;
        downloadBlob(blob, `${getActiveProject().name.replace(/\s+/g, "_")}_frame.png`);
        showToast("Saved PNG frame snapshot");
      });
    });
    document.getElementById("btn-export-project-json").addEventListener("click", () => {
      const proj = getActiveProject();
      const json = JSON.stringify(proj, null, 2);
      const filename = `${proj.name.replace(/\s+/g, "_")}.kythera.json`;
      if (window.AndroidBridge && typeof window.AndroidBridge.exportJsonFile === "function") {
        window.AndroidBridge.exportJsonFile(filename, json);
      } else {
        downloadBlob(new Blob([json], { type: "application/json" }), filename);
        showToast(`Exported ${filename}`);
      }
    });

    // JS Animation Studio events
    document.getElementById("btn-js-back").addEventListener("click", () =>
      navigateTo(state.previousScreen === "editor" ? "editor" : "home"));
    document.getElementById("js-code-input").addEventListener("input", (e) => {
      state.jsStudio.files[state.jsStudio.activeFile] = e.target.value;
      updateCodeLineNumbers();
    });
    document.getElementById("btn-js-run").addEventListener("click", runJsStudioPreview);
    document.getElementById("btn-js-reload").addEventListener("click", runJsStudioPreview);
    document.getElementById("btn-js-clear-console").addEventListener("click", () => {
      document.getElementById("js-console-output").innerHTML = "";
    });
    document.getElementById("btn-js-import-zip").addEventListener("click", triggerZipImport);
    document.getElementById("btn-js-export-zip").addEventListener("click", triggerZipExport);
    document.getElementById("btn-js-render").addEventListener("click", () => {
      runJsStudioPreview();
      appendConsoleLine("log", "Rendered live JS animation frame loop @ 60 FPS (sandbox preview).");
      showToast("JS animation render pass active");
    });
    document.getElementById("btn-js-preset").addEventListener("click", () => {
      const nextKey = state.jsStudio.presetKey === "cyberPulse" ? "kineticBars" : "cyberPulse";
      state.jsStudio.presetKey = nextKey;
      state.jsStudio.files = structuredClone(JS_PRESETS[nextKey].files);
      state.jsStudio.activeFile = "app.js";
      renderJsStudioFiles();
      runJsStudioPreview();
      showToast(`Loaded ${JS_PRESETS[nextKey].name}`);
    });
    document.getElementById("btn-js-new-file").addEventListener("click", () => {
      const idx = Object.keys(state.jsStudio.files).length + 1;
      const fname = `module_${idx}.js`;
      state.jsStudio.files[fname] = "// New Kythera JS module\nconsole.log('Loaded " + fname + "');\n";
      state.jsStudio.activeFile = fname;
      renderJsStudioFiles();
    });
    document.getElementById("btn-js-send-timeline").addEventListener("click", () => {
      navigateTo("editor");
      addNewLayerToProject({
        name: `JS: ${JS_PRESETS[state.jsStudio.presetKey]?.name || "Custom Script"}`,
        type: "shape",
        shapeKind: "ring",
        color: "#00E5FF",
      });
    });

    window.addEventListener("message", (ev) => {
      if (ev.data && ev.data.__kytheraConsole) {
        appendConsoleLine(ev.data.lvl, ev.data.text);
      }
    });

    // Browser Fallback File Inputs
    document.getElementById("input-import-project").addEventListener("change", async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      window.__kytheraNativeProjectImported(await file.text());
      e.target.value = "";
    });
    document.getElementById("input-import-zip").addEventListener("change", async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      const files = await parseZipArrayBuffer(await file.arrayBuffer());
      window.__kytheraNativeZipImported(files);
      e.target.value = "";
    });
    document.getElementById("input-pick-media").addEventListener("change", (e) => {
      const file = e.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = () => {
        window.__kytheraNativeMediaPicked({
          name: file.name,
          isVideo: file.type.startsWith("video/"),
          dataUrl: file.type.startsWith("image/") ? reader.result : null,
        });
      };
      reader.readAsDataURL(file);
      e.target.value = "";
    });

    // Modal close
    document.getElementById("btn-modal-close").addEventListener("click", closeModal);
    document.getElementById("modal-backdrop").addEventListener("click", (e) => {
      if (e.target.id === "modal-backdrop") closeModal();
    });

    setupEasingGraphDrag();
    setupVectorDrawingTool();

    // Update ABI badge immediately if running inside Android WebView
    const diag = getNativeDiagnostics();
    if (diag.abis && diag.abis.length > 0) {
      document.getElementById("abi-badge-label").textContent = diag.abis[0].toUpperCase();
    }
  }

  let toastTimer = null;
  function showToast(msg) {
    const el = document.getElementById("toast-banner");
    el.textContent = msg;
    el.classList.remove("hidden");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.add("hidden"), 2400);
  }

  function escapeHtml(str) {
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  // Boot
  loadProjects();
  initInteractions();
  renderHomeProjects();
  window.addEventListener("resize", () => {
    if (state.currentScreen === "editor") renderActivePreview();
  });
})();
