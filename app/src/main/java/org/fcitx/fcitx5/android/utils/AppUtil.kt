/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.main.ClipboardEditActivity
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.fcitx.fcitx5.android.ui.main.settings.im.InputMethodConfigFragment
import kotlin.system.exitProcess

object AppUtil {

    fun launchMain(context: Context) {
        context.startActivity<MainActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
    }

    private fun launchMainToDest(context: Context, @IdRes dest: Int, arguments: Bundle? = null) {
        NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.settings_nav)
            .addDestination(dest, arguments)
            .createTaskStackBuilder()
            /**
             * [androidx.core.app.TaskStackBuilder.getIntents] would add unwanted flags
             * [Intent.FLAG_ACTIVITY_CLEAR_TASK] and [Intent.FLAG_ACTIVITY_TASK_ON_HOME]
             * so we must launch the Intent by ourselves
             */
            .editIntentAt(0)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                context.startActivity(this)
            }
    }

    fun launchMainToKeyboard(context: Context) =
        launchMainToDest(context, R.id.keyboardSettingsFragment)

    fun launchMainToInputMethodList(context: Context) =
        launchMainToDest(context, R.id.imListFragment)

    fun launchMainToThemeList(context: Context) =
        launchMainToDest(context, R.id.themeFragment)

    fun launchMainToInputMethodConfig(context: Context, uniqueName: String, displayName: String) =
        launchMainToDest(
            context, R.id.imConfigFragment, bundleOf(
                InputMethodConfigFragment.ARG_NAME to displayName,
                InputMethodConfigFragment.ARG_UNIQUE_NAME to uniqueName
            )
        )

    fun launchClipboardEdit(context: Context, id: Int, lastEntry: Boolean = false) {
        context.startActivity<ClipboardEditActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ClipboardEditActivity.ENTRY_ID, id)
            putExtra(ClipboardEditActivity.LAST_ENTRY, lastEntry)
        }
    }

    fun exit() {
        exitProcess(0)
    }

    private const val RESTART_CHANNEL_ID = "app-restart"

    private const val RESTART_NOTIFY_ID = 0xdead

    private fun createRestartNotificationChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RESTART_CHANNEL_ID,
                ctx.getText(R.string.restart_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = RESTART_CHANNEL_ID }
            ctx.notificationManager.createNotificationChannel(channel)
        }
    }

    fun showRestartNotification(ctx: Context) {
        createRestartNotificationChannel(ctx)
        NotificationCompat.Builder(ctx, RESTART_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle(ctx.getText(R.string.app_name))
            .setContentText(ctx.getText(R.string.restart_notify_msg))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(
                PendingIntent.getActivity(
                    ctx,
                    0,
                    Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .build()
            .let { ctx.notificationManager.notify(RESTART_NOTIFY_ID, it) }
    }
}
