package net.android.lastversion

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class TutorialAdapter(private val items: List<String>) :
    RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tutorial, parent, false)
        return TutorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class TutorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent = itemView.findViewById<TextView>(R.id.tvContent)
        fun bind(text: String) {
            tvContent.text = text
        }
    }
}