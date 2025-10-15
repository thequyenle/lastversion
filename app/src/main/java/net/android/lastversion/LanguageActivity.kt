package net.android.lastversion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.android.lastversion.data.LanguageItem
import net.android.lastversion.utils.LocaleHelper
import net.android.lastversion.utils.SystemUtils

class LanguageActivity : BaseActivity() {

    private lateinit var languageList: MutableList<LanguageItem>
    private var selectedLanguageCode: String = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        // Lấy ngôn ngữ hiện tại
        val currentLang = LocaleHelper.getLanguage(this)
        selectedLanguageCode = currentLang

        // Danh sách ngôn ngữ với mã ngôn ngữ chuẩn ISO
        languageList = mutableListOf(
            LanguageItem("English", R.drawable.flag_english, false, "en"),
            LanguageItem("Spanish", R.drawable.flag_spanish, false, "es"),
            LanguageItem("French", R.drawable.flag_french, false, "fr"),
            LanguageItem("Hindi", R.drawable.flag_hindi, false, "hi"),
            LanguageItem("Portuguese", R.drawable.flag_portugeese, false, "pt")
        )

        // Đánh dấu ngôn ngữ hiện tại
        languageList.find { it.code == currentLang }?.isSelected = true

        val rv = findViewById<RecyclerView>(R.id.rvLanguages)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = LanguageAdapter(languageList) { selected ->
            selectedLanguageCode = selected.code
        }

        findViewById<ImageButton>(R.id.btnDone).setOnClickListener {
            // Lưu ngôn ngữ đã chọn (không recreate ngay)
            LocaleHelper.setLanguage(this, selectedLanguageCode)

            // Kiểm tra xem có phải mở từ Settings không
            val fromSettings = intent.getBooleanExtra("from_settings", false)

            if (fromSettings) {
                // Nếu từ Settings, chỉ cần finish và quay lại
                setResult(RESULT_OK)
                finish()
            } else {
                // Nếu từ onboarding, tiếp tục flow bình thường
                getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
                    .edit().putBoolean("lang_done", true).apply()

                startActivity(Intent(this, TutorialActivity::class.java))
                finish()
            }
        }

        rv.addItemDecoration(SpaceItemDecoration(this, 16))
    }

    class SpaceItemDecoration(private val context: Context, private val dp: Int) :
        RecyclerView.ItemDecoration() {

        private val space = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.bottom = space
        }
    }

    override fun onResume() {
        super.onResume()
        showSystemUI(white = false)
    }

    private fun Activity.showSystemUI(white: Boolean = false) {
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        if (white) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }
}