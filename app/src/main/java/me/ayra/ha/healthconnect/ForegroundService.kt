package me.ayra.ha.healthconnect

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
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.ayra.ha.healthconnect.data.Settings.getAutoSync
import me.ayra.ha.healthconnect.data.Settings.getForegroundServiceEnabled
import me.ayra.ha.healthconnect.data.Settings.getSettings

class ForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
        const val NOTIFICATION_ID = 1
        private const val SETTINGS_POLL_INTERVAL_MS = 60_000L

        fun Context.runServiceIfEnabled() {
            if (getForegroundServiceEnabled()) {
                val appContext = applicationContext
                val serviceIntent = Intent(appContext, ForegroundService::class.java)
                ContextCompat.startForegroundService(appContext, serviceIntent)
            }
        }

        fun stopService(context: Context) {
            val appContext = context.applicationContext
            val stopIntent = Intent(appContext, ForegroundService::class.java)
            appContext.stopService(stopIntent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var syncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val notificationIntent =
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification: Notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.foreground_service_notification_title))
                .setContentText(getString(R.string.foreground_service_notification_text))
                .setSmallIcon(R.drawable.ic_ecg_heart_24px)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(true)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }

        startSyncLoop()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
        stopForeground(true)
    }

    private fun startSyncLoop() {
        if (syncJob?.isActive == true) return
        syncJob =
            serviceScope.launch {
                while (isActive) {
                    val intervalSeconds =
                        applicationContext.getSettings("updateInterval")?.toLongOrNull()
                            ?: SyncWorker.DEFAULT_INTERVAL

                    if (intervalSeconds < SyncWorker.MINIMUM_INTERVAL) {
                        delay(intervalSeconds.coerceAtLeast(SyncWorker.MINIMUM_FAST_INTERVAL) * 1000L)
                        if (applicationContext.getAutoSync() != false) {
                            SyncWorker.startNow(applicationContext)
                        }
                    } else {
                        // WorkManager already covers intervals at or above its floor; just
                        // watch for the user switching to a faster interval later.
                        delay(SETTINGS_POLL_INTERVAL_MS)
                    }
                }
            }
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.foreground_service_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        val manager = getSystemService<NotificationManager>()
        manager?.createNotificationChannel(channel)
    }
}
