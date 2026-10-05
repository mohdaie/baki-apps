package com.mohdaie.baki.service

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mohdaie.baki.BakiApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Receives every notification on the phone once the user grants "Notification access".
 * Text is read on the device and never leaves it.
 */
class MoneyNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName == packageName) return
        val n = sbn.notification ?: return
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if ((n.flags and Notification.FLAG_ONGOING_EVENT) != 0) return
        val extras = n.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString("\n") { it.toString() }
            .orEmpty()
        val body = listOf(if (big.length > text.length) big else text, lines)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")
        if (title.isBlank() && body.isBlank()) return

        val pkg = sbn.packageName
        val label = appLabel(pkg, extras)
        val postedAt = sbn.postTime
        val repo = (application as BakiApp).repository
        scope.launch {
            runCatching { repo.handleNotification(pkg, label, title, body, postedAt) }
        }
    }

    @Suppress("DEPRECATION")
    private fun appLabel(pkg: String, extras: Bundle): String {
        val pm = packageManager
        val fromNotification = runCatching {
            extras.getParcelable<ApplicationInfo>("android.appInfo")?.let { pm.getApplicationLabel(it).toString() }
        }.getOrNull()
        if (!fromNotification.isNullOrBlank()) return fromNotification
        val fromPackage = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrNull()
        if (!fromPackage.isNullOrBlank()) return fromPackage
        return pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
