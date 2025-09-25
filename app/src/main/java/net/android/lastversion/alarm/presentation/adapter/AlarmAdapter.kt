package net.android.lastversion.alarm.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import net.android.lastversion.R
import net.android.lastversion.alarm.domain.model.Alarm

class AlarmAdapter(
    private val onItemClick: (Alarm) -> Unit,
    private val onSwitchToggle: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
        val tvActiveDays: TextView = itemView.findViewById(R.id.tvActiveDays)
        val switchAlarm: Switch = itemView.findViewById(R.id.switchAlarm)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = getItem(position)

        holder.tvTime.text = alarm.getTimeString()
        holder.tvLabel.text = alarm.label
        holder.tvActiveDays.text = alarm.activeDaysText

        // Tạm thời tắt listener để tránh trigger khi set checked
        holder.switchAlarm.setOnCheckedChangeListener(null)
        holder.switchAlarm.isChecked = alarm.isEnabled

        // Set listener lại sau khi đã set trạng thái
        holder.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            onSwitchToggle(alarm)
        }
    }

    // Method để lấy item tại vị trí cụ thể (cho swipe to delete)
    fun getAlarmAt(position: Int): Alarm {
        return getItem(position)
    }
}

class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
    override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
        return oldItem == newItem
    }
}