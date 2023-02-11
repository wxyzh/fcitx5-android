package org.fcitx.fcitx5.android.input.bar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.ViewAnimator
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.Clipboard
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.ClipboardTimedOut
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.Empty
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.Toolbar
import org.fcitx.fcitx5.android.input.bar.IdleUiStateMachine.State.ToolbarWithClip
import org.fcitx.fcitx5.android.input.keyboard.BaseKeyboard
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import org.fcitx.fcitx5.android.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.after
import splitties.views.dsl.constraintlayout.before
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalMargin
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter
import splitties.views.gravityVerticalCenter
import splitties.views.imageResource
import splitties.views.padding
import timber.log.Timber

sealed class KawaiiBarUi(override val ctx: Context, protected val theme: Theme) : Ui {

    companion object {
        val disableAnimation by AppPrefs.getInstance().advanced.disableAnimation
    }

    protected fun toolButton(@DrawableRes icon: Int, initView: ToolButton.() -> Unit = {}) =
        ToolButton(ctx, icon, theme).apply(initView)

    class Candidate(ctx: Context, theme: Theme, private val horizontalView: View) :
        KawaiiBarUi(ctx, theme) {

        val expandButton = toolButton(R.drawable.ic_baseline_expand_more_24) {
            id = R.id.expand_candidate_btn
            visibility = View.INVISIBLE
        }

        override val root = ctx.constraintLayout {
            add(expandButton, lParams(dp(40)) {
                centerVertically()
                endOfParent()
            })
            add(horizontalView, lParams {
                centerVertically()
                startOfParent()
                before(expandButton)
            })
        }
    }

    class Idle(
        ctx: Context,
        theme: Theme,
        private val getCurrentState: () -> IdleUiStateMachine.State,
    ) : KawaiiBarUi(ctx, theme) {

        private val IdleUiStateMachine.State.menuButtonRotation
            get() =
                if (inPrivate) 0f
                else when (this) {
                    Empty -> -90f
                    Clipboard -> -90f
                    Toolbar -> 90f
                    ToolbarWithClip -> 90f
                    ClipboardTimedOut -> -90f
                }

        private var inPrivate = false

        val menuButton = toolButton(R.drawable.ic_baseline_expand_more_24) {
            rotation = getCurrentState().menuButtonRotation
        }

        val undoButton = toolButton(R.drawable.ic_baseline_undo_24)

        val redoButton = toolButton(R.drawable.ic_baseline_redo_24)

        val cursorMoveButton = toolButton(R.drawable.ic_cursor_move)

        val clipboardButton = toolButton(R.drawable.ic_clipboard)

        val moreButton = toolButton(R.drawable.ic_baseline_more_horiz_24)

        val hideKeyboardButton = toolButton(R.drawable.ic_baseline_arrow_drop_down_24)

        private fun ConstraintLayout.addButton(
            v: View,
            initParams: ConstraintLayout.LayoutParams.() -> Unit = {}
        ) {
            add(v, ConstraintLayout.LayoutParams(dp(40), dp(40)).apply {
                centerVertically()
                initParams(this)
            })
        }

        private val buttonsBar = constraintLayout {
            addButton(undoButton) { startOfParent(); before(redoButton) }
            addButton(redoButton) { after(undoButton); before(cursorMoveButton) }
            addButton(cursorMoveButton) { after(redoButton); before(clipboardButton) }
            addButton(clipboardButton) { after(cursorMoveButton); before(moreButton) }
            addButton(moreButton) { after(clipboardButton); endOfParent() }
        }

        private val clipboardIcon = imageView {
            imageResource = R.drawable.ic_clipboard
            colorFilter = PorterDuffColorFilter(theme.altKeyTextColor, PorterDuff.Mode.SRC_IN)
        }

        private val clipboardText = textView {
            isSingleLine = true
            maxWidth = dp(120)
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(theme.altKeyTextColor)
        }

        private val clipboardSuggestionLayout = horizontalLayout {
            gravity = gravityCenter
            padding = dp(4)
            add(clipboardIcon, lParams(dp(20), dp(20)))
            add(clipboardText, lParams {
                leftMargin = dp(4)
            })
        }

        val clipboardSuggestionItem = object : CustomGestureView(ctx) {
            init {
                visibility = View.GONE
                isHapticFeedbackEnabled = false
                background = rippleDrawable(theme.keyPressHighlightColor)
                add(clipboardSuggestionLayout, lParams(wrapContent, matchParent))
            }
        }

        private val clipboardBar = constraintLayout {
            add(clipboardSuggestionItem, lParams(wrapContent, matchConstraints) {
                topOfParent()
                startOfParent()
                endOfParent()
                bottomOfParent()
                verticalMargin = dp(4)
            })
        }

        private val animator = ViewAnimator(ctx).apply {
            add(clipboardBar, lParams(matchParent, matchParent))
            add(buttonsBar, lParams(matchParent, matchParent))

            if (disableAnimation) {
                inAnimation = null
                outAnimation = null
            } else {
                inAnimation = AnimationSet(true).apply {
                    duration = 200L
                    addAnimation(AlphaAnimation(0f, 1f))
                    addAnimation(ScaleAnimation(0f, 1f, 0f, 1f, 0f, dp(20f)))
                    addAnimation(TranslateAnimation(dp(-100f), 0f, 0f, 0f))
                }
                outAnimation = AnimationSet(true).apply {
                    duration = 200L
                    addAnimation(AlphaAnimation(1f, 0f))
                    addAnimation(ScaleAnimation(1f, 0f, 1f, 0f, 0f, dp(20f)))
                    addAnimation(TranslateAnimation(0f, dp(-100f), 0f, 0f))
                }
            }
        }

        override val root = constraintLayout {
            addButton(menuButton) { startOfParent() }
            add(animator, lParams(matchConstraints, dp(40)) {
                after(menuButton)
                before(hideKeyboardButton)
            })
            addButton(hideKeyboardButton) { endOfParent() }
        }

        fun privateMode(activate: Boolean = true) {
            if (activate == inPrivate) return
            inPrivate = activate
            updateMenuButtonIcon()
            updateMenuButtonRotation(instant = true)
        }

        private fun updateMenuButtonIcon() {
            menuButton.image.imageResource =
                if (inPrivate) R.drawable.ic_view_private
                else R.drawable.ic_baseline_expand_more_24
        }

        private fun updateMenuButtonRotation(instant: Boolean = false) {
            val targetRotation = getCurrentState().menuButtonRotation
            menuButton.apply {
                if (targetRotation == rotation) return
                if (instant || disableAnimation) {
                    rotation = targetRotation
                } else {
                    animate().setDuration(200L).rotation(targetRotation)
                }
            }
        }

        private fun transitionToClipboardBar() {
            animator.displayedChild = 0
        }

        private fun transitionToButtonsBar() {
            animator.displayedChild = 1
        }

        fun switchUiByState(state: IdleUiStateMachine.State) {
            Timber.d("Switch idle ui to $state")
            when (state) {
                Clipboard -> {
                    transitionToClipboardBar()
                    enableClipboardItem()
                }

                Toolbar -> {
                    transitionToButtonsBar()
                    disableClipboardItem()
                }

                Empty -> {
                    // empty and clipboard share the same view
                    transitionToClipboardBar()
                    disableClipboardItem()
                    setClipboardItemText("")
                }

                ToolbarWithClip -> {
                    transitionToButtonsBar()
                }

                ClipboardTimedOut -> {
                    transitionToClipboardBar()
                }
            }
            updateMenuButtonRotation()
        }

        private fun enableClipboardItem() {
            clipboardSuggestionItem.visibility = View.VISIBLE
        }

        private fun disableClipboardItem() {
            clipboardSuggestionItem.visibility = View.GONE
        }

        fun setClipboardItemText(text: String) {
            clipboardText.text = text
        }
    }

    class Title(ctx: Context, theme: Theme) : KawaiiBarUi(ctx, theme) {

        private val backButton = toolButton(R.drawable.ic_baseline_arrow_back_24)

        private val titleText = textView {
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(theme.altKeyTextColor)
            gravity = gravityVerticalCenter
            textSize = 16f
        }

        private var extension: View? = null

        override val root = constraintLayout {
            add(backButton, lParams(dp(40), dp(40)) {
                topOfParent()
                startOfParent()
                bottomOfParent()
            })
            add(titleText, lParams(wrapContent, dp(40)) {
                topOfParent()
                after(backButton, dp(8))
                bottomOfParent()
            })
        }

        fun setReturnButtonOnClickListener(block: () -> Unit) {
            backButton.setOnClickListener {
                block()
            }
        }

        fun setTitle(title: String) {
            titleText.text = title
        }

        fun addExtension(view: View, showTitle: Boolean) {
            if (extension != null) {
                throw IllegalStateException("TitleBar extension is already present")
            }
            backButton.isVisible = showTitle
            titleText.isVisible = showTitle
            extension = view
            root.run {
                add(view, lParams(matchConstraints, dp(40)) {
                    centerVertically()
                    if (showTitle) {
                        endOfParent(dp(5))
                    } else {
                        centerHorizontally()
                    }
                })
            }
        }

        fun removeExtension() {
            if (extension == null)
                return
            root.removeView(extension)
            extension = null
        }
    }

    class NumberRowUi(ctx: Context, theme: Theme) : KawaiiBarUi(ctx, theme) {
        @SuppressLint("ViewConstructor")
        class Keyboard(ctx: Context, theme: Theme) : BaseKeyboard(
            ctx,
            theme,
            listOf(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map(::NumKey))
        ) {
            class NumKey(digit: String) : KeyDef(
                Appearance.Text(
                    displayText = digit,
                    textSize = 21f
                ),
                setOf(Behavior.Press(KeyAction.SymAction(KeySym(digit.codePointAt(0))))),
                arrayOf(Popup.Preview(digit))
            )
        }

        override val root = Keyboard(ctx, theme)

    }
}