package com.example.motion.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.motion.export.VideoExportEngine
import com.example.motion.io.ZipProjectStorage
import com.example.motion.model.RenderQuality
import com.example.motion.model.RenderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

class RenderForegroundService : Service() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var activeJob: Job? = null
  private var wakeLock: PowerManager.WakeLock? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_CANCEL) {
      RenderProgressBus.requestCancel()
      updateNotification("Membatalkan render…", 0, 0)
      return START_NOT_STICKY
    }

    val projectId = intent?.getStringExtra(EXTRA_PROJECT_ID).orEmpty()
    val qualityName = intent?.getStringExtra(EXTRA_QUALITY) ?: RenderQuality.NORMAL.name
    val quality = runCatching { RenderQuality.valueOf(qualityName) }.getOrDefault(RenderQuality.NORMAL)

    if (projectId.isBlank()) {
      RenderProgressBus.update(RenderStatus.Failed("ID proyek tidak valid."))
      stopSelf()
      return START_NOT_STICKY
    }

    val initialNotif = buildNotification("Menyiapkan render MP4…", 0, 0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIF_ID,
        initialNotif,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
      )
    } else {
      startForeground(NOTIF_ID, initialNotif)
    }

    activeJob?.cancel()
    activeJob = serviceScope.launch {
      acquireWakeLock()
      val startMs = System.currentTimeMillis()
      var lastNotifMs = 0L
      try {
        RenderProgressBus.resetForStart("Menyiapkan proyek & audio…")
        val storage = ZipProjectStorage(applicationContext)
        val manifest = storage.loadProject(projectId)
          ?: throw IllegalStateException("Proyek tidak ditemukan di penyimpanan.")
        val projectDir = storage.projectDir(projectId)
        val engine = VideoExportEngine(applicationContext)

        val result = engine.renderToGalleryMp4(
          projectDir = projectDir,
          manifest = manifest,
          quality = quality,
          onProgress = { frame, total, phase ->
            val elapsedSec = max(0.2f, (System.currentTimeMillis() - startMs) / 1000f)
            val etaSec = if (frame > 5 && total > frame) {
              val fpsRate = frame / elapsedSec
              ((total - frame) / max(0.5f, fpsRate)).roundToInt()
            } else {
              0
            }
            RenderProgressBus.update(
              RenderStatus.Rendering(
                frame = frame,
                totalFrames = total,
                etaSeconds = etaSec,
                phaseLabel = phase,
              ),
            )
            val now = System.currentTimeMillis()
            if (now - lastNotifMs >= 450L || frame == total) {
              lastNotifMs = now
              val etaTxt = if (etaSec > 0) " · sisa ~${etaSec} dtk" else ""
              updateNotification("$phase Frame $frame/$total$etaTxt", frame, total)
            }
          },
          isCancelled = { RenderProgressBus.isCancelled() },
        )

        RenderProgressBus.update(
          RenderStatus.Completed(
            uriString = result.galleryUri.toString(),
            fileName = result.fileName,
            sizeBytes = result.sizeBytes,
            durationSec = result.durationSec,
          ),
        )
        postFinalNotification("Render selesai: ${result.fileName}")
      } catch (_: InterruptedException) {
        RenderProgressBus.update(RenderStatus.Cancelled)
        postFinalNotification("Render dibatalkan. Mulai ulang dari awal jika ingin merender kembali.")
      } catch (t: Throwable) {
        val msg = t.message ?: "Terjadi kesalahan saat merender MP4."
        RenderProgressBus.update(RenderStatus.Failed("$msg (Jika terhenti di tengah, mulai render ulang dari awal.)"))
        postFinalNotification("Render gagal: $msg")
      } finally {
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
      }
    }

    return START_NOT_STICKY
  }

  override fun onDestroy() {
    releaseWakeLock()
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun acquireWakeLock() {
    runCatching {
      val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
      wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KytheraStudio:RenderLock").apply {
        acquire(5 * 60 * 1000L)
      }
    }
  }

  private fun releaseWakeLock() {
    runCatching {
      if (wakeLock?.isHeld == true) wakeLock?.release()
      wakeLock = null
    }
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val mgr = getSystemService(NotificationManager::class.java)
      val channel = NotificationChannel(
        CHANNEL_ID,
        getString(R.string.render_notif_channel_name),
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = getString(R.string.render_notif_channel_desc)
      }
      mgr.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(text: String, progress: Int, max: Int): Notification {
    val openIntent = PendingIntent.getActivity(
      this,
      1,
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      },
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val cancelIntent = PendingIntent.getService(
      this,
      2,
      Intent(this, RenderForegroundService::class.java).apply { action = ACTION_CANCEL },
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("Kythera Studio · Render MP4")
      .setContentText(text)
      .setOnlyAlertOnce(true)
      .setOngoing(true)
      .setContentIntent(openIntent)
      .setProgress(max, progress, max == 0)
      .addAction(0, "Batalkan", cancelIntent)
      .build()
  }

  private fun updateNotification(text: String, progress: Int, max: Int) {
    val mgr = getSystemService(NotificationManager::class.java)
    mgr.notify(NOTIF_ID, buildNotification(text, progress, max))
  }

  private fun postFinalNotification(text: String) {
    val openIntent = PendingIntent.getActivity(
      this,
      1,
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      },
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val mgr = getSystemService(NotificationManager::class.java)
    val notif = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("Kythera Studio")
      .setContentText(text)
      .setAutoCancel(true)
      .setContentIntent(openIntent)
      .build()
    mgr.notify(NOTIF_DONE_ID, notif)
  }

  companion object {
    const val CHANNEL_ID = "kythera_render_channel"
    const val NOTIF_ID = 4101
    const val NOTIF_DONE_ID = 4102
    const val ACTION_START = "com.example.motion.ACTION_START_RENDER"
    const val ACTION_CANCEL = "com.example.motion.ACTION_CANCEL_RENDER"
    const val EXTRA_PROJECT_ID = "extra_project_id"
    const val EXTRA_QUALITY = "extra_quality"

    fun startRender(context: Context, projectId: String, quality: RenderQuality) {
      val intent = Intent(context, RenderForegroundService::class.java).apply {
        action = ACTION_START
        putExtra(EXTRA_PROJECT_ID, projectId)
        putExtra(EXTRA_QUALITY, quality.name)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun cancelRender(context: Context) {
      RenderProgressBus.requestCancel()
      val intent = Intent(context, RenderForegroundService::class.java).apply {
        action = ACTION_CANCEL
      }
      context.startService(intent)
    }
  }
}
