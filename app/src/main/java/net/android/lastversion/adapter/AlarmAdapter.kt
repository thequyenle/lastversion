// AlarmAdapter.kt - RecyclerView Adapter
package net.android.lastversion.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.android.lastversion.R
import net.android.lastversion.model.Alarm

class AlarmAdapter(
    private val alarmList: MutableList<Alarm>,
    private val onItemClick: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
        val tvActiveDays: TextView = itemView.findViewById(R.id.tvActiveDays)
        val switchAlarm: Switch = itemView.findViewById(R.id.switchAlarm)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(alarmList[position])
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
        val alarm = alarmList[position]

        holder.tvTime.text = alarm.getTimeString()
        holder.tvLabel.text = alarm.label
        holder.tvActiveDays.text = alarm.activeDaysText
        holder.switchAlarm.isChecked = alarm.isEnabled

        // Handle switch toggle
        holder.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            // Here you would update the alarm's enabled state in database
            // For now, just update the local list
            alarmList[position] = alarm.copy(isEnabled = isChecked)
        }
    }

    override fun getItemCount(): Int = alarmList.size
}