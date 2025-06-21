package app.platecarbon.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.platecarbon.databinding.ItemVehicleLogBinding
import app.platecarbon.model.VehicleLog
import java.text.SimpleDateFormat
import java.util.Locale

class VehicleLogAdapter(private var logs: List<VehicleLog>) :
    RecyclerView.Adapter<VehicleLogAdapter.VehicleLogViewHolder>() {

    private val dbFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    inner class VehicleLogViewHolder(private val binding: ItemVehicleLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: VehicleLog) {
            binding.tvPlateNumber.text = log.plate
            binding.tvEntryTime.text = "Giriş: ${formatDate(log.entryTime)}"
            binding.tvExitTime.text = if(log.exitTime != null) "Çıkış: ${formatDate(log.exitTime)}" else "Çıkış yapılmadı"
            binding.tvTotalTime.text = "${log.totalTimeSeconds ?: 0}s\nToplam"
            binding.tvParkedTime.text = "${log.totalParkedSeconds ?: 0}s\nPark"
            binding.tvMovingTime.text = "${log.actualMovingSeconds ?: 0}s\nHareket"

            if (log.carbonEmission != null) {
                binding.tvCarbonEmission.text = "%.2f g/km".format(log.carbonEmission)
                binding.tvCarbonEmission.visibility = View.VISIBLE
            } else {
                binding.tvCarbonEmission.visibility = View.GONE
            }
        }

        private fun formatDate(dateString: String?): String {
            if (dateString.isNullOrEmpty()) return "Bilinmiyor"
            return try {
                val date = dbFormat.parse(dateString)
                if (date != null) displayFormat.format(date) else "Hatalı Tarih"
            } catch (e: Exception) {
                dateString
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleLogViewHolder {
        val binding = ItemVehicleLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehicleLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleLogViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size

    fun updateLogs(newLogs: List<VehicleLog>) {
        this.logs = newLogs
        notifyDataSetChanged()
    }
}