package net.android.lastversion.alarm.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.android.lastversion.R
import net.android.lastversion.alarm.domain.model.Alarm

class AlarmAdapter(
    private val onItemClick: (Alarm) -> Unit,
    private val onSwitchToggle: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
        private val tvActiveDays: TextView = itemView.findViewById(R.id.tvActiveDays)
        private val switchAlarm: Switch = itemView.findViewById(R.id.switchAlarm)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(alarm: Alarm) {
            tvTime.text = alarm.getTimeString()
            tvLabel.text = alarm.label
            tvActiveDays.text = alarm.getActiveDaysText()

            // Temporarily remove listener to prevent unwanted triggers
            switchAlarm.setOnCheckedChangeListener(null)
            switchAlarm.isChecked = alarm.isEnabled

            // Re-add listener
            switchAlarm.setOnCheckedChangeListener { _, _ ->
                onSwitchToggle(alarm)
            }

            // Visual state for enabled/disabled alarms
            val alpha = if (alarm.isEnabled) 1.0f else 0.5f
            tvTime.alpha = alpha
            tvLabel.alpha = alpha
            tvActiveDays.alpha = alpha
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