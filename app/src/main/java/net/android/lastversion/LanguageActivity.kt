package net.android.lastversion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.android.lastversion.data.LanguageItem


class LanguageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        val languageList = mutableListOf(
            LanguageItem("English", R.drawable.flag_english),
            LanguageItem("Spanish", R.drawable.flag_spanish),
            LanguageItem("French", R.drawable.flag_french),
            LanguageItem("Hindi", R.drawable.flag_hindi),
            LanguageItem("Portugeese", R.drawable.flag_portugeese),
        )
        val rv = findViewById<RecyclerView>(R.id.rvLanguages)
        var selectedLanguage: String? = null
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = LanguageAdapter(languageList) { selected ->
            selectedLanguage = selected.name
        }

        findViewById<ImageButton>(R.id.btnDone).setOnClickListener {
            getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
                .edit().putBoolean("lang_done", true).apply()
            startActivity(Intent(this, TutorialActivity::class.java))
            finish()
        }

        rv.addItemDecoration(SpaceItemDecoration(this, 16))

    }
    class SpaceItemDecoration(private val context: Context, private val dp: Int) : RecyclerView.ItemDecoration() {
        private val space = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics
        ).toInt()

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.bottom = space
        }
    }
    override fun onResume() {
        super.onResume()
        showSystemUI(white = false)
    }
    fun Activity.showSystemUI(white: Boolean = false) {
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        if (white) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }
}