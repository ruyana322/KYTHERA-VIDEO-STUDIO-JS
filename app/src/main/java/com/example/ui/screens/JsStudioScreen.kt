package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.JsPresetKind
import com.example.model.PreviewQuality
import com.example.model.StudioJsonSerializer
import com.example.ui.components.GlassCard
import com.example.ui.components.PreviewCanvas
import com.example.ui.theme.CinemaStageVoid
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraMint
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.KytheraViolet
import com.example.ui.theme.LocalKytheraGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

private const val JS_SANDBOX_HOST = "jsstudio.kythera.local"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JsStudioScreen(
  preset: JsPresetKind,
  files: Map<String, String>,
  binaryFiles: Map<String, Pair<String, ByteArray>>,
  activeFile: String,
  runRevision: Int,
  consoleLogs: List<String>,
  isPlaying: Boolean,
  onBack: () -> Unit,
  onSelectFile: (String) -> Unit,
  onEditContent: (String) -> Unit,
  onAddNewFile: () -> Unit,
  onTogglePreset: () -> Unit,
  onImportZip: () -> Unit,
  onExportZip: () -> Unit,
  onRunRender: () -> Unit,
  onConsoleLog: (String) -> Unit,
  onClearConsole: () -> Unit,
  onSendToTimeline: () -> Unit,
  bottomContentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val glass = LocalKytheraGlass.current
  val activeCode = files[activeFile].orEmpty()

  val hasHtml = files.keys.any { it.endsWith(".html", ignoreCase = true) || it.endsWith(".htm", ignoreCase = true) }
  val hasJs = files.keys.any { it.endsWith(".js", ignoreCase = true) || it.endsWith(".mjs", ignoreCase = true) }
  val timelineKey = files.keys.firstOrNull { it.endsWith("timeline.json", ignoreCase = true) }
  val parsedTimelineProject = remember(timelineKey, files[timelineKey]) {
    timelineKey?.let { files[it] }?.let { StudioJsonSerializer.parseExternalTimelineJson(it, "ZIP Timeline") }
  }
  val timelineOnlyMode = !hasHtml && !hasJs && parsedTimelineProject != null

  var timelineTime by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(timelineOnlyMode, parsedTimelineProject?.id, isPlaying) {
    if (timelineOnlyMode && parsedTimelineProject != null && isPlaying) {
      while (isActive) {
        delay(16L)
        timelineTime = (timelineTime + 0.016f) % parsedTimelineProject.durationSec.coerceAtLeast(1f)
      }
    }
  }

  val webViewRef = remember { arrayOfNulls<WebView>(1) }
  DisposableEffect(Unit) {
    onDispose {
      webViewRef[0]?.stopLoading()
      webViewRef[0]?.destroy()
      webViewRef[0] = null
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 16.dp,
      end = 16.dp,
      top = 10.dp,
      bottom = bottomContentPadding.calculateBottomPadding() + 20.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("js_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = glass.textPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("JS Animation Studio", style = MaterialTheme.typography.titleLarge, color = glass.textPrimary)
          Text(
            text = if (timelineOnlyMode) "timeline.json · ${parsedTimelineProject?.layers?.size ?: 0} layers"
            else if (hasHtml || hasJs) "${files.size} project files loaded"
            else preset.title,
            style = MaterialTheme.typography.bodySmall,
            color = KytheraPeriwinkle,
          )
        }
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onTogglePreset)
            .testTag("js_preset_switch"),
          color = KytheraViolet.copy(alpha = 0.16f),
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = KytheraViolet, modifier = Modifier.size(14.dp))
            Text("Preset", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KytheraViolet)
          }
        }
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onImportZip,
          modifier = Modifier.weight(1f).testTag("js_import_zip"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Filled.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
          Text(" Import ZIP", fontSize = 11.5.sp)
        }
        OutlinedButton(
          onClick = onExportZip,
          modifier = Modifier.weight(1f).testTag("js_export_zip"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
          Text(" Export ZIP", fontSize = 11.5.sp)
        }
        Button(
          onClick = onSendToTimeline,
          colors = ButtonDefaults.buttonColors(containerColor = KytheraPeriwinkle),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.testTag("js_send_timeline"),
        ) {
          Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
          Text(if (parsedTimelineProject != null) " To Editor" else " Timeline", fontSize = 11.5.sp)
        }
      }
    }

    item {
      GlassCard(cornerRadius = 20.dp, elevated = true, contentPadding = 10.dp) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = if (timelineOnlyMode) "TIMELINE.JSON LIVE PREVIEW" else "LIVE HTML/JS SANDBOX",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = glass.textSecondary,
          )
          Button(
            onClick = onRunRender,
            colors = ButtonDefaults.buttonColors(containerColor = KytheraElectricBlue),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp).testTag("js_render_button"),
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
            Text(" Run / Reload", fontSize = 11.sp)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (timelineOnlyMode && parsedTimelineProject != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(220.dp)
              .clip(RoundedCornerShape(16.dp)),
          ) {
            PreviewCanvas(
              project = parsedTimelineProject,
              currentTimeSec = timelineTime,
              selectedLayerId = null,
              previewQuality = PreviewQuality.FULL,
              isDrawMode = false,
              loadedImages = emptyMap(),
              onRequestMediaLoad = { _, _ -> },
              onCommitDrawStroke = {},
              onDragSelectedLayer = { _, _ -> },
              modifier = Modifier.fillMaxSize(),
            )
          }
        } else {
          val bundleHtml = remember(files, runRevision) {
            buildRunnableHtmlDocument(files)
          }
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(230.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(CinemaStageVoid),
          ) {
            AndroidView(
              modifier = Modifier.fillMaxSize(),
              factory = { ctx ->
                WebView(ctx).apply {
                  layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                  )
                  setBackgroundColor(AndroidColor.parseColor("#090C14"))
                  settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                  }
                  webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                      onConsoleLog("[${msg.messageLevel()}] ${msg.message()}")
                      return true
                    }
                  }
                  webViewRef[0] = this
                }
              },
              update = { wv ->
                wv.webViewClient = object : WebViewClient() {
                  override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                  ): WebResourceResponse? {
                    val url = request.url
                    if (url.host != JS_SANDBOX_HOST) return null
                    val relPath = url.path?.trimStart('/').orEmpty()
                    if (relPath == "index.html" || relPath.isEmpty()) {
                      return WebResourceResponse(
                        "text/html",
                        "UTF-8",
                        ByteArrayInputStream(bundleHtml.toByteArray(StandardCharsets.UTF_8)),
                      )
                    }
                    val textMatch = files[relPath]
                      ?: files.entries.firstOrNull { it.key.equals(relPath, ignoreCase = true) }?.value
                    if (textMatch != null) {
                      return WebResourceResponse(
                        mimeForPath(relPath),
                        "UTF-8",
                        ByteArrayInputStream(textMatch.toByteArray(StandardCharsets.UTF_8)),
                      )
                    }
                    val binMatch = binaryFiles[relPath]
                      ?: binaryFiles.entries.firstOrNull { it.key.equals(relPath, ignoreCase = true) }?.value
                    if (binMatch != null) {
                      return WebResourceResponse(
                        binMatch.first,
                        null,
                        ByteArrayInputStream(binMatch.second),
                      )
                    }
                    return null
                  }
                }
                wv.loadUrl("https://$JS_SANDBOX_HOST/index.html?rev=$runRevision")
              },
            )
          }
        }
      }
    }

    item {
      GlassCard(cornerRadius = 20.dp, contentPadding = 12.dp) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          files.keys.forEach { fileName ->
            val sel = fileName == activeFile
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectFile(fileName) },
              color = if (sel) KytheraPeriwinkle else glass.glassSurface,
              border = BorderStroke(1.dp, glass.subtleBorder),
              shape = RoundedCornerShape(10.dp),
            ) {
              Text(
                text = fileName,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (sel) Color.White else glass.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onAddNewFile),
            color = KytheraMint.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.Add, contentDescription = "New File", tint = KytheraMint, modifier = Modifier.size(14.dp))
              Text(" New", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KytheraMint)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = activeCode,
          onValueChange = onEditContent,
          textStyle = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = glass.textPrimary,
          ),
          minLines = 7,
          maxLines = 12,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("js_code_editor"),
        )
      }
    }

    item {
      GlassCard(cornerRadius = 18.dp, contentPadding = 12.dp) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("CONSOLE", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = glass.textSecondary)
          TextButton(onClick = onClearConsole) { Text("Clear", fontSize = 11.sp) }
        }
        if (consoleLogs.isEmpty()) {
          Text("Console empty", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = glass.textMuted)
        } else {
          consoleLogs.forEach { line ->
            Text(
              text = line,
              fontFamily = JetBrainsMonoFamily,
              fontSize = 11.sp,
              color = KytheraElectricBlue,
            )
          }
        }
      }
    }
  }
}

private fun mimeForPath(path: String): String {
  return when (path.substringAfterLast('.', "").lowercase()) {
    "html", "htm" -> "text/html"
    "css" -> "text/css"
    "js", "mjs", "cjs" -> "application/javascript"
    "json" -> "application/json"
    "svg" -> "image/svg+xml"
    else -> "text/plain"
  }
}

private fun buildRunnableHtmlDocument(files: Map<String, String>): String {
  val htmlEntry = files.entries.firstOrNull { it.key.equals("index.html", ignoreCase = true) }
    ?: files.entries.firstOrNull { it.key.endsWith(".html", ignoreCase = true) || it.key.endsWith(".htm", ignoreCase = true) }

  val timelineJson = files.entries.firstOrNull { it.key.endsWith("timeline.json", ignoreCase = true) }?.value
  val timelineShim = if (!timelineJson.isNullOrBlank()) {
    "<script>window.KYTHERA_TIMELINE = $timelineJson;</script>"
  } else {
    ""
  }

  val errorHook = """
    <script>
      window.addEventListener('error', function(e) {
        console.error((e && e.message) ? e.message : 'Script runtime error');
      });
    </script>
  """.trimIndent()

  if (htmlEntry != null) {
    val rawHtml = htmlEntry.value
    // Inject helper draw functions if user uses Kythera preset shorthand, plus timeline/error hooks
    val baseTag = "<base href=\"https://$JS_SANDBOX_HOST/\" />"
    return if (rawHtml.contains("<head>", ignoreCase = true)) {
      rawHtml.replaceFirst("<head>", "<head>$baseTag$errorHook$timelineShim", ignoreCase = true)
    } else {
      "<!DOCTYPE html><html><head>$baseTag$errorHook$timelineShim</head><body>$rawHtml</body></html>"
    }
  }

  // Synthesize HTML wrapper when ZIP contains JS/CSS files without index.html
  val cssLinks = files.keys.filter { it.endsWith(".css", ignoreCase = true) }
    .joinToString("\n") { "<link rel=\"stylesheet\" href=\"$it\" />" }
  val jsScripts = files.keys.filter { it.endsWith(".js", ignoreCase = true) || it.endsWith(".mjs", ignoreCase = true) }
    .joinToString("\n") { "<script src=\"$it\"></script>" }

  return """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <base href="https://$JS_SANDBOX_HOST/" />
  $errorHook
  $timelineShim
  $cssLinks
  <style>
    html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #090C14; overflow: hidden; }
    canvas#stage { width: 100%; height: 100%; display: block; }
  </style>
</head>
<body>
  <canvas id="stage"></canvas>
  <script>
    (function() {
      var cvs = document.getElementById('stage');
      var ctx = cvs ? cvs.getContext('2d') : null;
      if (cvs) { cvs.width = window.innerWidth || 400; cvs.height = window.innerHeight || 230; }
      window.drawGlowRing = window.drawGlowRing || function(r, hue) {
        if (!ctx) return;
        ctx.strokeStyle = 'hsla(' + (hue || 210) + ', 95%, 62%, 0.72)';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.arc(cvs.width / 2, cvs.height / 2, Math.max(8, r % 120), 0, Math.PI * 2);
        ctx.stroke();
      };
      window.drawSpectrumBar = window.drawSpectrumBar || function(i, amp) {
        if (!ctx) return;
        var bw = (cvs.width * 0.8) / 18;
        var bh = amp * (cvs.height * 0.7);
        ctx.fillStyle = '#00C6FF';
        ctx.fillRect(cvs.width * 0.1 + i * bw, cvs.height - 20 - bh, Math.max(4, bw - 4), bh);
      };
    })();
  </script>
  $jsScripts
  <script>
    (function() {
      var cvs = document.getElementById('stage');
      var ctx = cvs ? cvs.getContext('2d') : null;
      var start = performance.now();
      if (typeof window.renderFrame === 'function' || typeof window.renderSpectrum === 'function') {
        function tick(now) {
          if (ctx) { ctx.clearRect(0, 0, cvs.width, cvs.height); }
          var t = (now - start) / 1000;
          if (typeof window.renderFrame === 'function') window.renderFrame(t);
          if (typeof window.renderSpectrum === 'function') window.renderSpectrum(t);
          requestAnimationFrame(tick);
        }
        requestAnimationFrame(tick);
      }
    })();
  </script>
</body>
</html>"""
}
