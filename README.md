# KYTHERA VIDEO STUDIO

**Create Without Limits.**

An Alight Motion-inspired mobile motion graphics & multi-layer video editing studio prototype packaged as a native Android application (Kotlin + Jetpack Compose + Android WebView + Storage Access Framework + MediaCodec Diagnostics) with an automated GitHub Actions Debug APK build pipeline.

## Architecture

- **Native Android Shell (`app/src/main/java/com/example/`)**
  - `MainActivity.kt` — Edge-to-edge host, Android Photo Picker (`PickVisualMedia`), Storage Access Framework (`OpenDocument` / `CreateDocument`) for `.zip` and `.kythera.json` files, and hardware back handling.
  - `webview/KytheraWebViewHost.kt` — Hardware-accelerated `WebView` powered by `WebViewAssetLoader` serving bundled local assets from `https://appassets.androidplatform.net/assets/index.html`.
  - `bridge/KytheraNativeBridge.kt` — `@JavascriptInterface` (`AndroidBridge`) bridging native file pickers, ZIP handling, and hardware codec diagnostics to the WebView UI.
  - `media/NativeMediaEngine.kt` — `MediaEngineGateway` architectural boundary for future native FFmpeg & `MediaCodec` rendering integration, with live `MediaCodecList` & `arm64-v8a` inspection.
  - `media/ZipProjectHandler.kt` — Safe `ZipInputStream` / `ZipOutputStream` extraction and packaging.
- **Interactive Motion Editor Prototype (`app/src/main/assets/`)**
  - `index.html`, `style.css`, `app.js` — Vanilla HTML5/CSS3/JS workspace (also runnable directly in Chrome Android) covering Home Screen, Multi-Layer Timeline, Live Canvas Compositor, Keyframe & Cubic-Bezier Graph Editor, 9-Category Effects Browser, Simulated Export Pipeline, and JS Animation Studio with ZIP import/export.

## Prototype vs. Native Rendering Scope

- Timeline playback, keyframe interpolation, vector drawing, effects previews, JS Animation Studio sandboxing, PNG frame snapshots, `.kythera.json` exports, and `.zip` packaging work locally in real time.
- Video muxing on the Export screen is an interactive prototype simulation; `MediaEngineGateway` defines the native integration seam for future FFmpeg / `MediaCodec` hardware encoding.

## Building Debug APK

### GitHub Actions
Push to `main`, open a pull request, or trigger **Android Debug APK Build** manually via `workflow_dispatch` in `.github/workflows/android-build.yml`. The workflow outputs `app/build/outputs/apk/debug/app-debug.apk` as a downloadable artifact.

### Local Build
```bash
./gradlew assembleDebug
```
