package net.android.lastversion.utils

import android.app.Activity
import android.widget.Toast
import com.google.android.play.core.review.ReviewManagerFactory

object InAppReviewHelper {

    fun showInAppReview(activity: Activity, onComplete: ((Boolean) -> Unit)? = null) {
        val reviewManager = ReviewManagerFactory.create(activity)

        val request = reviewManager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result

                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { flowTask ->
                    onComplete?.invoke(flowTask.isSuccessful)

                    if (flowTask.isSuccessful) {
                        Toast.makeText(
                            activity,
                            "Thank you for your feedback!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                onComplete?.invoke(false)
                openPlayStore(activity)
            }
        }
    }

    private fun openPlayStore(activity: Activity) {
        try {
            val packageName = activity.packageName
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("market://details?id=$packageName")
            )
            activity.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            val packageName = activity.packageName
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            activity.startActivity(intent)
        }
    }

    fun shouldShowReview(activity: Activity): Boolean {
        val prefs = activity.getSharedPreferences("app_review_prefs", Activity.MODE_PRIVATE)
        val reviewShownCount = prefs.getInt("review_shown_count", 0)
        val lastReviewTime = prefs.getLong("last_review_time", 0)

        val currentTime = System.currentTimeMillis()
        val daysSinceLastReview = (currentTime - lastReviewTime) / (1000 * 60 * 60 * 24)

        return reviewShownCount < 3 && (lastReviewTime == 0L || daysSinceLastReview >= 7)
    }

    fun markReviewShown(activity: Activity) {
        val prefs = activity.getSharedPreferences("app_review_prefs", Activity.MODE_PRIVATE)
        val currentCount = prefs.getInt("review_shown_count", 0)

        prefs.edit().apply {
            putInt("review_shown_count", currentCount + 1)
            putLong("last_review_time", System.currentTimeMillis())
            apply()
        }
    }
}