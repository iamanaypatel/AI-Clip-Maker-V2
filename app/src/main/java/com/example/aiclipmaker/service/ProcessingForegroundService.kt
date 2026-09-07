package com.example.aiclipmaker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.aiclipmaker.MainActivity
import com.example.aiclipmaker.R
import com.example.aiclipmaker.data.model.AspectRatioPreset
import com.example.aiclipmaker.data.model.CaptionStylePreset
import com.example.aiclipmaker.data.model.ClipGenerationConfig
import com.example.aiclipmaker.data.repository.ClipRepository
import com.example.aiclipmaker.processing.ClipProcessingPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProcessingForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "ai_clip_processing_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_PROCESSING"
        const val ACTION_CANCEL = "ACTION_CANCEL_PROCESSING"

        const val EXTRA_VIDEO_URI = "extra_video_uri"
        const val EXTRA_VIDEO_NAME = "extra_video_name"
        const val EXTRA_MIN_DUR = "extra_min_dur"
        const val EXTRA_PREF_DUR = "extra_pref_dur"
        const val EXTRA_MAX_DUR = "extra_max_dur"
        const val EXTRA_ASPECT = "extra_aspect"
        const val EXTRA_CAPTION = "extra_caption"

        fun start(
            context: Context,
            videoUri: Uri,
            videoName: String,
            config: ClipGenerationConfig
        ) {
            val intent = Intent(context, ProcessingForegroundService::class.java).apply {
                action = ACTION_START
                data = videoUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(EXTRA_VIDEO_URI, videoUri.toString())
                putExtra(EXTRA_VIDEO_NAME, videoName)
                putExtra(EXTRA_MIN_DUR, config.minDurationSec)
                putExtra(EXTRA_PREF_DUR, config.preferredDurationSec)
                putExtra(EXTRA_MAX_DUR, config.maxDurationSec)
                putExtra(EXTRA_ASPECT, config.aspectRatio.name)
                putExtra(EXTRA_CAPTION, config.captionStyle.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancel(context: Context) {
            val intent = Intent(context, ProcessingForegroundService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AIClipMaker::ProcessingWakeLock").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                ClipProcessingPipeline.cancelCurrentJob()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val videoUri = intent.data ?: intent.getStringExtra(EXTRA_VIDEO_URI)?.let { Uri.parse(it) } ?: return START_NOT_STICKY
                val videoName = intent.getStringExtra(EXTRA_VIDEO_NAME) ?: "Video"
                val minDur = intent.getIntExtra(EXTRA_MIN_DUR, 20)
                val prefDur = intent.getIntExtra(EXTRA_PREF_DUR, 60)
                val maxDur = intent.getIntExtra(EXTRA_MAX_DUR, 90)
                val aspectName = intent.getStringExtra(EXTRA_ASPECT) ?: AspectRatioPreset.RATIO_9_16.name
                val captionName = intent.getStringExtra(EXTRA_CAPTION) ?: CaptionStylePreset.PODCAST.name

                val config = ClipGenerationConfig(
                    minDurationSec = minDur,
                    preferredDurationSec = prefDur,
                    maxDurationSec = maxDur,
                    aspectRatio = AspectRatioPreset.valueOf(aspectName),
                    captionStyle = CaptionStylePreset.valueOf(captionName)
                )

                startForeground(NOTIFICATION_ID, buildNotification("Starting video analysis...", 0))
                wakeLock?.acquire(60 * 60 * 1000L) // 60 min max timeout

                observePipelineProgress()

                serviceScope.launch {
                    val repo = ClipRepository.getInstance(applicationContext)
                    ClipProcessingPipeline.processVideo(
                        context = applicationContext,
                        videoUri = videoUri,
                        videoName = videoName,
                        config = config,
                        repository = repo
                    )
                    repo.refreshData()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun observePipelineProgress() {
        serviceScope.launch {
            ClipProcessingPipeline.currentJob.collectLatest { job ->
                if (job != null) {
                    val progressPercent = (job.progress * 100).toInt()
                    val notification = buildNotification(
                        title = job.stage.title,
                        progress = progressPercent
                    )
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun buildNotification(title: String, progress: Int): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, ProcessingForegroundService::class.java).apply {
            action = ACTION_CANCEL
        }
        val pendingCancel = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Clip Maker: $title")
            .setContentText("Processing video offline on device")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .setProgress(100, progress, progress == 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", pendingCancel)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Clip Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of offline video clipping and rendering"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
