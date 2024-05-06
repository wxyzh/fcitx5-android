/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.main.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.preference.DialogPreference
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.setOnChangeListener
import splitties.dimensions.dp
import splitties.resources.resolveThemeAttribute
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.horizontalMargin
import splitties.views.dsl.core.seekBar
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalMargin
import splitties.views.dsl.core.wrapContent
import splitties.views.textAppearance

class TwinSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : DialogPreference(context, attrs) {

    var min: Int = 0
    var max: Int = 100
    var step: Int = 1
    var unit: String = ""

    var label: String = ""
    var secondaryKey: String = ""
    var secondaryLabel: String = ""

    var default: Int = 0
    var secondaryDefault: Int = 0
    var defaultLabel: String? = null

    var value = 0
        private set
    var secondaryValue = 0
        private set

    override fun onSetInitialValue(defaultValue: Any?) {
        preferenceDataStore?.apply {
            value = getInt(key, default)
            secondaryValue = getInt(secondaryKey, secondaryDefault)
        } ?: sharedPreferences?.apply {
            value = getInt(key, default)
            secondaryValue = getInt(secondaryKey, secondaryDefault)
        }
    }

    /**
     * @param defaultValue should be `Pair<Int, Int>`
     */
    override fun setDefaultValue(defaultValue: Any?) {
        super.setDefaultValue(defaultValue)
        val (first, second) = defaultValue as? Pair<*, *> ?: return
        default = first as? Int ?: 0
        secondaryDefault = second as? Int ?: 0
    }

    private fun persistValues(primary: Int, secondary: Int) {
        if (!shouldPersist()) return
        value = primary
        secondaryValue = secondary
        preferenceDataStore?.apply {
            putInt(key, primary)
            putInt(secondaryKey, secondary)
        } ?: sharedPreferences?.edit {
            putInt(key, primary)
            putInt(secondaryKey, secondary)
        }
    }

    override fun onClick() {
        showDialog()
    }

    private fun ConstraintLayout.addSeekBar(
        label: String,
        initialValue: Int,
        defaultValue: Int? = null,
        belowView: View? = null
    ): SeekBar {
        val textLabel = textView {
            text = label
            textAppearance = context.resolveThemeAttribute(android.R.attr.textAppearanceListItem)
        }
        val valueLabel = textView {
            text = textForValue(initialValue, defaultValue)
            textAppearance = context.resolveThemeAttribute(android.R.attr.textAppearanceListItem)
        }
        val seekBar = seekBar {
            max = progressForValue(this@TwinSeekBarPreference.max)
            progress = progressForValue(initialValue)
            setOnChangeListener {
                valueLabel.text = textForValue(valueForProgress(it), defaultValue)
            }
        }
        val textMargin = dp(24)
        val seekBarMargin = dp(10)
        add(textLabel, lParams(wrapContent, wrapContent) {
            if (belowView == null) topOfParent(textMargin)
            else below(belowView, textMargin)
            startOfParent(textMargin)
        })
        add(valueLabel, lParams(wrapContent, wrapContent) {
            if (belowView == null) topOfParent(textMargin)
            else below(belowView, textMargin)
            endOfParent(textMargin)
        })
        add(seekBar, lParams(matchConstraints, wrapContent) {
            below(valueLabel, seekBarMargin)
            centerHorizontally(seekBarMargin)
        })
        return seekBar
    }

    private fun showDialog() {
        var messageText: TextView? = null
        val primarySeekBar: SeekBar
        val secondarySeekBar: SeekBar
        val dialogContent = context.constraintLayout {
            if (dialogMessage != null) {
                messageText = textView { text = dialogMessage }
                add(messageText!!, lParams {
                    verticalMargin = dp(8)
                    horizontalMargin = dp(24)
                })
            }
            primarySeekBar = addSeekBar(label, value, default, messageText)
            secondarySeekBar = addSeekBar(secondaryLabel, secondaryValue, secondaryDefault, primarySeekBar)
        }
        AlertDialog.Builder(context)
            .setTitle(this@TwinSeekBarPreference.dialogTitle)
            .setView(dialogContent)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val primary = valueForProgress(primarySeekBar.progress)
                val secondary = valueForProgress(secondarySeekBar.progress)
                setValue(primary, secondary)
            }
            .setNeutralButton(R.string.default_) { _, _ ->
                setValue(default, secondaryDefault)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setValue(primary: Int, secondary: Int) {
        if (callChangeListener(primary to secondary)) {
            persistValues(primary, secondary)
            notifyChanged()
        }
    }

    private fun progressForValue(value: Int) = (value - min) / step

    private fun valueForProgress(progress: Int) = (progress * step) + min

    private fun textForValue(value: Int, default: Int? = null): String =
        if (value == default && defaultLabel != null) defaultLabel!! else "$value $unit"

    object SimpleSummaryProvider : SummaryProvider<TwinSeekBarPreference> {
        override fun provideSummary(preference: TwinSeekBarPreference): CharSequence {
            return preference.run {
                val primary = textForValue(value, default)
                val secondary = textForValue(secondaryValue, secondaryDefault)
                "$primary / $secondary"
            }
        }
    }

}
