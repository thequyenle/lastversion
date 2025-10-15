package net.android.lastversion.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import net.android.lastversion.R
import java.io.File
import java.io.FileOutputStream

class ThemeManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "alarm_theme_prefs"
        private const val KEY_SELECTED_THEME_ID = "selected_theme_id"
        private const val KEY_SELECTED_THEME_TYPE = "selected_theme_type"
        private const val KEY_CUSTOM_THEME_IDS = "custom_theme_ids"
        const val CUSTOM_THEMES_DIR = "custom_themes"

        // Danh sách ảnh có sẵn
        val PRESET_THEMES = listOf(
            Theme("preset_1", R.drawable.theme_1, ThemeType.PRESET),
            Theme("preset_2", R.drawable.theme_2, ThemeType.PRESET),
            Theme("preset_3", R.drawable.theme_3, ThemeType.PRESET),
            Theme("preset_4", R.drawable.theme_4, ThemeType.PRESET),
            Theme("preset_5", R.drawable.theme_5, ThemeType.PRESET),
            Theme("preset_6", R.drawable.theme_6, ThemeType.PRESET),
            Theme("preset_7", R.drawable.theme_7, ThemeType.PRESET),
            Theme("preset_8", R.drawable.theme_8, ThemeType.PRESET),
            Theme("preset_9", R.drawable.theme_9, ThemeType.PRESET),
        )
    }

    // Lấy theme hiện tại
    fun getCurrentTheme(): Theme? {
        val themeId = prefs.getString(KEY_SELECTED_THEME_ID, null) ?: return PRESET_THEMES[0]
        val typeStr = prefs.getString(KEY_SELECTED_THEME_TYPE, ThemeType.PRESET.name)
        val type = ThemeType.valueOf(typeStr!!)

        return when (type) {
            ThemeType.PRESET -> PRESET_THEMES.find { it.id == themeId }
            ThemeType.CUSTOM -> Theme(themeId, 0, ThemeType.CUSTOM)
            ThemeType.ADD_NEW -> null // ✅ ADD_NEW không bao giờ được lưu làm current theme
        }
    }

    // Lưu theme được chọn
    fun saveSelectedTheme(themeId: String, type: ThemeType) {
        // ✅ Không cho phép lưu ADD_NEW
        if (type == ThemeType.ADD_NEW) return

        prefs.edit().apply {
            putString(KEY_SELECTED_THEME_ID, themeId)
            putString(KEY_SELECTED_THEME_TYPE, type.name)
            apply()
        }
    }

    // Lấy drawable resource của theme hiện tại
    fun getCurrentThemeDrawable(): Int? {
        val theme = getCurrentTheme() ?: return null
        return when (theme.type) {
            ThemeType.PRESET -> theme.drawableRes
            ThemeType.CUSTOM -> null // Sẽ load từ file
            ThemeType.ADD_NEW -> null // ✅ ADD_NEW không có drawable
        }
    }

    // Lấy file path của theme custom
    fun getCurrentThemeFile(): File? {
        val theme = getCurrentTheme() ?: return null
        if (theme.type != ThemeType.CUSTOM) return null

        val customDir = File(context.filesDir, CUSTOM_THEMES_DIR)
        val file = File(customDir, "${theme.id}.jpg")
        return if (file.exists()) file else null
    }

    // Thêm ảnh custom
    fun addCustomTheme(uri: Uri): String {
        val timestamp = System.currentTimeMillis()
        val sequence = getNextSequence()
        val themeId = "custom_${timestamp}_${sequence}"
        val customDir = File(context.filesDir, CUSTOM_THEMES_DIR)
        if (!customDir.exists()) customDir.mkdirs()

        val imageFile = File(customDir, "$themeId.jpg")

        context.contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            val resized = resizeBitmap(bitmap, 1080, 1920)

            FileOutputStream(imageFile).use { output ->
                resized.compress(Bitmap.CompressFormat.JPEG, 85, output)
            }

            bitmap.recycle()
            resized.recycle()
        }

        // Lưu ID với atomic operation
        val customIds = prefs.getStringSet(KEY_CUSTOM_THEME_IDS, setOf())?.toMutableSet() ?: mutableSetOf()
        customIds.add(themeId)
        prefs.edit().putStringSet(KEY_CUSTOM_THEME_IDS, customIds).apply()

        return themeId
    }

    private fun getNextSequence(): Int {
        return prefs.getInt("custom_theme_sequence", 0) + 1.also {
            prefs.edit().putInt("custom_theme_sequence", it).apply()
        }
    }

    // Lấy tất cả custom themes
    fun getCustomThemes(): List<Theme> {
        val customIds = prefs.getStringSet(KEY_CUSTOM_THEME_IDS, setOf()) ?: setOf()
        return customIds
            .map { Theme(it, 0, ThemeType.CUSTOM) }
            .sortedWith(compareByDescending<Theme> { theme ->
                // Extract timestamp from theme ID (format: "custom_timestamp_sequence")
                try {
                    val afterCustom = theme.id.substringAfter("custom_")
                    val timestampPart = afterCustom.substringBefore("_")
                    timestampPart.toLong()
                } catch (e: Exception) {
                    0L // Fallback for invalid format
                }
            }.thenByDescending { theme ->
                // Extract sequence from theme ID for tie-breaking
                try {
                    val afterCustom = theme.id.substringAfter("custom_")
                    val sequencePart = afterCustom.substringAfter("_")
                    sequencePart.toInt()
                } catch (e: Exception) {
                    0 // Fallback for invalid format
                }
            })
    }

    // Xóa custom theme
    fun deleteCustomTheme(themeId: String) {
        val customDir = File(context.filesDir, CUSTOM_THEMES_DIR)
        val imageFile = File(customDir, "$themeId.jpg")
        imageFile.delete()

        val customIds = prefs.getStringSet(KEY_CUSTOM_THEME_IDS, setOf())?.toMutableSet()
        customIds?.remove(themeId)
        prefs.edit().putStringSet(KEY_CUSTOM_THEME_IDS, customIds).apply()
    }

    // Load ảnh custom
    fun loadCustomThemeBitmap(themeId: String): Bitmap? {
        val customDir = File(context.filesDir, CUSTOM_THEMES_DIR)
        val imageFile = File(customDir, "$themeId.jpg")
        return if (imageFile.exists()) {
            BitmapFactory.decodeFile(imageFile.absolutePath)
        } else null
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = Math.min(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )
        val width = (ratio * bitmap.width).toInt()
        val height = (ratio * bitmap.height).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}

data class Theme(
    val id: String,
    val drawableRes: Int,
    val type: ThemeType
)

enum class ThemeType {
    PRESET,
    CUSTOM,
    ADD_NEW
}