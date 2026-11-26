package com.example.webview

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.example.bridge.KytheraNativeBridge
import com.example.ui.theme.KytheraCyan
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraPanel
import com.example.ui.theme.KytheraTextPrimary
import com.example.ui.theme.KytheraTextSecondary
import com.example.ui.theme.KytheraVoid
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream

private const val TAG = "KytheraWebView"
const val KYTHERA_ASSET_URL = "https://appassets.androidplatform.net/assets/index.html"
const val KYTHERA_FILE_FALLBACK_URL = "file:///android_asset/index.html"

data class WebViewStartupState(
  val isLoading: Boolean = true,
  val pageFinished: Boolean = false,
  val bootConfirmed: Boolean = false,
  val activeUrl: String = KYTHERA_ASSET_URL,
  val errorTitle: String? = null,
  val errorDetails: String? = null,
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KytheraWebViewHost(
  nativeBridge: KytheraNativeBridge,
  externalStartupError: String?,
  externalBootConfirmed: Boolean,
  onClearExternalError: () -> Unit,
  onWebViewCreated: (WebView) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var startupState by remember { mutableStateOf(WebViewStartupState()) }
  val consoleErrors = remember { mutableStateListOf<String>() }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  // Verify bundled assets exist at startup
  val assetsPresent = remember {
    runCatching {
      val list = context.assets.list("")?.toSet() ?: emptySet()
      val ok = list.contains("index.html") && list.contains("style.css") && list.contains("app.js")
      Log.i(TAG, "Packaged assets check: ok=$ok files=$list")
      ok
    }.getOrElse { err ->
      Log.e(TAG, "Failed to inspect assets directory", err)
      false
    }
  }

  LaunchedEffect(assetsPresent) {
    if (!assetsPresent) {
      startupState = startupState.copy(
        isLoading = false,
        errorTitle = "Missing Packaged Web Assets",
        errorDetails = "Expected index.html, style.css, and app.js inside APK assets/.",
      )
    }
  }

  LaunchedEffect(externalBootConfirmed) {
    if (externalBootConfirmed) {
      startupState = startupState.copy(
        isLoading = false,
        bootConfirmed = true,
        errorTitle = null,
        errorDetails = null,
      )
    }
  }

  LaunchedEffect(externalStartupError) {
    if (!externalStartupError.isNullOrBlank()) {
      startupState = startupState.copy(
        isLoading = false,
        errorTitle = "JavaScript Startup Exception",
        errorDetails = externalStartupError,
      )
    }
  }

  // Watchdog: never leave the user staring at a silent black screen if WebView hangs
  LaunchedEffect(startupState.activeUrl, startupState.isLoading, startupState.bootConfirmed) {
    if (startupState.isLoading && !startupState.bootConfirmed && startupState.errorTitle == null) {
      delay(6500)
      if (!startupState.bootConfirmed && startupState.errorTitle == null) {
        Log.e(TAG, "Startup watchdog expired without JS boot confirmation for ${startupState.activeUrl}")
        startupState = startupState.copy(
          isLoading = false,
          errorTitle = "WebView Startup Timeout",
          errorDetails = "Page did not finish initializing within 6.5s.\nURL: ${startupState.activeUrl}\nPageFinished: ${startupState.pageFinished}",
        )
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(KytheraVoid)
      .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { ctx ->
        if ((ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
          WebView.setWebContentsDebuggingEnabled(true)
        }

        val assetLoader = WebViewAssetLoader.Builder()
          .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
          .build()

        WebView(ctx).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
          )
          setBackgroundColor(Color.parseColor("#07090E"))
          setLayerType(View.LAYER_TYPE_HARDWARE, null)
          overScrollMode = View.OVER_SCROLL_NEVER

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = false
            builtInZoomControls = false
            displayZoomControls = false
          }

          webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
              val line = "${consoleMessage.messageLevel()}: ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
              when (consoleMessage.messageLevel()) {
                ConsoleMessage.MessageLevel.ERROR -> {
                  Log.e(TAG, "WebConsole $line")
                  consoleErrors.add(line)
                }
                ConsoleMessage.MessageLevel.WARNING -> Log.w(TAG, "WebConsole $line")
                else -> Log.d(TAG, "WebConsole $line")
              }
              return true
            }
          }

          webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
              view: WebView,
              request: WebResourceRequest,
            ): WebResourceResponse? {
              val url = request.url
              if (url.path == "/favicon.ico") {
                return WebResourceResponse("image/x-icon", "UTF-8", 204, "No Content", emptyMap(), ByteArrayInputStream(ByteArray(0)))
              }
              val intercepted = assetLoader.shouldInterceptRequest(url)
              if (url.host == "appassets.androidplatform.net") {
                Log.d(TAG, "shouldInterceptRequest url=$url hit=${intercepted != null}")
              }
              return intercepted
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
              Log.i(TAG, "onPageStarted url=$url")
              startupState = startupState.copy(
                isLoading = true,
                pageFinished = false,
                activeUrl = url ?: startupState.activeUrl,
              )
            }

            override fun onPageFinished(view: WebView, url: String?) {
              Log.i(TAG, "onPageFinished url=$url")
              startupState = startupState.copy(pageFinished = true)
              view.evaluateJavascript(
                "(function(){ var s = document.getElementById('app-shell'); return JSON.stringify({ bootOk: !!window.__kytheraBootOk, height: s ? s.getBoundingClientRect().height : -1 }); })()",
              ) { rawResult ->
                Log.i(TAG, "Post-load DOM check: $rawResult")
                if (rawResult == null || !rawResult.contains("\"bootOk\":true")) {
                  startupState = startupState.copy(
                    isLoading = false,
                    errorTitle = "WebApp Failed To Boot",
                    errorDetails = "HTML loaded ($url) but app.js did not complete initialization.\nCheck: $rawResult",
                  )
                } else {
                  startupState = startupState.copy(
                    isLoading = false,
                    bootConfirmed = true,
                    errorTitle = null,
                    errorDetails = null,
                  )
                }
              }
            }

            override fun onReceivedError(
              view: WebView,
              request: WebResourceRequest,
              error: WebResourceError,
            ) {
              val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                "Code ${error.errorCode}: ${error.description} (${request.url})"
              } else {
                "WebView load error for ${request.url}"
              }
              Log.e(TAG, "onReceivedError mainFrame=${request.isForMainFrame} $msg")
              if (request.isForMainFrame) {
                startupState = startupState.copy(
                  isLoading = false,
                  errorTitle = "WebView Resource Error",
                  errorDetails = msg,
                )
              }
            }

            override fun onReceivedHttpError(
              view: WebView,
              request: WebResourceRequest,
              errorResponse: WebResourceResponse,
            ) {
              val msg = "HTTP ${errorResponse.statusCode} ${errorResponse.reasonPhrase} (${request.url})"
              Log.e(TAG, "onReceivedHttpError mainFrame=${request.isForMainFrame} $msg")
              if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                startupState = startupState.copy(
                  isLoading = false,
                  errorTitle = "WebView HTTP Error",
                  errorDetails = msg,
                )
              }
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
              Log.e(TAG, "onRenderProcessGone didCrash=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) detail.didCrash() else "unknown"}")
              startupState = startupState.copy(
                isLoading = false,
                errorTitle = "WebView Renderer Terminated",
                errorDetails = "The Android WebView render process exited unexpectedly.",
              )
              return true
            }
          }

          addJavascriptInterface(nativeBridge, "AndroidBridge")
          if (assetsPresent) {
            Log.i(TAG, "Loading local asset URL: $KYTHERA_ASSET_URL")
            loadUrl(KYTHERA_ASSET_URL)
          }
          webViewRef = this
          onWebViewCreated(this)
        }
      },
    )

    // Visible Loading Indicator (prevents silent black screen during WebView init)
    if (startupState.isLoading && startupState.errorTitle == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(KytheraVoid.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = KytheraCyan,
            strokeWidth = 3.dp,
          )
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "KYTHERA VIDEO STUDIO",
            color = KytheraTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Initializing Motion Workspace...",
            color = KytheraTextSecondary,
            fontSize = 12.sp,
          )
        }
      }
    }

    // Visible Startup Error Screen (replaces any permanent black screen)
    if (startupState.errorTitle != null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(KytheraVoid)
          .padding(20.dp),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = KytheraPanel,
        ) {
          Column(
            modifier = Modifier
              .padding(20.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = startupState.errorTitle ?: "Startup Error",
              color = KytheraCyan,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = startupState.errorDetails ?: "Unknown WebView startup failure.",
              color = KytheraTextPrimary,
              fontSize = 12.5.sp,
              fontFamily = FontFamily.Monospace,
            )
            if (consoleErrors.isNotEmpty()) {
              Text(
                text = "Console Errors:\n" + consoleErrors.takeLast(5).joinToString("\n"),
                color = KytheraTextSecondary,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                modifier = Modifier
                  .weight(1f)
                  .testTag("startup_retry_button"),
                colors = ButtonDefaults.buttonColors(containerColor = KytheraElectricBlue),
                onClick = {
                  consoleErrors.clear()
                  onClearExternalError()
                  startupState = WebViewStartupState(isLoading = true, activeUrl = KYTHERA_ASSET_URL)
                  webViewRef?.loadUrl(KYTHERA_ASSET_URL)
                },
              ) {
                Text("Retry")
              }
              OutlinedButton(
                modifier = Modifier
                  .weight(1f)
                  .testTag("startup_fallback_button"),
                onClick = {
                  consoleErrors.clear()
                  onClearExternalError()
                  startupState = WebViewStartupState(isLoading = true, activeUrl = KYTHERA_FILE_FALLBACK_URL)
                  webViewRef?.loadUrl(KYTHERA_FILE_FALLBACK_URL)
                },
              ) {
                Text("File Fallback", color = KytheraTextPrimary)
              }
            }
          }
        }
      }
    }
  }
}
