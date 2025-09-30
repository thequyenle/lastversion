package net.android.lastversion.alarm.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.android.lastversion.R
import net.android.lastversion.alarm.domain.model.Alarm

class AlarmAdapter(
    private val onItemClick: (Alarm) -> Unit,
    private val onSwitchToggle: (Alarm) -> Unit,
    private val onMenuClick: (Alarm, View) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
        private val tvActiveDays: TextView = itemView.findViewById(R.id.tvActiveDays)
        private val switchAlarm: ImageView = itemView.findViewById(R.id.switchAlarm)
        private val btnMenu: ImageView? = itemView.findViewById(R.id.btnMenu)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            switchAlarm.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSwitchToggle(getItem(position))
                }
            }

            btnMenu?.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMenuClick(getItem(position), it)
                }
            }
        }

        fun bind(alarm: Alarm) {
            tvTime.text = alarm.getTimeString()
            tvLabel.text = alarm.note
            tvActiveDays.text = alarm.getActiveDaysText()

            updateAlarmState(alarm.isEnabled)
        }

        private fun updateAlarmState(isEnabled: Boolean) {
            if (isEnabled) {
                // Alarm BẬT - màu xanh #84DCC6
                (itemView as LinearLayout).setBackgroundResource(R.drawable.bg_rounded_green_alarm_item)
                switchAlarm.setImageResource(R.drawable.ic_switch_on)

                tvLabel.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                tvTime.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                tvActiveDays.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))

            } else {
                // Alarm TẮT - màu xám #F7F7F7
                (itemView as LinearLayout).setBackgroundResource(R.drawable.bg_rounded_gray_alarm_item)
                switchAlarm.setImageResource(R.drawable.ic_switch_off)

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getAlarmAt(position: Int): Alarm = getItem(position)
}

private class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
    override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
        return oldItem == newItem
    }
}