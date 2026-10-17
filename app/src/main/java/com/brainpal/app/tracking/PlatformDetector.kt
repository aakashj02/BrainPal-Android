package com.brainpal.app.tracking

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.security.MessageDigest
import java.util.Locale

data class DetectionCandidate(
    val platform: String,
    val contentKey: String,
    val evidence: String
)

/** Best-effort UI heuristic; social apps do not expose an official watched-video API. */
class PlatformDetector {
    private val supportedPackages = mapOf(
        "com.instagram.android" to "instagram",
        "com.facebook.katana" to "facebook",
        "com.google.android.youtube" to "youtube"
    )

    fun isSupportedPackage(packageName: String?): Boolean =
        packageName != null && supportedPackages.containsKey(packageName)

    fun detect(packageName: String?, event: AccessibilityEvent, root: AccessibilityNodeInfo?): DetectionCandidate? {
        val platform = supportedPackages[packageName] ?: return null
        if (event.eventType !in ACCEPTED_EVENTS) return null
        root ?: return null

        val collected = mutableListOf<String>()
        collectText(root, collected, 0)
        val normalized = collected.map(::normalize).filter(String::isNotBlank).distinct()
        if (normalized.isEmpty()) return null
        val allText = normalized.joinToString(" ").lowercase(Locale.ROOT)

        val feedMarker = when (platform) {
            "instagram", "facebook" -> allText.contains("reels") || allText.contains("reel")
            "youtube" -> allText.contains("shorts") || allText.contains("short")
            else -> false
        }
        if (!feedMarker) return null

        // Best-effort ad exclusion; labels vary by app version and language.
        if (listOf("sponsored", "paid partnership", "promoted", "विज्ञापन").any(allText::contains)) return null

        val useful = normalized.filter { text ->
            val lower = text.lowercase(Locale.ROOT)
            text.length >= 5 &&
                lower !in GENERIC_LABELS &&
                !lower.matches(Regex("""[0-9,.kmb]+\s*(likes?|views?|comments?|shares?|followers?)?""")) &&
                !lower.contains("profile picture") &&
                !lower.contains("reel by ") && !lower.contains("short by ") &&
                !lower.contains("profile picture of") &&
                !lower.contains("liked this reel") &&
                !lower.contains("double tap to") &&
                !lower.contains("add a comment") &&
                !lower.contains("write a comment") &&
                !lower.contains("follow") && !lower.contains("subscribe") &&
                !lower.contains("reels") && !lower.contains("shorts") &&
                !lower.contains("sponsored") && !lower.contains("promoted")
        }.distinct().take(6)
        if (useful.isEmpty()) return null

        // The content key deliberately excludes changing engagement counts and common UI labels.
        // Requiring two useful text fragments reduces repeated false positives from one video.
        val stable = useful.filter { it.length >= 8 }.take(3)
        if (stable.isEmpty()) return null
        val material = "$platform|" + stable.joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(material.toByteArray(Charsets.UTF_8))
            .take(16).joinToString("") { "%02x".format(it) }

        return DetectionCandidate(platform, "$platform:$digest", stable.joinToString(" / ").take(180))
    }

    private fun collectText(node: AccessibilityNodeInfo, out: MutableList<String>, depth: Int) {
        if (depth > 18 || out.size >= 100) return
        node.text?.toString()?.trim()?.takeIf(String::isNotBlank)?.let(out::add)
        node.contentDescription?.toString()?.trim()?.takeIf(String::isNotBlank)?.let(out::add)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try { collectText(child, out, depth + 1) } finally { child.recycle() }
        }
    }

    private fun normalize(value: String): String =
        value.replace(Regex("\\s+"), " ")
            .replace(Regex("""[\u200B-\u200D\uFEFF]"""), "")
            .trim().take(160)

    companion object {
        private val ACCEPTED_EVENTS = setOf(
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        private val GENERIC_LABELS = setOf(
            "home", "search", "profile", "reels", "shorts", "friends", "for you",
            "following", "explore", "notifications", "create", "back", "more",
            "video player", "play", "pause", "next", "previous", "like", "comment", "share", "save", "audio", "original audio", "use audio", "send", "liked", "reply"
        )
    }
}
