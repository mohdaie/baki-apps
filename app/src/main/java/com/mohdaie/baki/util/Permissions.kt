package com.mohdaie.baki.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Permissions {

    /** Has the user switched on "notification access" for Baki? */
    fun listenerEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun openListenerSettings(context: Context) = start(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    fun openOverlaySettings(context: Context) = start(
        context,
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
    )

    fun openAppNotificationSettings(context: Context) = start(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )

    /** App info page — where "Allow restricted settings" lives for side-loaded apps. */
    fun openAppInfo(context: Context) = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
    )

    private fun start(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
