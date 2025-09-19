package net.android.lastversion


import android.view.*
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LanguageAdapter(private val items: List<String>,
                      private val onItemSelected: (String) -> Unit
) :
    RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        //  Dùng position ngay lập tức (ổn)
        holder.bind(items[position], position == selectedPosition)

        //  Dùng adapterPosition trong onClick để tránh sai lệch
        holder.itemView.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener

            val previousPosition = selectedPosition
            selectedPosition = currentPos

            notifyItemChanged(previousPosition)
            notifyItemChanged(currentPos)

            onItemSelected(items[currentPos])
        }
    }

    override fun getItemCount(): Int = items.size

    class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLang = itemView.findViewById<TextView>(R.id.tvLang)
        private val radio: RadioButton = itemView.findViewById(R.id.radio)
        fun bind(language: String, isSelected: Boolean) {
            tvLang.text = language
            radio.isChecked = isSelected
        }
    }
}