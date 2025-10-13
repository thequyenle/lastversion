package net.android.lastversion.utils

import android.view.View

/**
 * Extension function to prevent spam clicking on any View
 * @param debounceTime Time in milliseconds to wait before allowing next click (default: 1000ms = 1 second)
 * @param action The action to perform on click
 */
fun View.setOnClickListenerWithDebounce(debounceTime: Long = 1000L, action: (View) -> Unit) {
    var lastClickTime = 0L
    
    setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceTime) {
            lastClickTime = currentTime
            action(view)
        }
    }
}

