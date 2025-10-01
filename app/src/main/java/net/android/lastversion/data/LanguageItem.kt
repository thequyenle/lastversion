package net.android.lastversion.data

data class LanguageItem(
    val name: String,
    val flagResId: Int,
    var isSelected: Boolean = false,
    val code: String  // Mã ngôn ngữ ISO (en, es, fr, hi, pt)
)
