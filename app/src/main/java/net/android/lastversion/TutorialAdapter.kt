package net.android.lastversion

import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.android.lastversion.data.IntroPage


class TutorialAdapter(private val pages: List<IntroPage>) : RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder>() {

    inner class TutorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIntro: ImageView = itemView.findViewById(R.id.imgIntro)
        val tvWelcome: TextView = itemView.findViewById(R.id.tvWelcome)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tutorial, parent, false)
        return TutorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        val page = pages[position]
        holder.imgIntro.setImageResource(page.imageResId)
        holder.tvTitle.text = page.title
        holder.tvDescription.text = page.description

        // Show "Welcome to" only on first page (position 0)
        if (position == 0) {
            holder.tvWelcome.visibility = View.VISIBLE
        } else {
            holder.tvWelcome.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = pages.size
}