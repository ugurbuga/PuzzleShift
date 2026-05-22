package com.ugurbuga.blockgames.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.shared.R
import com.ugurbuga.blockgames.settings.AppSettingsStorage
import com.ugurbuga.blockgames.settings.shouldSendDailyChallengeReminder
import com.ugurbuga.blockgames.settings.shouldSendMissYouReminder
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.app_title_blocksort
import blockgames.composeapp.generated.resources.app_title_blockwise
import blockgames.composeapp.generated.resources.app_title_boomblocks
import blockgames.composeapp.generated.resources.app_title_chainshift
import blockgames.composeapp.generated.resources.app_title_mergeshift
import blockgames.composeapp.generated.resources.app_title_stackshift
import blockgames.composeapp.generated.resources.notification_daily_challenge_body
import blockgames.composeapp.generated.resources.notification_daily_challenge_title
import blockgames.composeapp.generated.resources.notification_miss_you_body
import blockgames.composeapp.generated.resources.notification_reminders_channel_name
import blockgames.composeapp.generated.resources.notification_test_body
import java.util.Locale

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "stackshift_notification_channel_v2"

        const val TAG_MISS_YOU = "miss_you_notification"
        const val TAG_DAILY_CHALLENGE = "daily_challenge_notification"

        const val DATA_KEY_TYPE = "notification_type"
        const val EXTRA_TARGET_GAMEPLAY_STYLE = "target_gameplay_style"

        const val TYPE_MISS_YOU = "miss_you"
        const val TYPE_DAILY_CHALLENGE = "daily_challenge"
        const val TYPE_TEST = "test"
    }

    override suspend fun doWork(): Result {
        val type = inputData.getString(DATA_KEY_TYPE) ?: TYPE_MISS_YOU
        val settings = AppSettingsStorage.load()
        val shouldShow = when (type) {
            TYPE_DAILY_CHALLENGE -> settings.shouldSendDailyChallengeReminder(getCurrentDate())
            TYPE_MISS_YOU -> settings.shouldSendMissYouReminder(currentEpochMillis())
            TYPE_TEST -> true
            else -> true
        }
        if (shouldShow) showNotification(type)
        return Result.success()
    }

    private suspend fun showNotification(type: String) {
        val settings = AppSettingsStorage.load()
        val selectedGameplayStyle = settings.selectedGameplayStyle ?: GameplayStyle.StackShift
        val appLocale = Locale.forLanguageTag(settings.language.localeTag)

        // Temporarily set default locale to fetch strings in app-selected language
        val originalLocale = Locale.getDefault()
        Locale.setDefault(appLocale)

        suspend fun localized(resource: StringResource): String =
            runCatching { getString(resource) }.getOrDefault("")

        val appName = localized(selectedGameplayStyle.appTitleResource())
            .ifBlank {
                applicationContext.applicationInfo
                    .loadLabel(applicationContext.packageManager)
                    .toString()
            }

        val title = when (type) {
            TYPE_DAILY_CHALLENGE -> localized(Res.string.notification_daily_challenge_title)
            TYPE_TEST -> appName
            else -> appName
        }.ifBlank { appName }

        val message = when (type) {
            TYPE_DAILY_CHALLENGE -> localized(Res.string.notification_daily_challenge_body)
            TYPE_TEST -> localized(Res.string.notification_test_body)
            else -> localized(Res.string.notification_miss_you_body)
        }

        val channelName = localized(Res.string.notification_reminders_channel_name).ifBlank { appName }

        Locale.setDefault(originalLocale)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply {
                putExtra(EXTRA_TARGET_GAMEPLAY_STYLE, selectedGameplayStyle.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationIdForType(type),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationId = notificationIdForType(type)
        val largeIcon = runCatching {
            applicationContext.packageManager
                .getApplicationIcon(applicationContext.packageName)
                .toBitmap()
        }.getOrNull()

        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor("#071018".toColorInt())
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        largeIcon?.let(notificationBuilder::setLargeIcon)

        val notification = notificationBuilder.build()

        notificationManager.notify(notificationId, notification)
    }

    private fun notificationIdForType(type: String): Int = when (type) {
        TYPE_DAILY_CHALLENGE -> 1002
        TYPE_TEST -> 1003
        else -> 1001
    }
}

private fun GameplayStyle.appTitleResource(): StringResource = when (this) {
    GameplayStyle.BlockSort -> Res.string.app_title_blocksort
    GameplayStyle.BlockWise -> Res.string.app_title_blockwise
    GameplayStyle.ChainShift -> Res.string.app_title_chainshift
    GameplayStyle.MergeShift -> Res.string.app_title_mergeshift
    GameplayStyle.BoomBlocks -> Res.string.app_title_boomblocks
    GameplayStyle.StackShift -> Res.string.app_title_stackshift
}

private fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) {
        bitmap?.let { return it }
    }

    val width = intrinsicWidth.takeIf { it > 0 } ?: 256
    val height = intrinsicHeight.takeIf { it > 0 } ?: 256
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

