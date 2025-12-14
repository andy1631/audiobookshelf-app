package com.audiobookshelf.app.download

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.audiobookshelf.app.MainActivity
import com.audiobookshelf.app.R
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DownloadService : Service() {

  companion object {
    private const val CHANNEL_ID = "abs_downloads"
    private const val CHANNEL_NAME = "Audiobookshelf Downloads"
    private const val NOTIFICATION_ID = 2002

    @Volatile private var instance: DownloadService? = null
    private val pendingLaunchBlocks =
            CopyOnWriteArrayList<suspend CoroutineScope.() -> Unit>()
    private val pendingNotifications = CopyOnWriteArrayList<NotificationState>()

    /** Ensures the foreground service is started. */
    fun start(context: Context) {
      val intent = Intent(context, DownloadService::class.java)
      ContextCompat.startForegroundService(context, intent)
    }

    /** Stops the foreground service. */
    fun stop(context: Context) {
      val intent = Intent(context, DownloadService::class.java)
      context.stopService(intent)
    }

    /**
     * Launches work on the service scope. If the service is not created yet, the work is queued and
     * executed once it starts.
     */
    fun launchDownloadWork(block: suspend CoroutineScope.() -> Unit): Job? {
      val service = instance
      return if (service != null) {
        service.launch(block)
      } else {
        pendingLaunchBlocks.add(block)
        null
      }
    }

    /** Updates the persistent notification with the latest aggregate progress. */
    fun updateNotification(progressPercent: Int?, activeCount: Int, titles: List<String>) {
      val service = instance
      val state = NotificationState(progressPercent, activeCount, titles)
      if (service != null) {
        service.postNotification(state)
      } else {
        pendingNotifications.add(state)
      }
    }
  }

  private val tag = "DownloadService"
  private val serviceJob = SupervisorJob()
  private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
  private val activeJobs = Collections.synchronizedSet(mutableSetOf<Job>())
  private var latestNotificationState = NotificationState(null, 0, emptyList())

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    instance = this
    Log.d(tag, "onCreate foreground download service")

    createNotificationChannel()
    startForegroundSafely(buildNotification(latestNotificationState))
    flushPendingLaunchBlocks()
    flushPendingNotifications()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.d(tag, "onDestroy foreground download service")
    serviceScope.cancel()
    instance = null
  }

  private fun launch(block: suspend CoroutineScope.() -> Unit): Job {
    val job = serviceScope.launch(block = block)
    trackJob(job)
    return job
  }

  private fun flushPendingLaunchBlocks() {
    if (pendingLaunchBlocks.isEmpty()) return
    pendingLaunchBlocks.forEach { block -> launch(block) }
    pendingLaunchBlocks.clear()
  }

  private fun flushPendingNotifications() {
    if (pendingNotifications.isEmpty()) return
    pendingNotifications.forEach { postNotification(it) }
    pendingNotifications.clear()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
              NotificationChannel(
                              CHANNEL_ID,
                              CHANNEL_NAME,
                              NotificationManager.IMPORTANCE_LOW
                      )
                      .apply {
                        enableLights(false)
                        enableVibration(false)
                        lightColor = Color.DKGRAY
                        setShowBadge(false)
                        description = "Download progress"
                      }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(state: NotificationState): Notification {
    val progressPercent = state.progressPercent?.coerceIn(0, 100)
    val itemLabel = if (state.activeCount == 1) "item" else "items"
    val primaryTitle =
            when {
              state.activeCount == 1 && state.titles.isNotEmpty() -> state.titles.first()
              state.activeCount > 0 && progressPercent != null ->
                      "Downloading ${state.activeCount} $itemLabel • $progressPercent%"
              state.activeCount > 0 -> "Downloading ${state.activeCount} $itemLabel"
              else -> "Downloads"
            }
    val statusText =
            when {
              state.activeCount > 0 && progressPercent != null ->
                      "Downloading (${progressPercent}% complete)"
              state.activeCount > 0 -> "Downloading in progress"
              else -> "Waiting for downloads"
            }
    val detailLines = state.titles.take(5)
    val expandedText =
            if (detailLines.isNotEmpty()) detailLines.joinToString("\n") else statusText
    val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
    )

    val accentColor = ContextCompat.getColor(this, com.audiobookshelf.app.R.color.light_blue_600)

    return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(primaryTitle)
            .setContentText(statusText)
            .setStyle(
                    NotificationCompat.BigTextStyle()
                            .bigText(expandedText)
                            .setSummaryText(
                                    if (state.activeCount > 1)
                                            "Downloading ${state.activeCount} $itemLabel"
                                    else "Audiobookshelf downloads"
                            )
            )
            .setSmallIcon(R.drawable.icon_monochrome)
            .setColor(accentColor)
            .setColorized(true)
            .setOnlyAlertOnce(true)
            .setOngoing(state.activeCount > 0)
            .setProgress(100, progressPercent ?: 0, progressPercent == null)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
  }

  private fun postNotification(state: NotificationState) {
    latestNotificationState = state
    val notification = buildNotification(state)
    if (!canPostNotifications()) {
      Log.w(tag, "Notification permission not granted; skipping download notification")
      checkStopWhenIdle()
      return
    }

    try {
      NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    } catch (se: SecurityException) {
      Log.w(tag, "Unable to post download notification", se)
    }
    checkStopWhenIdle()
  }

  private data class NotificationState(
          val progressPercent: Int?,
          val activeCount: Int,
          val titles: List<String>
  )

  private fun trackJob(job: Job) {
    activeJobs.add(job)
    job.invokeOnCompletion {
      activeJobs.remove(job)
      checkStopWhenIdle()
    }
  }

  private fun checkStopWhenIdle() {
    if (isIdle()) stopSelf()
  }

  private fun isIdle(): Boolean {
    return activeJobs.isEmpty() && latestNotificationState.activeCount == 0
  }

  private fun canPostNotifications(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
  }

  private fun startForegroundSafely(notification: Notification) {
    if (!canPostNotifications()) {
      Log.w(tag, "Notification permission not granted; starting service without visible notification")
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    } catch (se: SecurityException) {
      Log.w(tag, "Unable to start foreground with notification", se)
    }
  }
}
