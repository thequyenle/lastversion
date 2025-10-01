package net.android.lastversion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.android.lastversion.utils.Theme
import net.android.lastversion.utils.ThemeManager
import net.android.lastversion.utils.ThemeType
import net.android.lastversion.utils.showSystemUI
import java.io.File

class ThemeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var themeAdapter: ThemeAdapter
    private lateinit var themeManager: ThemeManager
    private lateinit var btnSave: TextView
    private val themes = mutableListOf<Theme>()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val themeId = themeManager.addCustomTheme(it)
            // Tự động chọn theme mới thêm
            themeManager.saveSelectedTheme(themeId, ThemeType.CUSTOM)
            loadThemes()
            Toast.makeText(this, "Theme added!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme)

        showSystemUI(white = false)

        themeManager = ThemeManager(this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnSave = findViewById(R.id.btnSave)
        btnSave.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        loadThemes()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewThemes)
        themeAdapter = ThemeAdapter(
            onThemeClick = { theme ->
                themeManager.saveSelectedTheme(theme.id, theme.type)
                themeAdapter.notifyDataSetChanged()
            },
            getCurrentThemeId = { themeManager.getCurrentTheme()?.id }
        )

        recyclerView.apply {
            adapter = themeAdapter
            layoutManager = GridLayoutManager(this@ThemeActivity, 3)
        }
    }

    private fun loadThemes() {
        themes.clear()

        // Thêm empty slot cho nút Add
        themes.add(Theme("add_new", 0, ThemeType.ADD_NEW))

        // Thêm ảnh có sẵn
        themes.addAll(ThemeManager.PRESET_THEMES)

        // Thêm ảnh custom
        themes.addAll(themeManager.getCustomThemes())

        themeAdapter.submitList(themes.toList())
    }

    inner class ThemeAdapter(
        private val onThemeClick: (Theme) -> Unit,
        private val getCurrentThemeId: () -> String?
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var themes = listOf<Theme>()

        companion object {
            const val VIEW_TYPE_ADD = 0
            const val VIEW_TYPE_THEME = 1
        }

        fun submitList(newThemes: List<Theme>) {
            themes = newThemes
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return if (themes[position].type == ThemeType.ADD_NEW) {
                VIEW_TYPE_ADD
            } else {
                VIEW_TYPE_THEME
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_ADD) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_theme_add, parent, false)
                AddThemeViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_theme, parent, false)
                ThemeViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is AddThemeViewHolder -> holder.bind()
                is ThemeViewHolder -> holder.bind(themes[position])
            }
        }

        override fun getItemCount() = themes.size

        inner class AddThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
                itemView.setOnClickListener {
                    pickImageLauncher.launch("image/*")
                }
            }
        }

        inner class ThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView.findViewById(R.id.imgTheme)
            private val selectedIndicator: ImageView = itemView.findViewById(R.id.selectedIndicator)

            fun bind(theme: Theme) {
                // Hiển thị selected indicator
                selectedIndicator.visibility = if (theme.id == getCurrentThemeId()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                when (theme.type) {
                    ThemeType.PRESET -> {
                        Glide.with(itemView.context)
                            .load(theme.drawableRes)
                            .centerCrop()
                            .into(imageView)
                    }
                    ThemeType.CUSTOM -> {
                        val file = File(
                            itemView.context.filesDir,
                            "custom_themes/${theme.id}.jpg"
                        )
                        Glide.with(itemView.context)
                            .load(file)
                            .centerCrop()
                            .into(imageView)
                    }
                    else -> {}
                }

                itemView.setOnClickListener {
                    onThemeClick(theme)
                }
            }
        }
    }
}