package com.mohdaie.baki.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mohdaie.baki.R
import com.mohdaie.baki.data.PendingEntity
import com.mohdaie.baki.model.Category
import com.mohdaie.baki.model.TxType
import com.mohdaie.baki.ui.ReviewActivity
import com.mohdaie.baki.util.Fmt
import com.mohdaie.baki.util.Permissions

/** Baki's own heads-up notification: "RM 12.80 · McDonald's — Save / Ignore". */
object Notifier {

    const val CHANNEL_ID = "new_transactions"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "New transactions",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Pops up each time a money notification is captured" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notificationId(pendingId: Long): Int = (1000 + pendingId % 100_000).toInt()

    @SuppressLint("MissingPermission")
    fun showPending(context: Context, p: PendingEntity) {
        if (!Permissions.canPostNotifications(context)) return
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val base = notificationId(p.id) * 4

        val open = PendingIntent.getActivity(context, base, ReviewActivity.newIntent(context, p.id), flags)
        val category = Category.of(p.category)?.label
        val summary = when (p.type) {
            TxType.INCOME -> "Income · ${p.account}"
            TxType.TRANSFER -> "Transfer · ${p.account}"
            else -> if (category != null) "$category · ${p.account}" else "Pick a category · ${p.account}"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_baki)
            .setColor(0xFF1F7A4D.toInt())
            .setContentTitle("RM ${Fmt.money(p.amount)} · ${p.merchant}")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$summary\n${p.rawText}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)

        // One-tap save only when nothing needs picking.
        if (p.type != TxType.EXPENSE || p.category != null) {
            builder.addAction(0, "Save", broadcast(context, QuickActionReceiver.ACTION_SAVE, p.id, base + 1))
        }
        builder.addAction(0, "Review", open)
        builder.addAction(0, "Ignore", broadcast(context, QuickActionReceiver.ACTION_IGNORE, p.id, base + 2))

        NotificationManagerCompat.from(context).notify(notificationId(p.id), builder.build())
    }

    fun cancel(context: Context, pendingId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(pendingId))
    }

    /** Opens the review popup over other apps when "Display over other apps" is allowed. */
    fun tryOpenPopup(context: Context, pendingId: Long) {
        if (!Permissions.canDrawOverlays(context)) return
        runCatching { context.startActivity(ReviewActivity.newIntent(context, pendingId)) }
    }

    private fun broadcast(context: Context, action: String, pendingId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(context, QuickActionReceiver::class.java)
            .setAction(action)
            .putExtra(QuickActionReceiver.EXTRA_ID, pendingId)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
