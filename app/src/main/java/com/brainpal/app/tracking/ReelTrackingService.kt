package com.brainpal.app.tracking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.brainpal.app.TodayWidgetProvider
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.brainpal.app.data.BrainPalDatabase
import com.brainpal.app.data.DetectionDiagnosticEntity
import com.brainpal.app.data.WatchEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.LinkedHashMap

/**
 * Stage 4: heuristic candidate detection, in-session deduplication, and local Room inserts.
 * Exact detection cannot be guaranteed because these apps do not provide an official watch API.
 */
class ReelTrackingService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val detector = PlatformDetector()
    private val seenInSession = LinkedHashMap<String, Long>()
    private val lastCountAtByPlatform = mutableMapOf<String, Long>()
    private var lastSupportedActivityAt = 0L
    private var lastDiagnosticAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility tracking service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString()
        val nowElapsed = SystemClock.elapsedRealtime()

        if (!detector.isSupportedPackage(packageName)) return
        if (lastSupportedActivityAt != 0L && nowElapsed - lastSupportedActivityAt > SESSION_IDLE_RESET_MS) {
            synchronized(seenInSession) { seenInSession.clear(); lastCountAtByPlatform.clear() }
            Log.i(TAG, "Viewing session cache reset after long idle")
        }
        lastSupportedActivityAt = nowElapsed

        if (event.eventType !in ACCEPTED_EVENT_TYPES) return
        val root = rootInActiveWindow ?: return
        val candidate = try {
            detector.detect(packageName, event, root)
        } catch (t: Throwable) {
            Log.w(TAG, "Detector failed safely", t)
            null
        } finally {
            root.recycle()
        } ?: return

        synchronized(seenInSession) {
            if (seenInSession.containsKey(candidate.contentKey)) return
            // Accessibility may emit several slightly different trees while one video is still on screen.
            // A short per-platform cooldown trades a few missed rapid swipes for fewer multi-count errors.
            val previousCountAt = lastCountAtByPlatform[ candidate.platform ] ?: 0L
            if (previousCountAt != 0L && nowElapsed - previousCountAt < SAME_VIDEO_COOLDOWN_MS) return
            lastCountAtByPlatform[candidate.platform] = nowElapsed
            seenInSession[candidate.contentKey] = nowElapsed
            while (seenInSession.size > MAX_SESSION_KEYS) {
                val oldest = seenInSession.keys.firstOrNull() ?: break
                seenInSession.remove(oldest)
            }
        }

        val wallTime = System.currentTimeMillis()
        val localDate = LocalDate.now().toString()
        serviceScope.launch {
            runCatching {
                val dao = BrainPalDatabase.getInstance(applicationContext).brainPalDao()
                dao.insertWatchEvent(
                    WatchEventEntity(
                        platform = candidate.platform,
                        watchedAtEpochMillis = wallTime,
                        localDate = localDate,
                        contentKey = candidate.contentKey,
                        wasAd = false
                    )
                )
                maybeNotifyMilestone(dao, localDate)
                TodayWidgetProvider.refresh(applicationContext)
                Log.i(TAG, "Saved estimated candidate for ${candidate.platform}: ${candidate.evidence}")
                if (wallTime - lastDiagnosticAt > DIAGNOSTIC_INTERVAL_MS) {
                    lastDiagnosticAt = wallTime
                    dao.insertDiagnostic(
                        DetectionDiagnosticEntity(
                            createdAtEpochMillis = wallTime,
                            platform = candidate.platform,
                            message = "Watch event inserted (heuristic). Evidence: ${candidate.evidence}"
                        )
                    )
                }
            }.onFailure { Log.e(TAG, "Could not save watch event", it) }
        }
    }


    private suspend fun maybeNotifyMilestone(dao: com.brainpal.app.data.BrainPalDao, localDate: String) {
        val goal = dao.getDailyGoal()?.reelsTarget ?: return
        val count = dao.countForDate(localDate) + dao.correctionForDate(localDate)
        // Notifications are opt-in at Android OS level; a denied permission is handled below.
        val interval = 5
        if (count <= goal || (count - goal) % interval != 0) return
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("brainpal_goal", "Goal milestones", NotificationManager.IMPORTANCE_DEFAULT))
        }
        try {
            val notification = NotificationCompat.Builder(this, "brainpal_goal")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Brain Pal goal milestone")
                .setContentText("You've reached $count estimated Reels/Shorts today (goal: $goal).")
                .setAutoCancel(true).build()
            if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(4000 + count, notification)
            }
        } catch (e: SecurityException) { Log.i(TAG, "Notification permission not granted") }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean = super.onUnbind(intent)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BrainPalTracking"
        private const val SESSION_IDLE_RESET_MS = 10 * 60 * 1000L
        private const val SAME_VIDEO_COOLDOWN_MS = 5_000L
        private const val DIAGNOSTIC_INTERVAL_MS = 10_000L
        private const val MAX_SESSION_KEYS = 500
        private val ACCEPTED_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
    }
}
