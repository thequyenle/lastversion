package net.android.lastversion.alarm.util

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * Extension functions cho Context
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Extension functions cho Fragment
 */
fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(message, duration)
}

/**
 * Extension function cho BooleanArray
 */
fun BooleanArray.toStringList(): String {
    return this.joinToString(",")
}

fun String.toBooleanArray(): BooleanArray {
    return this.split(",").map { it.toBoolean() }.toBooleanArray()
}