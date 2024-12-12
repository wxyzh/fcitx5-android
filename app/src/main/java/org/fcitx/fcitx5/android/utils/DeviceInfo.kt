/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import org.fcitx.fcitx5.android.BuildConfig

// Adapted from https://gist.github.com/hendrawd/01f215fd332d84793e600e7f82fc154b
object DeviceInfo {
    fun get(context: Context) = buildString {
        appendLine("--------- Device Info")
        appendLine("OS Name: ${Build.DISPLAY}")
        appendLine("OS Version: ${System.getProperty("os.version")} (${Build.VERSION.INCREMENTAL})")
        appendLine("OS API Level: ${Build.VERSION.SDK_INT}")
        appendLine("Device: ${Build.DEVICE}")
        appendLine("Model (product): ${Build.MODEL} (${Build.PRODUCT})")
        appendLine("Manufacturer: ${Build.MANUFACTURER}")
        appendLine("Tags: ${Build.TAGS}")
        @Suppress("DEPRECATION") // we really want the physical display size
        val size = Point().also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display!!
            } else {
                context.windowManager.defaultDisplay
            }.getRealSize(it)
        }
        appendLine("Screen Size: ${size.x} x ${size.y}")
        val metrics = context.resources.displayMetrics
        appendLine("Screen Density: ${metrics.density}")
        appendLine(
            "Screen orientation: ${
                when (context.resources.configuration.orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> "Portrait"
                    Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
                    Configuration.ORIENTATION_UNDEFINED -> "Undefined"
                    else -> "Unknown"
                }
            }"
        )
        appendLine("--------- Package Info")
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        appendLine("Package Name: ${pkgInfo.packageName}")
        appendLine("Version Code: ${pkgInfo.versionCodeCompat}")
        appendLine("Version Name: ${pkgInfo.versionName}")
        appendLine("--------- Build Info")
        appendLine("Build Type: ${BuildConfig.BUILD_TYPE}")
        appendLine("Build Time: ${iso8601UTCDateTime(BuildConfig.BUILD_TIME)}")
        appendLine("Build Git Hash: ${BuildConfig.BUILD_GIT_HASH}")
    }
}