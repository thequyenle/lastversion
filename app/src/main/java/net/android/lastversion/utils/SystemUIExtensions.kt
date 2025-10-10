package net.android.lastversion.utils

import android.app.Activity
import android.app.Dialog
import android.view.View

/**
 * Extension function để quản lý SystemUI cho Activity
 * Sử dụng: activity?.showSystemUI(white = false)
 */
fun Activity.showSystemUI(white: Boolean = false) {
    if (white) {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    } else {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}

/**
 * Extension function to hide system UI (navigation bar) in any Dialog
 * Sử dụng: dialog.hideNavigationBar()
 */
fun Dialog.hideNavigationBar() {
    window?.decorView?.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            )
}

/**
 * Extension function to apply navigation hiding to Dialog when shown
 * Sử dụng: dialog.showWithHiddenNavigation()
 */
fun Dialog.showWithHiddenNavigation() {
    setOnShowListener {
        hideNavigationBar()
    }
    show()
}