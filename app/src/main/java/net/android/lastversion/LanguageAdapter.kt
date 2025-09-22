package net.android.lastversion


import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.android.lastversion.data.LanguageItem

class LanguageAdapter(
    private val items: MutableList<LanguageItem>,
    private val onItemSelected: (LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            items.forEachIndexed { index, lang -> lang.isSelected = index == position }
            notifyDataSetChanged()
            onItemSelected(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLanguage: TextView = itemView.findViewById(R.id.tvLanguage)
        private val ivFlag: ImageView = itemView.findViewById(R.id.ivLanguage)
        private val radio: ImageButton = itemView.findViewById(R.id.radio)

        fun bind(item: LanguageItem) {
            tvLanguage.text = item.name
            ivFlag.setImageResource(item.flagResId)
            val icon = if (item.isSelected) R.drawable.ic_select_checked else R.drawable.ic_select_unchecked
            radio.setImageResource(icon)
        }
    }
}
